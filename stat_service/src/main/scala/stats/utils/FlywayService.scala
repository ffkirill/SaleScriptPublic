package stats.utils

import org.flywaydb.core.Flyway

class FlywayService(jdbcUrl: String, dbUser: String, dbPassword: String) {

  private[this] val flyway = new Flyway()
  flyway.setDataSource(jdbcUrl, dbUser, dbPassword)

  def migrateDatabaseSchema() : Unit = flyway.migrate()

  def repair() : Unit = flyway.repair()

  def dropDatabase() : Unit = flyway.clean()
}

object FlywayService {
  def apply(jdbcUrl: String, dbUser: String, dbPassword: String): FlywayService =
    new FlywayService(jdbcUrl, dbUser, dbPassword)
}