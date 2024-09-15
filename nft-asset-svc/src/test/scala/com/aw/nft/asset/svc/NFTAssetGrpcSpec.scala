package com.aw.nft.asset.svc

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AsyncWordSpec

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

  val testKit: ActorTestKit = ActorTestKit(config)

  given typedSystem: ActorSystem[?] = testKit.system

  override def beforeAll(): Unit =
    super.beforeAll()

  override def afterAll(): Unit =
    testKit.shutdownTestKit()

  "NFTAsset Grpc service" should {
    "return dummy success " in {
      true shouldBe true
    }
  }
