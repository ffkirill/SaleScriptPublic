package scenarist.model

import org.scalajs.dom.ext.Ajax
import org.threeten.bp.LocalDateTime


object BackendApi {
  import utils.CsrfToken._

  val scriptApiEndpoint = "/api/scripts"
  val statsQueryEndpoint = "/stats-v1-query"

  def formatDateForUrl(date: LocalDateTime): String =
    date.toString

  def scriptPermissionsEndpoint(pk: String) =
    s"$scriptApiEndpoint/$pk/permissions"

  lazy val baseHeaders = Map(
    "Content-Type" -> "application/json",
    "Accept" -> "application/json")

  lazy val unsafeHeaders = baseHeaders + getCsrfTokenPair

  def getScriptsList = Ajax.get(
    scriptApiEndpoint,
    headers = baseHeaders)

  def getScriptDetail(pk: String)= Ajax.get(
    s"$scriptApiEndpoint/$pk",
    headers = baseHeaders)

  def createScript(data: Ajax.InputData) = Ajax.post(
    s"$scriptApiEndpoint/",
    data = data, headers = unsafeHeaders)

  def updateScript(pk: String, data: Ajax.InputData) = Ajax.put(
    s"$scriptApiEndpoint/$pk/", data=data, headers = unsafeHeaders)

  def deleteScript(pk: String) = Ajax.delete(
    s"$scriptApiEndpoint/$pk/",
    headers = unsafeHeaders)

  def getScriptPermissions(pk: String) = Ajax.get(
    scriptPermissionsEndpoint(pk),
    headers = baseHeaders
  )

  def createScriptPermission(pk: String, data: Ajax.InputData) = Ajax.post(
    s"${scriptPermissionsEndpoint(pk)}/",
    headers = unsafeHeaders,
    data = data
  )

  def updateScriptPermission(scriptPk: String, pk: String, data: Ajax.InputData) = Ajax.put(
    s"${scriptPermissionsEndpoint(scriptPk)}/$pk/",
    headers = unsafeHeaders,
    data = data
  )

  def deleteScriptPermission(scriptPk: String, pk: String) = Ajax.delete(
    s"${scriptPermissionsEndpoint(scriptPk)}/$pk/",
    headers = unsafeHeaders
  )

  def getOwnScriptsStats(rangeStart: Option[LocalDateTime] = None,
                         rangeEnd: Option[LocalDateTime] = None,
                         groupByUser: Boolean = false,
                         groupByDate: Option[String] = None) = {
    val params = Seq(
      rangeStart.map("dateRangeStart" -> formatDateForUrl(_)),
      rangeEnd.map("dateRangeFinish" -> formatDateForUrl(_)),
      Some("groupByUser" -> groupByUser.toString),
      groupByDate map ("groupByDate" -> _)
    ) flatMap  {
      case Some(p) => Some(p._1 + "=" + p._2)
      case _ => None
    }
    val paramsString = params.mkString("?", "&", "")
    Ajax.get(
      s"$statsQueryEndpoint/summary" + paramsString,
      headers = baseHeaders)
  }
}

