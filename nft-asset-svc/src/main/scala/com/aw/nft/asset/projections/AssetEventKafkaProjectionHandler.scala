package com.aw.nft.asset.projections

import com.aw.nft.asset.entity.NFTAssetEntity.{AssetCreated, AssetEvent}
import com.aw.nft.asset.model.NFTAsset
import com.aw.nft.kafka.publish.event.NFTAssetCreatedMessage
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.kafka.scaladsl.SendProducer
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.scaladsl.Handler
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import com.google.protobuf.any.Any as ScalaPBAny

class AssetEventKafkaProjectionHandler(
    actorSystem: ActorSystem[_],
    sendProducer: SendProducer[String, Array[Byte]]
) extends Handler[EventEnvelope[AssetEvent]]:
  given system: ActorSystem[?] = actorSystem
  given ec: ExecutionContext   = system.executionContext
  val log                      = system.log
  val topic                    = system.settings.config.getString("nft-asset-svc.kafka.topics.nft-messages")

  override def process(envelope: EventEnvelope[AssetEvent]): Future[Done] =
    given timeout: Timeout = Timeout(5.seconds)
    val event              = envelope.event
    val key                = event.id
    val producerRecord     = new ProducerRecord(topic, key, serialize(event))
    sendProducer
      .send(producerRecord)
      .map(metadata =>
        log.info("Published Tenant Event [{}] to topic/partition {}/{}", event, topic, metadata.partition)
        Done
      )
  private def serialize(event: AssetEvent): Array[Byte]                   =
    event match
      case AssetCreated(asset: NFTAsset) =>
        val message = NFTAssetCreatedMessage.defaultInstance
          .withAssetId(asset.id)
          .withAssetName(asset.name)
          .withAssetDescription(asset.description)
          .withAssetFileId(asset.fileId.getOrElse(""))
          .withAssetStatus(asset.assetStatus.value)
        ScalaPBAny.pack(message, "nft-asset/com. aw. nft. asset. entity. NFTAssetEntity. AssetCreated").toByteArray
