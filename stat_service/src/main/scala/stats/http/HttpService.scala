package stats.http

import java.time.LocalDateTime

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import stats.models.User
import stats.services.EventsDBService
import stats.utils.Protocols

import scala.concurrent.ExecutionContext


private object HttpServiceTypes {
  type EventsRecorderFlowFactoryT = ((User, Long) => Flow[Message, Message, NotUsed])
}

class HttpService(eventsRecorderFlowFactory: HttpServiceTypes.EventsRecorderFlowFactoryT)
                 (implicit ec: ExecutionContext,
                  val externalService: ExternalService,
                  eventsService: EventsDBService)
  extends CirceSupport with Protocols with SecurityDirectives {
  import eventsService.databaseService.driver.api.DateTruncKind

  def checkUserRightsOnScript(user: User, id: Long): Boolean = {
    user.isSuperuser || user.ownScripts.keys.toSet.contains(id)
  }

  def scriptSummary(ids: Option[Seq[Long]]): Route =
    parameters(
      'dateRangeStart.as[LocalDateTime].?,
      'dateRangeFinish.as[LocalDateTime].?,
      'groupByUser.as[Boolean] ? false,
      'groupByDate.as[DateTruncKind].?
    ) { (fromDate, tillDate, groupByUser, groupByDate) =>
      complete(
        eventsService.getScriptSummary(
          ids,
          fromDate,
          tillDate,
          groupByUser,
          groupByDate
        ).map(_.asJson))
    }

  val route: Route =
    authenticate { user =>
      get {
        path("collect") {
          parameter("scriptId".as[Long]) {
            scriptId => handleWebSocketMessages(eventsRecorderFlowFactory(user, scriptId))
          }
        } ~
          pathPrefix("stats-v1-query") {
            pathPrefix(LongNumber) { id =>
              authorize(checkUserRightsOnScript(user, id)) {
                path("summary") {
                  scriptSummary(Some(Seq(id)))
                }
              }
            } ~
              path("summary") {
                scriptSummary(Some(user.ownScripts.keys.toSeq))
              } ~
              path("summary-all") {
                authorize(user.isSuperuser) {
                  scriptSummary(None)
                }
              }
          }
      }
    }
}

object HttpService {
  def apply(wsMessagesProcessorFactory: HttpServiceTypes.EventsRecorderFlowFactoryT)
           (implicit ec: ExecutionContext,
            externalService: ExternalService,
            eventsService: EventsDBService): HttpService =
    new HttpService(wsMessagesProcessorFactory)(ec, externalService, eventsService)
}
