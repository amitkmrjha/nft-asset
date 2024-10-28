package com.aw.nft.asset

package object repository:
  object Tables:
    val nftAssetTableName: String = "nft_assets"

  object Columns:
    val assetIdColumnName: String            = "asset_id"
    val assetNameColumnName: String          = "name"
    val assetDescriptionColumnName: String   = "description"
    val assetFileIdColumnName: String        = "fileId"
    val assetStatusColumnName: String        = "status"
    val assetStatusMessageColumnName: String = "status_message"
