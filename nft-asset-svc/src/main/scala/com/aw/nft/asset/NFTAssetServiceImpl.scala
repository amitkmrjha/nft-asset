package com.aw.nft.asset

import com.aw.nft.grpc.{GetHealthResponse, NFTAssetServicePowerApi}
import com.google.protobuf.empty.Empty
import com.google.protobuf.timestamp.Timestamp
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.grpc.scaladsl.Metadata

import java.net.InetAddress
import java.time.Instant
import scala.concurrent.Future

class NFTAssetServiceImpl[A: ActorSystem]() extends NFTAssetServicePowerApi:

  private val hostname = InetAddress.getLocalHost.getHostAddress

  override def getHealth(in: Empty, metadata: Metadata): Future[GetHealthResponse] =
    Future.successful(
      GetHealthResponse("NFTAsset gRPC is healthy!", hostname, Some(Timestamp(Instant.now())))
    )
