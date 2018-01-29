package stats.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.actor.{PoisonPill, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import io.circe.parser.decode
import io.circe.generic.auto._

import stats.models.User
import stats.services.EventsDBService
import stats.{NoSuchEvent, ScriptPlayerEvent, ScriptPlayerEventsRecorder}

import scala.concurrent.ExecutionContext


class WsService()(implicit val system: ActorSystem,
                  fm: ActorMaterializer,
                  eventsService: EventsDBService,
                  ec: ExecutionContext) {

  def createProcessor(user: User, scriptId: Long): Flow[Message, Message, NotUsed] = {
    val collectingActor = system.actorOf(ScriptPlayerEventsRecorder.props(user, scriptId))

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        // transform websocket message to domain message
        case TextMessage.Strict(text) => decode[ScriptPlayerEventsRecorder.NodeReached](text) getOrElse NoSuchEvent
        case _ => NoSuchEvent
      }.to(Sink.actorRef[ScriptPlayerEvent](collectingActor, PoisonPill))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[String](10, OverflowStrategy.fail)
        .mapMaterializedValue(_ => NotUsed)
        .map(TextMessage(_))
    // then combine both to a flow

    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

}

object WsService {
  def apply()(implicit system: ActorSystem,
              fm: ActorMaterializer,
              eventsService: EventsDBService,
              ec: ExecutionContext) = new WsService()(system, fm, eventsService, ec)
}