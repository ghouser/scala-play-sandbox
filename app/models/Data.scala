package models

import play.api.libs.json.{Json, Writes}

object Data {
  case class Reply(metadata: Metadata, rows: List[Map[String, String]], errors: List[Map[String, String]])
  object Reply {
    implicit val jsonWrites: Writes[Reply] = Json.writes[Reply]
  }
  case class Metadata(importName: String, numRecords: Int)
  object Metadata {
    implicit val jsonWrites: Writes[Metadata] = Json.writes[Metadata]
  }
}
