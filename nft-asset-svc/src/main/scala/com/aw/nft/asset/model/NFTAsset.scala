package com.aw.nft.asset.model

case class NFTAsset(
    id: String,
    name: String,
    description: String,
    fileId: Option[String] = None,
    assetStatus: AssetStatus = DoesNotExistStatus()
)
