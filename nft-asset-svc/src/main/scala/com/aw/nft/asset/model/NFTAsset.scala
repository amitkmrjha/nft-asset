package com.aw.nft.asset.model

import com.aw.nft.asset.serialization.CborSerializable

case class NFTAsset(
    id: String,
    name: String,
    description: String,
    fileId: Option[String] = None,
    assetStatus: AssetStatus = DoesNotExistStatus()
) extends CborSerializable
