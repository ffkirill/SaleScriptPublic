package stats.models

import java.sql.Timestamp
import java.util.UUID

case class EventEntity(id: Option[Long] = None,
                       userId: Long,
                       scriptId: Long,
                       fromNodeId: Option[UUID] = None,
                       toNodeId: Option[UUID] = None,
                       reachedGoalId: Option[Long] = None,
                       textFrom: Option[String] = None,
                       textTo: Option[String] = None,
                       timestamp: Timestamp) {
}
