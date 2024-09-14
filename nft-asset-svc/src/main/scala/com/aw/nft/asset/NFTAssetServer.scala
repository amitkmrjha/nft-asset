package com.aw.nft.asset

import com.aw.nft.grpc.{NFTAssetService, NFTAssetServicePowerApi, NFTAssetServicePowerApiHandler}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.grpc.scaladsl.{ServerReflection, ServiceHandler}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.server.Directives.handle
import org.apache.pekko.http.scaladsl.server.Route

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object NFTAssetServer:

  def start(interface: String, port: Int, grpcService: NFTAssetServicePowerApi)(using
      system: ActorSystem[?]
  ): Future[Http.ServerBinding] =
    given ec: ExecutionContext = system.executionContext

    val service: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(
        NFTAssetServicePowerApiHandler.partial(grpcService),
        // ServerReflection enabled to support grpcurl directly
        ServerReflection.partial(List(NFTAssetService))
      )
    val grpcRoute: Route                             = handle(service)

    val bound =
      Http()
        .newServerAt(interface, port)
        .bind(grpcRoute)
        .map(_.addToCoordinatedShutdown(3.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("NFTAsset online at gRPC server {}:{}", address.getHostName, address.getPort)
      case Failure(error)   =>
        system.log.error("Failed to bind gRPC endpoint, terminating system")
        system.terminate()
    }
    bound
