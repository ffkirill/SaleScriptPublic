package stats.models.db


import slick.lifted.ProvenShape
import stats.models.ScriptEntity
import stats.utils.DatabaseService

trait ScriptEntityTable {

  protected val databaseService: DatabaseService
  import databaseService.driver.api._
  import databaseService.scriptsSchema

  class Scripts(tag: Tag) extends Table[ScriptEntity](
    tag,
    Some(scriptsSchema),
    "scripts_script") {

    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title")
    def text: Rep[String] = column[String]("text")
    def description: Rep[String] = column[String]("description")

    def * : ProvenShape[ScriptEntity] = (
      id,
      title,
      text,
      description) <> ((ScriptEntity.apply _).tupled, ScriptEntity.unapply)
  }

  val scripts: TableQuery[Scripts] = TableQuery[Scripts]

}
