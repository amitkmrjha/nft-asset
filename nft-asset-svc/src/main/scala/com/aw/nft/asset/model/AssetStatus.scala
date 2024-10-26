package com.aw.nft.asset.model

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[DoesNotExistStatus], name = "doesNotExist"),
    new JsonSubTypes.Type(value = classOf[ActiveStatus], name = "active"),
    new JsonSubTypes.Type(value = classOf[DeletedStatus], name = "deleted")
  )
) sealed trait AssetStatus:
  def value: String
  def message: String

case class DoesNotExistStatus(message: String) extends AssetStatus:
  def value: String = "doesNotExist"
object DoesNotExistStatus:
  def apply(): DoesNotExistStatus = DoesNotExistStatus("Asset is being initialized")

case class ActiveStatus(message: String) extends AssetStatus:
  def value: String = "active"
object ActiveStatus:
  def apply(): ActiveStatus = ActiveStatus("Asset is active and ready to be used .")

case class DeletedStatus(message: String) extends AssetStatus:
  def value: String = "deleted"
object DeletedStatus:
  def apply(): DeletedStatus = DeletedStatus("Asset is deleted and no longer available.")
