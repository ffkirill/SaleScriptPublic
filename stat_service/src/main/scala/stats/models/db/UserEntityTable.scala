package stats.models.db

import slick.lifted.ProvenShape
import stats.models.UserEntity
import stats.utils.DatabaseService

trait UserEntityTable {

  protected val databaseService: DatabaseService
  import databaseService.driver.api._
  import databaseService.scriptsSchema

  class Users(tag: Tag) extends Table[UserEntity](
    tag,
    Some(scriptsSchema),
    "auth_user") {

    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username: Rep[String] = column[String]("username")
    def email: Rep[String] = column[String]("email")
    def firstName: Rep[String] = column[String]("first_name")
    def lastName: Rep[String] = column[String]("last_name")

    def * : ProvenShape[UserEntity] = (
      id,
      username,
      email,
      firstName,
      lastName) <> ((UserEntity.apply _).tupled, UserEntity.unapply)
  }

  val users: TableQuery[Users] = TableQuery[Users]

}
