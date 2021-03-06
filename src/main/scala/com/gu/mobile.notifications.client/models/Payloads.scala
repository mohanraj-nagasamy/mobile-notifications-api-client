package com.gu.mobile.notifications.client.models

import java.net.URI
import java.util.UUID

import com.gu.mobile.notifications.client.lib.JsonFormatsHelper._
import com.gu.mobile.notifications.client.models.Importance.{Importance, Major}
import play.api.libs.json._
import NotificationPayloadType._
sealed case class GuardianItemType(mobileAggregatorPrefix: String)
object GuardianItemType {
  implicit val jf = Json.writes[GuardianItemType]
}

object GITSection extends GuardianItemType("section")
object GITTag extends GuardianItemType("latest")
object GITContent extends GuardianItemType("item-trimmed")

sealed trait Link
object Link {
  implicit val jf = new Writes[Link] {
    override def writes(o: Link): JsValue = o match {
      case l: ExternalLink => ExternalLink.jf.writes(l)
      case l: GuardianLinkDetails => GuardianLinkDetails.jf.writes(l)
    }
  }
}

object ExternalLink { implicit val jf = Json.writes[ExternalLink] }
case class ExternalLink(url: String) extends Link {
  override val toString = url
}
case class GuardianLinkDetails(
  contentApiId: String,
  shortUrl: Option[String],
  title: String,
  thumbnail: Option[String],
  git: GuardianItemType,
  blockId: Option[String] = None) extends Link {
  val webUrl = s"http://www.theguardian.com/$contentApiId"
  override val toString = webUrl
}

object GuardianLinkDetails {
  implicit val jf = Json.writes[GuardianLinkDetails]
}

sealed trait GoalType
object OwnGoalType extends GoalType
object PenaltyGoalType extends GoalType
object DefaultGoalType extends GoalType

object GoalType {
  implicit val jf = new Writes[GoalType] {
    override def writes(o: GoalType): JsValue = o match {
      case OwnGoalType => JsString("Own")
      case PenaltyGoalType => JsString("Penalty")
      case DefaultGoalType => JsString("Default")
    }
  }
}

sealed trait NotificationPayload {
  def id: String
  def title: String
  def `type`: NotificationPayloadType
  def message: String
  def thumbnailUrl: Option[URI]
  def sender: String
  def importance: Importance
  def topic: Set[Topic]
  def debug: Boolean
}

object NotificationPayload {
  implicit val jf = new Writes[NotificationPayload] {
    override def writes(o: NotificationPayload): JsValue = o match {
      case n: BreakingNewsPayload => BreakingNewsPayload.jf.writes(n)
      case n: ContentAlertPayload => ContentAlertPayload.jf.writes(n)
      case n: GoalAlertPayload => GoalAlertPayload.jf.writes(n)
    }
  }
}
sealed trait NotificationWithLink extends NotificationPayload {
  def link: Link
}

object BreakingNewsPayload { val jf = Json.writes[BreakingNewsPayload] withTypeString BreakingNews.toString }
case class BreakingNewsPayload(
  id: String = UUID.randomUUID.toString,
  title: String = "The Guardian",
  message: String,
  thumbnailUrl: Option[URI],
  sender: String,
  link: Link,
  imageUrl: Option[URI],
  importance: Importance,
  topic: Set[Topic],
  debug: Boolean
) extends NotificationWithLink {
  val `type` = BreakingNews
}

object ContentAlertPayload {
  implicit val jf = new Writes[ContentAlertPayload] {
    override def writes(o: ContentAlertPayload) = (Json.writes[ContentAlertPayload] withAdditionalStringFields Map("type" -> ContentAlert.toString, "id" -> o.id)).writes(o)
  }
}

case class ContentAlertPayload(
  title: String,
  message: String,
  thumbnailUrl: Option[URI],
  sender: String,
  link: Link,
  imageUrl: Option[URI] = None,
  importance: Importance,
  topic: Set[Topic],
  debug: Boolean
) extends NotificationWithLink with derivedId {
  val `type` = ContentAlert

  override val derivedId: String = {
    def newContentIdentifier(contentApiId: String) = s"contentNotifications/newArticle/$contentApiId"
    def newBlockIdentifier(contentApiId: String, blockId: String) = s"contentNotifications/newBlock/$contentApiId/$blockId"

    val contentCoordinates = link match {
      case GuardianLinkDetails(contentApiId, _, _, _, _, blockId) => (Some(contentApiId), blockId)
      case _ => (None, None)
    }

    contentCoordinates match {
      case (Some(contentApiId), Some(blockId)) => newBlockIdentifier(contentApiId, blockId)
      case (Some(contentApiId), None) => newContentIdentifier(contentApiId)
      case (None, _) => UUID.randomUUID.toString
    }
  }
}

object GoalAlertPayload {
  implicit val jf = new Writes[GoalAlertPayload] {
    override def writes(o: GoalAlertPayload) = (Json.writes[GoalAlertPayload] withAdditionalStringFields Map("type" -> GoalAlert.toString, "id" -> o.id)).writes(o)
  }
}
case class GoalAlertPayload(
  title: String,
  message: String,
  thumbnailUrl: Option[URI] = None,
  sender: String,
  goalType: GoalType,
  awayTeamName: String,
  awayTeamScore: Int,
  homeTeamName: String,
  homeTeamScore: Int,
  scoringTeamName: String,
  scorerName: String,
  goalMins: Int,
  otherTeamName: String,
  matchId: String,
  mapiUrl: URI,
  importance: Importance,
  topic: Set[Topic],
  debug: Boolean,
  addedTime: Option[String]
) extends NotificationPayload with derivedId {
  val `type` = GoalAlert
  override val derivedId = s"goalAlert/${matchId}/${homeTeamScore}-${awayTeamScore}/${goalMins}"
}

trait derivedId {
  val derivedId: String
  lazy val id = UUID.nameUUIDFromBytes(derivedId.getBytes).toString
}