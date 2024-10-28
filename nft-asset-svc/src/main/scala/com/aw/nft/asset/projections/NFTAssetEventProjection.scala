package com.aw.nft.asset.projections

import com.aw.nft.asset.entity.NFTAssetEntity
import com.aw.nft.asset.entity.NFTAssetEntity.AssetEvent
import com.aw.nft.asset.repository.NFTAssetRepository
import com.aw.nft.asset.utils.persistence.ScalikeJdbcSession
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings
import org.apache.pekko.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import org.apache.pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.jdbc.scaladsl.JdbcProjection
import org.apache.pekko.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import org.apache.pekko.projection.{ProjectionBehavior, ProjectionId}

object NFTAssetEventProjection:

  def init(system: ActorSystem[?], assetRepo: NFTAssetRepository): Unit =
    val log = system.log
    log.info("Starting Projection for NFT Asset Events")

    ShardedDaemonProcess(system).init(
      name = "NFTAssetEventProjection",
      NFTAssetEntity.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, assetRepo, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )

  private def createProjectionFor(
      system: ActorSystem[?],
      assetRepo: NFTAssetRepository,
      index: Int
  ): ExactlyOnceProjection[Offset, EventEnvelope[AssetEvent]] =
    val tag = NFTAssetEntity.tags(index)

    val sourceProvider: SourceProvider[Offset, EventEnvelope[AssetEvent]] =
      EventSourcedProvider.eventsByTag[AssetEvent](
        system = system,
        readJournalPluginId = JdbcReadJournal.Identifier,
        tag = tag
      )

    JdbcProjection.exactlyOnce(
      projectionId = ProjectionId("NFTAssetEventProjection", tag),
      sourceProvider,
      handler = () => new AssetEventProjectionHandler(tag, system, assetRepo),
      sessionFactory = () => new ScalikeJdbcSession()
    )(system)
