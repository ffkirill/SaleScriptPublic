package stats.utils

import com.github.tminglei.slickpg.PgDateSupport

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

trait DateTruncateEnumExtension {
  object DateTruncKind extends Enumeration {
    val hour = Value("hour")
    val day = Value("day")
    val week = Value("week")
    val month = Value("month")
    val quarter = Value("quarter")
    val year = Value("year")
  }

  type DateTruncKind = DateTruncKind.Value
}

trait PostgresProfileWithExtensions extends slick.jdbc.PostgresProfile
  with PgDateSupport {
  override val api = MyAPI

  object MyAPI extends API
    with SimpleDateTimeImplicits
    with DateTruncateEnumExtension

}

object PostgresProfileWithExtensions extends PostgresProfileWithExtensions

class DatabaseService(jdbcUrl: String,
                      dbUser: String,
                      dbPassword: String,
                      scriptsSchemaName: String) extends {

  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)

  private val dataSource = new HikariDataSource(hikariConfig)

  def scriptsSchema: String = scriptsSchemaName

  val driver = PostgresProfileWithExtensions
  import driver.api._
  val db: driver.backend.DatabaseDef = Database.forDataSource(dataSource)
  db.createSession()
}

object DatabaseService {
  def apply(jdbcUrl: String, dbUser: String, dbPassword: String, scriptsSchemaName: String): DatabaseService =
    new DatabaseService(jdbcUrl, dbUser, dbPassword, scriptsSchemaName)
}