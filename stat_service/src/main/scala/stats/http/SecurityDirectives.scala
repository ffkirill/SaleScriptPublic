package stats.http

import akka.http.scaladsl.server.directives.{BasicDirectives, CookieDirectives, FutureDirectives, RouteDirectives}
import akka.http.scaladsl.server.Directive1
import stats.models.User


trait SecurityDirectives {

  import BasicDirectives._
  import CookieDirectives._
  import RouteDirectives._
  import FutureDirectives._

  val externalService: ExternalService

  def authenticate: Directive1[User] = {
    cookie("sessionid").flatMap { cookiePair =>
      onSuccess(externalService.fetchUserCredential(cookiePair.value)).flatMap {
        case Right(user) => provide(user)
        case _       => reject
      }
    }
  }
}
