package com.aw.nft.asset.projections

import com.aw.nft.asset.entity.NFTAssetEntity
import com.aw.nft.asset.entity.NFTAssetEntity.AssetEvent
import com.aw.nft.asset.utils.ScalikeJdbcSession
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.SendProducer
import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.jdbc.scaladsl.JdbcProjection
import org.apache.pekko.projection.scaladsl.{AtLeastOnceProjection, SourceProvider}
import org.apache.pekko.projection.{ProjectionBehavior, ProjectionId}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.pekko.actor.CoordinatedShutdown

object NFTAssetEventKafkaProjection:
  def init(actorSystem: ActorSystem[?]): Unit =
    given system: ActorSystem[?] = actorSystem
    val log                      = system.log
    log.info("Starting Projection for Asset Events to kafka")

    val sendProducer = createProducer(system)

    ShardedDaemonProcess(actorSystem).init(
      name = "AssetEventKafkaProjection",
      NFTAssetEntity.tags.size,
      index => ProjectionBehavior(createProjectionFor(sendProducer, index)),
      ShardedDaemonProcessSettings(actorSystem),
      Some(ProjectionBehavior.Stop)
    )

  private def createProducer(system: ActorSystem[?]): SendProducer[String, Array[Byte]] =
    val producerSettings: ProducerSettings[String, Array[Byte]] =
      ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    val sendProducer                                            = SendProducer(producerSettings)(system)
    CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "close-sendProducer") {
      () =>
        sendProducer.close()
    }
    sendProducer

  private def createProjectionFor(
      sendProducer: SendProducer[String, Array[Byte]],
      index: Int
  )(using system: ActorSystem[?]): AtLeastOnceProjection[Offset, EventEnvelope[AssetEvent]] =
    val tag = NFTAssetEntity.tags(index)

    val sourceProvider: SourceProvider[Offset, EventEnvelope[AssetEvent]] =
      EventSourcedProvider.eventsByTag[AssetEvent](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag
      )

    JdbcProjection.atLeastOnceAsync(
      projectionId = ProjectionId("AssetEventKafkaProjection", tag),
      sourceProvider,
      handler = () => new AssetEventKafkaProjectionHandler(system, sendProducer),
      sessionFactory = () => new ScalikeJdbcSession()
    )(system)
