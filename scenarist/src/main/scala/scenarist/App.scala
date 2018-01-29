package scenarist

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import scenarist.containers.{StatsSummaryView, EditorView, PlayerView, PreferencesView}
import utils.CsrfToken.getCsrfToken

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import utils.Url

@JSExport("App")
object App extends JSApp {

  @JSExport
  override def main(): Unit = {
    AppCircuit.addProcessor(new DataAffectionsProcessor())

    def onUiTabClick(node: dom.html.Anchor)(e: dom.Event): Unit = {
      val currentScript = Url.getUrlParameter("script")
      currentScript.foreach(scriptId => {
        e.preventDefault()
        dom.window.location.href = s"${node.href}?script=$scriptId"
      })
    }

    dom.document.addEventListener("DOMContentLoaded", (e: dom.Event) => {
      val nodeList = dom.document.querySelectorAll("a[data-update-params=true]")
      val length = nodeList.length
      for (idx <- 0 until length) {
        val node = nodeList.item(idx)
        node.addEventListener("click", onUiTabClick(node.asInstanceOf[dom.html.Anchor]) _ )
      }

    })

  }

  @JSExport
  def renderEditor(sel: String): Unit = {
    ReactDOM.render(EditorView(),
      dom.document.querySelector(sel))
  }

  @JSExport
  def renderViewer(sel: String): Unit = {
    ReactDOM.render(PlayerView(),
      dom.document.querySelector(sel))
  }

  @JSExport
  def renderPrefs(sel: String): Unit = {
    ReactDOM.render(PreferencesView(),
      dom.document.querySelector(sel))
  }

  @JSExport
  def renderStatsAll(sel: String): Unit = {
    ReactDOM.render(StatsSummaryView(),
      dom.document.querySelector(sel))
  }


  @JSExport
  def csrf(): String = {
    getCsrfToken
  }

}
