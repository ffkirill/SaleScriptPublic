package scenarist
import scala.scalajs.js

trait i18n {
  def gettext(str: String): String
}

object i18n extends i18n{
  def gettext(str: String): String = {
    if (js.Dynamic.global.selectDynamic("gettext").isInstanceOf[js.Object])
      js.Dynamic.global.gettext(str).asInstanceOf[String]
    else
      str
  }
}