package utils
import org.scalajs.dom.document
import scala.scalajs.js.URIUtils.decodeURIComponent

object CsrfToken {
  def getCsrfToken: String = {
    document.cookie.split(';').view.flatMap(
      oneCookie => {val components = oneCookie.split('=') map {_.trim}
        for (left <- components.headOption;
             right <- components.lastOption) yield (left, right)}
    ).find(pair => pair._1 == "csrftoken") map {p =>
      decodeURIComponent(p._2)}  getOrElse ""
  }

  def getCsrfTokenPair = "X-CSRFToken" -> getCsrfToken
}
