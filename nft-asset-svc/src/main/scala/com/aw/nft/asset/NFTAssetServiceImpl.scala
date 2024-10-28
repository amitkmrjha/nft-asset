package com.aw.nft.asset

import com.aw.nft.asset.entity.NFTAssetEntity
import com.aw.nft.asset.entity.NFTAssetEntity.*
import com.aw.nft.asset.model.NFTAsset
import com.aw.nft.asset.repository.NFTAssetRepository
import com.aw.nft.grpc.*
import com.google.protobuf.empty.Empty
import com.google.protobuf.timestamp.Timestamp
import io.grpc.Status
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import org.apache.pekko.grpc.GrpcServiceException
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.util.Timeout
import org.slf4j.LoggerFactory

import java.net.InetAddress
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class NFTAssetServiceImpl[A: ActorSystem](asserRepo: NFTAssetRepository) extends NFTAssetServicePowerApi:

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
      .map(_ => CreateNFTAssetResponse(in.assetId, "Done"))
      .recover { case e =>
        val msg = s"Failed to create NFT Asset: ${e.getMessage}"
        log.error(msg)
        CreateNFTAssetResponse(in.assetId, msg)
      }

  override def getNFTAsset(in: GetNFTAssetRequest, metadata: Metadata): Future[GetNFTAssetResponse] =
    getAsset(in.assetId)
      .map { asset =>
        GetNFTAssetResponse.defaultInstance
          .withAssetId(asset.id)
          .withAssetName(asset.name)
          .withAssetDescription(asset.description)
          .withStatus(asset.assetStatus.value)
          .withAssetFileId(asset.fileId.getOrElse(""))
      }
      .recoverWith { case e =>
        val msg = s"Failed to get NFT Asset: ${e.getMessage}"
        log.error(msg)
        Future.failed(
          new GrpcServiceException(Status.UNKNOWN.withDescription(msg))
        )
      }

  override def addNFTFileId(in: AddNFTFileIdRequest, metadata: Metadata): Future[AddNFTFileIdResponse] =
    addFileIdToAsset(in.assetId, in.assetFileId)
      .map(asset => AddNFTFileIdResponse(asset.id, "Done"))
      .recover { case e =>
        log.error(s"Failed to add file id to NFT Asset: ${e.getMessage}")
        AddNFTFileIdResponse(in.assetId, s"unable to add file id to NFT Asset: ${e.getMessage}")
      }

  override def renameNFTAsset(in: RenameNFTAssetRequest, metadata: Metadata): Future[RenameNFTAssetResponse] =
    renameAsset(in.assetId, in.assetName)
      .map(asset => RenameNFTAssetResponse(asset.id, "Done"))
      .recover { case e =>
        log.error(s"Failed to rename NFT Asset: ${e.getMessage}")
        RenameNFTAssetResponse(in.assetId, s"unable to rename NFT Asset: ${e.getMessage}")
      }

  override def removeNFTAsset(in: RemoveNFTAssetRequest, metadata: Metadata): Future[RemoveNFTAssetResponse] =
    removeAsset(in.assetId)
      .map(done => RemoveNFTAssetResponse(in.assetId, "Done"))
      .recover { case e =>
        log.error(s"Failed to remove NFT Asset: ${e.getMessage}")
        RemoveNFTAssetResponse(in.assetId, s"unable to remove NFT Asset: ${e.getMessage}")
      }

  private def entityRef(assetId: String): EntityRef[AssetCommand] =
    sharding.entityRefFor(NFTAssetEntity.EntityKey, assetId)

  protected def getAsset(assetId: String): Future[NFTAsset] =
    entityRef(assetId).askWithStatus[NFTAsset](ref => GetAsset(assetId, ref))

  protected def createNewAsset(asset: NFTAsset): Future[Done] =
    entityRef(asset.id).askWithStatus[Done](ref => CreateAsset(asset, ref))

  protected def addFileIdToAsset(assetId: String, fileId: String): Future[NFTAsset] =
    entityRef(assetId).askWithStatus[NFTAsset](ref => AddFileIdToAsset(assetId, fileId, ref))

  protected def renameAsset(assetId: String, newName: String): Future[NFTAsset] =
    entityRef(assetId).askWithStatus[NFTAsset](ref => RenameAsset(assetId, newName, ref))

  protected def removeAsset(assetId: String): Future[Done] =
    entityRef(assetId).askWithStatus[Done](ref => RemoveAsset(assetId, ref))

  override def getNFTAssetByFileId(in: GetNFTAssetByFileIdRequest, metadata: Metadata): Future[GetNFTAssetResponse] =
    Future.fromTry(
      Try(asserRepo.getByFileId(in.fileId)) match
        case Success(Some(asset)) =>
          Success(GetNFTAssetResponse(asset.id, asset.name, asset.description, asset.fileId, asset.assetStatus.value))
        case Success(None)        =>
          val msg = s"Asset with file id ${in.fileId} not found or deleted"
          log.info(msg)
          throw new GrpcServiceException(Status.UNKNOWN.withDescription(msg))
        case Failure(exception)   =>
          val msg = s"unable to get asset by file id: ${exception.getMessage}"
          log.info(msg)
          throw new GrpcServiceException(Status.UNKNOWN.withDescription(msg))
    )
