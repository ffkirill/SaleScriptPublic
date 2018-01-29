package stats.services

import stats.models.db.{ScriptEntityTable, UserEntityTable}
import stats.utils.DatabaseService

import scala.concurrent.ExecutionContext

class ScriptsDBService(val databaseService: DatabaseService)
                      (implicit executionContext: ExecutionContext) extends ScriptEntityTable

object ScriptsDBService {
  def apply(databaseService: DatabaseService)
           (implicit executionContext: ExecutionContext): ScriptsDBService =
    new ScriptsDBService(databaseService)(executionContext)
}


class UsersDBService(val databaseService: DatabaseService)
                    (implicit executionContext: ExecutionContext) extends UserEntityTable

object UsersDBService {
  def apply(databaseService: DatabaseService)
           (implicit executionContext: ExecutionContext): UsersDBService =
    new UsersDBService(databaseService)(executionContext)
}
