package com.aw.nft.asset

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object NFTAssetMain:
  val logger = LoggerFactory.getLogger(NFTAssetMain.getClass)

  lazy val config =
    ConfigFactory
      .parseString("pekko.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.load())

  given system: ActorSystem[?] = ActorSystem(Behaviors.empty, "nft-asset-svc", config)
  given ec: ExecutionContext   = system.executionContext

  @main def main(): Unit =
    sys.addShutdownHook(system.terminate())

    try {
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization error", e)
        system.terminate()
    }

  def init(
      system: ActorSystem[?]
  ): Unit = {}
  startGrpc(system)

  private def startGrpc(
      system: ActorSystem[?]
  ): Future[Http.ServerBinding] =
    given ActorSystem[?]   = system
    given ExecutionContext = system.executionContext

    val grpcInterface                       = system.settings.config.getString("nft-asset-svc.grpc.interface")
    val grpcPort                            = system.settings.config.getInt("nft-asset-svc.grpc.port")
    val serviceImpl                         = new NFTAssetServiceImpl
    val binding: Future[Http.ServerBinding] = NFTAssetServer.start(grpcInterface, grpcPort, serviceImpl)
    system.log.info(s"NFTAsset gRPC server running at $grpcInterface:$grpcPort")
    binding
