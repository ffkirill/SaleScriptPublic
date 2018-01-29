package utils
import org.scalajs.dom.window
import scala.scalajs.js.URIUtils.decodeURIComponent

object Url {
  def getUrlParameter(param: String): Option[String] = {
    window.location.search.substring(1).split('&').view
      .map(_.split('='))
      .find(p=>p.headOption.contains(param))
      .flatMap(item => item.lastOption)
      .map(decodeURIComponent)
  }
}
