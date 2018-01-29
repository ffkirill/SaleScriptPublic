package stats.models.db

import java.sql.Timestamp
import java.util.UUID

import slick.lifted.ProvenShape
import stats.models.EventEntity
import stats.utils.DatabaseService

trait EventEntityTable {

  protected val databaseService: DatabaseService
  import databaseService.driver.api._

  class Events(tag: Tag) extends Table[EventEntity](tag, "script_eventlog") {
    def id: Rep[Option[Long]] = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def userId: Rep[Long] = column[Long]("user_id")
    def scriptId: Rep[Long] = column[Long]("script_id")
    def fromNodeId: Rep[Option[UUID]] = column[Option[UUID]]("from_node")
    def toNodeId: Rep[Option[UUID]] = column[Option[UUID]]("to_node")
    def reachedGoalId: Rep[Option[Long]] = column[Option[Long]]("reached_goal")
    def textFrom: Rep[Option[String]] = column[Option[String]]("text_from")
    def textTo: Rep[Option[String]] = column[Option[String]]("text_to")
    def timestamp: Rep[Timestamp] = column[Timestamp]("timestamp", O.SqlType("timestamp default current_timestamp"))

    def * : ProvenShape[EventEntity] = (
      id,
      userId,
      scriptId,
      fromNodeId,
      toNodeId,
      reachedGoalId,
      textFrom,
      textTo,
      timestamp) <> ((EventEntity.apply _).tupled, EventEntity.unapply)
  }

  protected val events: TableQuery[Events] = TableQuery[Events]

}
