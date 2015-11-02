package com.gu.mobile.notifications.client.models

import play.api.libs.json._

object Regions {

  val regions: Map[String, Region] = Map(
    "uk" -> UK,
    "us" -> US,
    "au" -> AU,
    "international" -> International
  )

  sealed trait Region

  case object UK extends Region {
    override def toString = "uk"
  }

  case object US extends Region {
    override def toString = "us"
  }

  case object AU extends Region {
    override def toString = "au"
  }

  case object International extends Region {
    override def toString = "international"
  }

  object Region {
    implicit val jf = new Format[Region] {
      override def reads(json: JsValue): JsResult[Region] = json match {
        case JsString("uk") => JsSuccess(UK)
        case JsString("us") => JsSuccess(US)
        case JsString("au") => JsSuccess(AU)
        case JsString("international") => JsSuccess(International)
        case JsString(unknown) => JsError(s"Unkown region [$unknown]")
        case _ => JsError(s"Unknown type $json")
      }

      override def writes(region: Region): JsValue = JsString(region.toString)
    }
  }
}