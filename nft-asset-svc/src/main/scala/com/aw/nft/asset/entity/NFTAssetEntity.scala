package com.aw.nft.asset.entity

import com.aw.nft.asset.model.{ActiveStatus, DeletedStatus, NFTAsset}
import com.aw.nft.asset.serialization.CborSerializable
import com.aw.nft.asset.utils.PersistenceUtils
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.typed.*
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import org.slf4j.Logger

import scala.concurrent.duration.*

object NFTAssetEntity extends PersistenceUtils:

  type CommandReply = ReplyEffect[AssetEvent, AssetState]

  // Commands
  sealed trait AssetCommand(val id: String) extends CborSerializable

  final case class CreateAsset(asset: NFTAsset, replyTo: ActorRef[StatusReply[Done]])  extends AssetCommand(asset.id)
  final case class GetAsset(assetId: String, replyTo: ActorRef[StatusReply[NFTAsset]]) extends AssetCommand(assetId)
  final case class AddFileIdToAsset(assetId: String, fileId: String, replyTo: ActorRef[StatusReply[NFTAsset]])
      extends AssetCommand(assetId)
  final case class RenameAsset(assetId: String, newName: String, replyTo: ActorRef[StatusReply[NFTAsset]])
      extends AssetCommand(assetId)
  final case class RemoveAsset(assetId: String, replyTo: ActorRef[StatusReply[Done]])  extends AssetCommand(assetId)

  // Events
  sealed trait AssetEvent(val id: String) extends CborSerializable

  final case class AssetCreated(asset: NFTAsset)     extends AssetEvent(asset.id)
  final case class AssetFileIdAdded(asset: NFTAsset) extends AssetEvent(asset.id)
  final case class AssetRenamed(asset: NFTAsset)     extends AssetEvent(asset.id)
  final case class AssetRemoved(asset: NFTAsset)     extends AssetEvent(asset.id)

  // State
  sealed trait AssetState extends CborSerializable:
    def applyCommand(assetId: String, ctx: ActorContext[AssetCommand], cmd: AssetCommand): CommandReply
    def applyEvent(event: AssetEvent): AssetState

  case object DoesNotExistState extends AssetState:
    override def applyCommand(assetId: String, ctx: ActorContext[AssetCommand], cmd: AssetCommand): CommandReply =
      cmd match
        case CreateAsset(asset, replyTo)                =>
          createAsset(asset, replyTo, ctx)
        case GetAsset(assetId, replyTo)                 =>
          invalidReply(s"The entity ${assetId} is not created yet.", replyTo, ctx)
        case AddFileIdToAsset(assetId, fileId, replyTo) =>
          invalidReply(s"The entity ${assetId} is not created yet.", replyTo, ctx)
        case RenameAsset(assetId, newName, replyTo)     =>
          invalidReply(s"The entity ${assetId} is not created yet.", replyTo, ctx)
        case RemoveAsset(assetId, replyTo)              =>
          invalidReply(s"The entity ${assetId} is not created yet.", replyTo, ctx)

    override def applyEvent(event: AssetEvent): AssetState = event match
      case AssetCreated(asset) => ActiveState(asset)
      case _                   => this

  case class ActiveState(asset: NFTAsset) extends AssetState:
    override def applyCommand(assetId: String, ctx: ActorContext[AssetCommand], cmd: AssetCommand): CommandReply =
      cmd match
        case CreateAsset(asset, replyTo)                =>
          invalidReply(s"The entity ${asset.id} is in Active state.", replyTo, ctx)
        case GetAsset(assetId, replyTo)                 =>
          getAsset(asset, replyTo, ctx)
        case AddFileIdToAsset(assetId, fileId, replyTo) =>
          addFileId(asset, fileId, replyTo, ctx)
        case RenameAsset(assetId, newName, replyTo)     =>
          rename(asset, newName, replyTo, ctx)
        case RemoveAsset(assetId, replyTo)              =>
          remove(asset, replyTo, ctx)

    override def applyEvent(event: AssetEvent): AssetState = event match
      case AssetFileIdAdded(asset) => this.copy(asset = asset)
      case AssetRenamed(asset)     => this.copy(asset = asset)
      case AssetRemoved(asset)     => DeletedState(asset)
      case _                       => this

  case class DeletedState(asset: NFTAsset) extends AssetState:
    override def applyCommand(assetId: String, ctx: ActorContext[AssetCommand], cmd: AssetCommand): CommandReply =
      cmd match
        case CreateAsset(asset, replyTo)                =>
          invalidReply(s"The entity ${asset.id} is in Deleted state.", replyTo, ctx)
        case GetAsset(assetId, replyTo)                 => getAsset(asset, replyTo, ctx)
        case AddFileIdToAsset(assetId, fileId, replyTo) =>
          invalidReply(s"The entity ${asset.id} is in Deleted state.", replyTo, ctx)
        case RenameAsset(assetId, newName, replyTo)     =>
          invalidReply(s"The entity ${asset.id} is in Deleted state.", replyTo, ctx)
        case RemoveAsset(assetId, replyTo)              =>
          invalidReply(s"The entity ${asset.id} is in Deleted state.", replyTo, ctx)

    override def applyEvent(event: AssetEvent): AssetState = event match
      case _ => this

  // Cluster Sharding
  val EntityKey: EntityTypeKey[AssetCommand] =
    EntityTypeKey[AssetCommand](assetEntityTypeKeyName)

  val tags: Seq[String] = Vector.tabulate(10)(i => s"nft-asset-FSM-$i")

  def init(system: ActorSystem[_]): Unit =
    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      val i                  = math.abs(entityContext.entityId.hashCode % tags.size)
      val selectedTag        = tags(i)
      val supervisorStrategy = SupervisorStrategy.restartWithBackoff(500.milli, 10.seconds, 0.1)
      Behaviors
        .supervise(NFTAssetEntity(entityContext.entityId, selectedTag))
        .onFailure[Throwable](supervisorStrategy)
    })

  def apply(id: String, projectionTag: String): Behavior[AssetCommand] =
    Behaviors.setup { ctx =>
      val log: Logger = ctx.log
      log.info("Starting AssetEntity {}", id)
      EventSourcedBehavior
        .withEnforcedReplies[AssetCommand, AssetEvent, AssetState](
          persistenceId = PersistenceId.ofUniqueId(s"${id}"),
          emptyState = DoesNotExistState,
          (state, command) => state.applyCommand(id, ctx, command),
          (state, event) => state.applyEvent(event)
        )
        .withTagger(_ => Set(projectionTag))
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
    }

  private def invalidReply[Message](
      message: String,
      replyTo: ActorRef[StatusReply[Message]],
      ctx: ActorContext[AssetCommand]
  ): CommandReply =
    ctx.log.error(message)
    Effect.unhandled.thenReply(replyTo)(_ => StatusReply.Error[Message](message))

  private def createAsset(
      asset: NFTAsset,
      replyTo: ActorRef[StatusReply[Done]],
      ctx: ActorContext[AssetCommand]
  ): CommandReply =
    val evt = AssetCreated(asset.copy(assetStatus = ActiveStatus()))
    ctx.log.debug("Persisting Event: {} to object {}", evt, asset.id)
    persistEventAndAck(evt, replyTo)

  private def addFileId(
      asset: NFTAsset,
      fileId: String,
      replyTo: ActorRef[StatusReply[NFTAsset]],
      ctx: ActorContext[AssetCommand]
  ): CommandReply =
    val evt = AssetFileIdAdded(
      asset.copy(fileId = Some(fileId))
    )
    ctx.log.debug("Persisting Event: {} to object {}", evt, asset.id)
    persistAndReply(evt, replyTo, evt.asset)

  private def rename(
      asset: NFTAsset,
      newName: String,
      replyTo: ActorRef[StatusReply[NFTAsset]],
      ctx: ActorContext[AssetCommand]
  ): CommandReply =
    val evt = AssetRenamed(
      asset.copy(name = newName)
    )
    ctx.log.debug("Persisting Event: {} to object {}", evt, asset.id)
    persistAndReply(evt, replyTo, evt.asset)

  private def remove(
      asset: NFTAsset,
      replyTo: ActorRef[StatusReply[Done]],
      ctx: ActorContext[AssetCommand]
  ): CommandReply =
    val evt = AssetRemoved(
      asset.copy(assetStatus = DeletedStatus())
    )
    ctx.log.debug("Persisting Event: {} to object {}", evt, asset.id)
    persistEventAndAck(evt, replyTo)

  private def getAsset(
      asset: NFTAsset,
      replyTo: ActorRef[StatusReply[NFTAsset]],
      ctx: ActorContext[AssetCommand]
  ): CommandReply =
    Effect.reply(replyTo)(StatusReply.success(asset))
