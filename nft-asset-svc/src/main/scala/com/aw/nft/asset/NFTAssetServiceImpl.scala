package com.aw.nft.asset

import com.aw.nft.asset.entity.NFTAssetEntity
import com.aw.nft.asset.entity.NFTAssetEntity.{AssetCommand, CreateAsset, GetAsset}
import com.aw.nft.asset.model.NFTAsset
import com.aw.nft.grpc.*
import com.google.protobuf.empty.Empty
import com.google.protobuf.timestamp.Timestamp
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.util.Timeout
import org.slf4j.LoggerFactory

import java.net.InetAddress
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class NFTAssetServiceImpl[A: ActorSystem]() extends NFTAssetServicePowerApi:

  val system   = summon[ActorSystem[?]]
  val log      = LoggerFactory.getLogger(getClass)
  val sharding = ClusterSharding(system)

  given ec: ExecutionContext = system.executionContext

  given timeout: Timeout = Timeout.create(system.settings.config.getDuration("nft-asset-svc.grpc.ask-timeout"))

  private val hostname = InetAddress.getLocalHost.getHostAddress

  override def getHealth(in: Empty, metadata: Metadata): Future[GetHealthResponse] =
    Future.successful(
      GetHealthResponse("NFTAsset gRPC is healthy!", hostname, Some(Timestamp(Instant.now())))
    )

  override def createNFTAsset(in: CreateNFTAssetRequest, metadata: Metadata): Future[CreateNFTAssetResponse] =
    val newAsset = NFTAsset(
      id = in.assetId,
      name = in.assetName,
      description = in.assetDescription
    )
    createNewAsset(newAsset)
      .recoverWith { case e =>
        log.error(s"Failed to create NFT Asset: ${e.getMessage}")
        Future.failed(e)
      }
      .map(_ => CreateNFTAssetResponse(in.assetId))

  override def getNFTAsset(in: GetNFTAssetRequest, metadata: Metadata): Future[GetNFTAssetResponse] =
    getAsset(in.assetId)
      .recoverWith { case e =>
        log.error(s"Failed to get NFT Asset: ${e.getMessage}")
        Future.failed(e)
      }
      .map(asset => GetNFTAssetResponse(asset.id, asset.name, asset.description))

  override def addNFTFileId(in: AddNFTFileIdRequest, metadata: Metadata): Future[AddNFTFileIdResponse] = ???

  override def renameNFTAsset(in: RenameNFTAssetRequest, metadata: Metadata): Future[RenameNFTAssetResponse] = ???

  override def removeNFTAsset(in: RemoveNFTAssetRequest, metadata: Metadata): Future[RemoveNFTAssetResponse] = ???

  private def entityRef(assetId: String): EntityRef[AssetCommand] =
    sharding.entityRefFor(NFTAssetEntity.EntityKey, assetId)

  protected def getAsset(assetId: String): Future[NFTAsset] =
    entityRef(assetId).askWithStatus[NFTAsset](ref => GetAsset(assetId, ref))

  protected def createNewAsset(asset: NFTAsset): Future[Done] =
    entityRef(asset.id).askWithStatus[Done](ref => CreateAsset(asset, ref))
