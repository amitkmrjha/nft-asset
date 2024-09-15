package com.aw.nft.asset.svc

import com.aw.nft.asset.{NFTAssetServer, NFTAssetServiceImpl}
import com.aw.nft.grpc.{GetHealthResponse, NFTAssetServiceClient}
import com.google.protobuf.empty.Empty
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.grpc.GrpcClientSettings
import org.apache.pekko.http.scaladsl.Http
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AsyncWordSpec

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.*

class NFTAssetGrpcSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures:

  given patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  lazy val config =
    ConfigFactory
      .parseString("""
                       |pekko.http.server.preview.enable-http2 = on
                       |pekko.coordinated-shutdown.exit-jvm = off
          """.stripMargin)
      .withFallback(ConfigFactory.load("local1.conf"))
      .resolve()

  lazy val grpcInterface = config.getString("nft-asset-svc.grpc.interface")
  lazy val grpcPort      = config.getInt("nft-asset-svc.grpc.port")
  lazy val serviceImpl   = new NFTAssetServiceImpl()

  val testKit: ActorTestKit = ActorTestKit(config)

  given typedSystem: ActorSystem[?] = testKit.system

  def clientSettings: GrpcClientSettings = GrpcClientSettings
    .fromConfig(clientName = "nft-asset-grpc-test-client")(typedSystem)

  val assetGrpcClient: NFTAssetServiceClient =
    NFTAssetServiceClient.apply(clientSettings)

  override def beforeAll(): Unit =
    val service: Future[Http.ServerBinding] = NFTAssetServer.start(grpcInterface, grpcPort, serviceImpl)

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  "NFTAsset Grpc service" should {
    "check health of gRPC server" in {
      val request: Empty                      = Empty.defaultInstance
      val response: Future[GetHealthResponse] =
        assetGrpcClient
          .getHealth()
          .addHeader("x-correlation-id", s"${UUID.randomUUID().toString}")
          .invoke(request)
      response.futureValue.message shouldBe "NFTAsset gRPC is healthy!"
    }
  }
