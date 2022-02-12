package models

import play.api.libs.json.{Json, Writes}

object data {
  case class Reply(rows: List[Map[String, String]], errors: List[Map[String, String]])
  object Reply {
    implicit val jsonWrites: Writes[Reply] = Json.writes[Reply]
  }
}
