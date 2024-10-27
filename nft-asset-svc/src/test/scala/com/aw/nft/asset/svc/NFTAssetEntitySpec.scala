package com.aw.nft.asset.svc

import com.aw.nft.asset.entity.NFTAssetEntity
import com.aw.nft.asset.entity.NFTAssetEntity.*
import com.aw.nft.asset.model.NFTAsset
import com.typesafe.config.ConfigFactory
import org.apache.pekko.Done
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*

class NFTAssetEntitySpec
    extends AnyWordSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with Eventually:

  private val testKit            = ActorTestKit(
    EventSourcedBehaviorTestKit.config.withFallback(
      ConfigFactory.parseString("""
                                  |pekko {
                                  |    actor {
                                  |        serialization-bindings {
                                  |            "com.aw.nft.asset.serialization.CborSerializable" = jackson-cbor
                                  |        }
                                  |    }
                                  |}""".stripMargin)
    )
  )
  given system: ActorSystem[?]   = testKit.system
  given patience: PatienceConfig = PatienceConfig(testKit.testKitSettings.DefaultTimeout.duration, scaled(100.millis))

  private val projectionTag = "1"
  private val timestamp     = Instant.now

  private val assetId     = UUID.randomUUID.toString
  private val name        = "test asset"
  private val description = "awesome asset"
  private val fileId      = UUID.randomUUID.toString

  private val testAsset = NFTAsset(
    id = assetId,
    name = name,
    description = description
  )

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[AssetCommand, AssetEvent, AssetState](
      system,
      NFTAssetEntity(assetId, projectionTag)
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
  }

  "Uninitialized nft asset" should {
    "respond to GetAsset command with an error" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[NFTAsset]](ref => GetAsset(assetId, ref))
      result.reply.isError shouldBe true
    }
    "respond to Create command by initializing a new asset" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](ref => CreateAsset(testAsset, ref))
      result.reply.isSuccess shouldBe true
      result.event shouldBe a[AssetCreated]
      result.event.id shouldBe testAsset.id
    }
    "respond to GetAsset command with the asset" in {
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => CreateAsset(testAsset, ref))
      val result = eventSourcedTestKit.runCommand[StatusReply[NFTAsset]](ref => GetAsset(assetId, ref))
      result.reply.isSuccess shouldBe true
      result.reply.getValue.id shouldBe testAsset.id
      result.reply.getValue.assetStatus.value shouldBe "active"
    }
  }
