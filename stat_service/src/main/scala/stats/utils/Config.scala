package stats.utils

import com.typesafe.config.ConfigFactory

trait Config {
  private val config = ConfigFactory.load()
  private val backendConfig = config.getConfig("backend")
  private val httpConfig = config.getConfig("http")
  private val databaseConfig = config.getConfig("database")

  val httpHost: String = httpConfig.getString("interface")
  val httpPort: Int = httpConfig.getInt("port")

  val jdbcUrl: String = databaseConfig.getString("url")
  val dbUser: String = databaseConfig.getString("user")
  val dbPassword: String = databaseConfig.getString("password")
  val scriptsSchema: String = databaseConfig.getString("scriptsSchema")

  val backendApiEndpoint: String = backendConfig.getString("apiEndpoint")
}
