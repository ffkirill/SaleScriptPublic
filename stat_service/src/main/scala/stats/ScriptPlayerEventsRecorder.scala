package stats

import java.sql.Timestamp
import java.util.UUID

import scala.util.Try
import akka.actor.{Actor, Props}
import stats.models.{EventEntity, ScriptGoals, User}
import stats.services.EventsDBService

import scala.concurrent.ExecutionContext

sealed trait ScriptPlayerEvent
case object NoSuchEvent extends ScriptPlayerEvent

object ScriptPlayerEventsRecorder {
  final case class NodeReached(from: String, to: String,
                               textFrom: String, textTo: String) extends ScriptPlayerEvent

  val success = "__success__"
  val fail = "__fail__"
  val noSuchReply = "_no_such_reply"
  val entry = "__entry__"

  def props(user: User, scriptId: Long)
           (implicit ec:ExecutionContext, eventsService: EventsDBService) =
    Props(new ScriptPlayerEventsRecorder(user, scriptId)(ec, eventsService))
}

class ScriptPlayerEventsRecorder(user: User, scriptId: Long)
                                (implicit ec: ExecutionContext,
                                 eventsService: EventsDBService) extends Actor {

  def wrapText(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  def receive: PartialFunction[Any, Unit] = {
    case ScriptPlayerEventsRecorder.NodeReached(from, to, textFrom, textTo) =>
      val fromUUID = Try(UUID.fromString(from)).toOption
      val toUUID = Try(UUID.fromString(to)).toOption

      eventsService.createEvent(EventEntity(
        userId = user.id,
        scriptId = scriptId,
        fromNodeId = fromUUID,
        toNodeId = toUUID,
        textFrom = wrapText(textFrom),
        textTo = wrapText(textTo),
        timestamp = new Timestamp(System.currentTimeMillis()),
        reachedGoalId = (from, to) match {
          case (ScriptPlayerEventsRecorder.entry, _) =>
            Some(ScriptGoals.scriptRan.id)
          case (_, ScriptPlayerEventsRecorder.success) =>
            Some(ScriptGoals.success.id)
          case (_, ScriptPlayerEventsRecorder.fail) =>
            Some(ScriptGoals.failure.id)
          case (_, ScriptPlayerEventsRecorder.noSuchReply) =>
            Some(ScriptGoals.noSuchReply.id)
          case _ =>
            None
        }
      ))
  }
}
