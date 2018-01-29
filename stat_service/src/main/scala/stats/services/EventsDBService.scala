package stats.services

import java.sql.Timestamp
import java.time.LocalDateTime

import shapeless.syntax.std.tuple._
import stats.models.db.EventEntityTable
import stats.models.{EventEntity, ScriptGoals}
import stats.utils.DatabaseService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


case class ScriptSummaryEntry(scriptId: Long,
                              userId: Option[Long],
                              date: Option[Timestamp],
                              runCount: Int,
                              successCount: Int,
                              failCount: Int,
                              noSuchReplyCount: Int,
                              allEventsCount: Int,
                              scriptTitle: Option[String],
                              userName: Option[String],
                              userFirstName: Option[String],
                              userLastName: Option[String]
                             )

class EventsDBService(val databaseService: DatabaseService)
                     (implicit executionContext: ExecutionContext,
                      scriptsService: ScriptsDBService,
                      usersService: UsersDBService) extends EventEntityTable {

  import databaseService._
  import databaseService.driver.api._

  def get: Future[Seq[EventEntity]] = db.run(events.result)

  def getByScriptId(id: Long): Future[Seq[EventEntity]] =
    db.run(events.filter(_.scriptId === id).result)

  def getScriptSummary(ids: Option[Seq[Long]],
                       startDate: Option[LocalDateTime] = None,
                       endDate: Option[LocalDateTime] = None,
                       groupByUser: Boolean = false,
                       groupByDate: Option[DateTruncKind] = None) = {

    def aggGoalReachCount(query: Query[Events, EventEntity, Seq],
                          goal: ScriptGoals.Value) = query.map(p =>
      Case If (p.reachedGoalId === goal.id.toLong) Then 1 Else 0).sum.getOrElse(0)

    var filteredQuery: Query[Events, EventEntity, Seq] = events

    ids.foreach(v => filteredQuery = filteredQuery.filter(_.scriptId inSetBind v))
    startDate.foreach(v => filteredQuery = filteredQuery.filter(_.timestamp >= Timestamp.valueOf(v)))
    endDate.foreach(v => filteredQuery = filteredQuery.filter(_.timestamp <= Timestamp.valueOf(v)))

    def grouper(q: Events): (Rep[Long], Rep[Option[Long]], Rep[Option[Timestamp]]) = (
      q.scriptId,
      if (groupByUser) q.userId.? else None,
      groupByDate.map(v => q.timestamp.? trunc v.toString).getOrElse(None)
    )

    val groupedQuery = for {
      (group, nestedData) <- filteredQuery
        .groupBy(grouper)
    } yield {
      (group,
        (aggGoalReachCount(nestedData, ScriptGoals.scriptRan),
          aggGoalReachCount(nestedData, ScriptGoals.success),
          aggGoalReachCount(nestedData, ScriptGoals.failure),
          aggGoalReachCount(nestedData, ScriptGoals.noSuchReply),
          nestedData.length)
      )
    }

    val joinScripts = for {
      (eventsSummary, scriptsQuery) <- groupedQuery.joinLeft(scriptsService.scripts).on{
        case (((scriptId, _, _), _), scriptsQuery) => {
          scriptId === scriptsQuery.id
        }
      }
    } yield {
      (eventsSummary :+ scriptsQuery.map(_.title))
    }

    val query = if (groupByUser) {
      for {
        (eventsSummary, usersQuery) <- joinScripts.joinLeft(usersService.users).on{
          case (((_, userId, _), _, _), usersQuery) => {
            userId === usersQuery.id
          }
        }
      } yield {
        (eventsSummary :+ (
          usersQuery.map(_.username), usersQuery.map(_.firstName), usersQuery.map(_.lastName)))
      }
    } else {
      for {
        eventsSummary <- joinScripts
      } yield {
        (eventsSummary :+ (Option.empty[String], Option.empty[String], Option.empty[String]))
      }
    }

    val queryProjection = for {
      eventsSummary <- query
    } yield {
      (eventsSummary match {
        case (group, stats, scriptName, user) => (group ++ stats :+ scriptName) ++ user
      }) <> ((ScriptSummaryEntry.apply _).tupled, ScriptSummaryEntry.unapply)
    }

    val action = queryProjection.result
    action.statements.foreach(println)
    db.run(action)
  }

  def createEvent(event: EventEntity): Future[Try[EventEntity]] = {
    val query = (events returning events) += event
    db.run(query.asTry)
  }

}

object EventsDBService {
  def apply(databaseService: DatabaseService)
           (implicit executionContext: ExecutionContext,
            scriptsService: ScriptsDBService,
            usersService: UsersDBService): EventsDBService =
    new EventsDBService(databaseService)(executionContext, scriptsService, usersService)
}
