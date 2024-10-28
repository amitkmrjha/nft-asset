package com.aw.nft.asset.projections

import com.aw.nft.asset.entity.NFTAssetEntity.*
import com.aw.nft.asset.repository.NFTAssetRepository
import org.slf4j.LoggerFactory
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.jdbc.JdbcSession
import org.apache.pekko.projection.jdbc.scaladsl.JdbcHandler
import org.apache.pekko.util.Timeout

import scala.concurrent.duration.*

class AssetEventProjectionHandler(
    tag: String,
    system: ActorSystem[?],
    assetRepo: NFTAssetRepository
) extends JdbcHandler[EventEnvelope[AssetEvent], JdbcSession]():
  private val log = LoggerFactory.getLogger(getClass)

  override def process(session: JdbcSession, envelope: EventEnvelope[AssetEvent]): Unit =
    given timeout: Timeout = Timeout(5.seconds)
    envelope.event match
      case AssetCreated(asset)     =>
        log.info("AssetCreated: Creating read side NFT Asset with id {}", asset.id)
        assetRepo.upsert(asset)
      case AssetFileIdAdded(asset) =>
        log.info("AssetFileIdAdded: Updating read side NFT Asset with id {}", asset.id)
        assetRepo.upsert(asset)
      case AssetRenamed(asset)     =>
        log.info("AssetRenamed: Updating read side NFT Asset with id {}", asset.id)
        assetRepo.upsert(asset)
      case AssetRemoved(asset)     =>
        log.info("AssetRemoved: Deleting read side NFT Asset with id {}", asset.id)
        assetRepo.delete(asset.id)
