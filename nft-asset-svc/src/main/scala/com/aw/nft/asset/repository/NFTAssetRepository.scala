package com.aw.nft.asset.repository

import org.slf4j.LoggerFactory
import scalikejdbc.{sqls, *}
import com.aw.nft.asset.model.{AssetStatus, DoesNotExistStatus, ActiveStatus, DeletedStatus, NFTAsset}

import com.aw.nft.asset.repository.Columns.*
import com.aw.nft.asset.repository.Tables.*

trait NFTAssetRepository:
  def upsert(asset: NFTAsset)(using s: DBSession = AutoSession): Unit
  def get(id: String)(using s: DBSession = AutoSession): Option[NFTAsset]
  def getByFileId(id: String)(using s: DBSession = AutoSession): Option[NFTAsset]
  def delete(id: String)(using s: DBSession = AutoSession): Unit

object AssetSqlSyntaxSupport extends SQLSyntaxSupport[NFTAsset]:
  override val tableName: String = nftAssetTableName

  def apply(m: ResultName[NFTAsset])(rs: WrappedResultSet): NFTAsset =
    val status: AssetStatus =
      val statusString  = rs.stringOpt(m.column(assetStatusColumnName))
      val statusMessage = rs.string(m.column(assetStatusMessageColumnName))
      statusString match
        case Some("doesNotExist") => DoesNotExistStatus(statusMessage)
        case Some("active")       => ActiveStatus(statusMessage)
        case Some("deleted")      => DeletedStatus(statusMessage)
        case Some(_)              => throw new IllegalArgumentException(s"Invalid asset status: $statusString")
        case None                 => throw new IllegalArgumentException(s"Invalid asset status: $statusString")
    NFTAsset(
      id = rs.string(m.column(assetIdColumnName)),
      name = rs.stringOpt(m.column(assetNameColumnName)).getOrElse(""),
      description = rs.stringOpt(m.column(assetDescriptionColumnName)).getOrElse(""),
      fileId = rs.stringOpt(m.column(assetFileIdColumnName)),
      assetStatus = status
    )

class NFTAssetRepositoryImpl() extends NFTAssetRepository:
  private val log = LoggerFactory.getLogger(getClass)

  override def upsert(asset: NFTAsset)(using s: DBSession = AutoSession): Unit =
    val m = AssetSqlSyntaxSupport.column
    withSQL {
      insert
        .into(AssetSqlSyntaxSupport)
        .namedValues(
          m.column(assetIdColumnName)            -> asset.id,
          m.column(assetNameColumnName)          -> asset.name,
          m.column(assetDescriptionColumnName)   -> asset.description,
          m.column(assetFileIdColumnName)        -> asset.fileId,
          m.column(assetStatusColumnName)        -> asset.assetStatus.value,
          m.column(assetStatusMessageColumnName) -> asset.assetStatus.message
        )
    }.update.apply()

  override def get(id: String)(using s: DBSession): Option[NFTAsset] =
    val m = AssetSqlSyntaxSupport.syntax("m")
    withSQL {
      select
        .from(AssetSqlSyntaxSupport as m)
        .where
        .eq(m.column(assetIdColumnName), id)
    }.map(AssetSqlSyntaxSupport(m.resultName)).single.apply()

  override def getByFileId(id: String)(using s: DBSession): Option[NFTAsset] =
    val m = AssetSqlSyntaxSupport.syntax("m")
    withSQL {
      select
        .from(AssetSqlSyntaxSupport as m)
        .where
        .eq(m.column(assetFileIdColumnName), id)
    }.map(AssetSqlSyntaxSupport(m.resultName)).single.apply()

  override def delete(id: String)(using s: DBSession): Unit =
    val m = AssetSqlSyntaxSupport.syntax("m")
    withSQL {
      deleteFrom(AssetSqlSyntaxSupport as m).where
        .eq(m.column(assetIdColumnName), id)
    }.update.apply()
