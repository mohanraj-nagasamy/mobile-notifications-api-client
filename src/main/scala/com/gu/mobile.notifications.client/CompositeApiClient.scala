package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.{NotificationPayload}

import scala.concurrent.{ExecutionContext, Future}

class CompositeApiClient(apiClients: List[ApiClient], val clientId: String = "composite") extends ApiClient {
  require(apiClients.size > 0)

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, Unit]] = {

    def sendNotification(apiClient: ApiClient): Future[Either[ErrorWithSource, Unit]] = {
      apiClient.send(notificationPayload) map {
        case Left(error) => Left(ErrorWithSource(apiClient.clientId, error))
        case Right(_) => Right()
      }
    }

    val responses = Future.sequence(apiClients.map(sendNotification))
    responses.map(aggregateResponses)
  }

  private def aggregateResponses(responses: List[Either[ErrorWithSource, Unit]]): Either[ApiClientError, Unit] = {
    val errorResponses = responses.collect {case Left(v) => v}
    val successfulResponses = responses.collect {case Right(v) => v}

    (errorResponses,successfulResponses) match {
      case (Nil, _) => Right()
      case (failures, Nil) => Left(TotalApiError(failures))
      case (failures, _) => Left(PartialApiError(failures))
    }
  }
}