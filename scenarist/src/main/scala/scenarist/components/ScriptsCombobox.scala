package scenarist.components

import diode.data.{Ready, Pot}

import japgolly.scalajs.react._
import japgolly.scalajs.react.{ReactComponentB, BackendScope}
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.window


import scenarist.model.questions.ScriptList

import utils.Url.getUrlParameter

object ScriptsCombobox {
  case class Props(scripts: ScriptList,
                   dispatchSelected: Pot[String] => Callback,
                   dispatchLoadList: Callback)
  case class State()

  val none = "none"

  class Backend($: BackendScope[Props, State]) {

    def scriptSelected(p: Props)(e: ReactEventI) = {
      Callback(window.history.pushState((), "",
        s"?script=${e.target.value}")) >>
      p.dispatchSelected(
        if (e.target.value == none)
          Pot.empty
        else
          Ready(e.target.value)
        )
    }

    def render(p: Props, s: State) = {
      <.select(^.value := p.scripts.current.getOrElse(none),
        ^.className := "form-control",
        ^.onChange ==> scriptSelected(p),
        p.scripts.list map { script =>
          <.option(^.key := script._1, ^.value := script._1, script._2.title)
        },
        <.option(^.value := none, ^.key := none, "----")
      )
    }
  }

  val component = ReactComponentB[Props]("CScriptsCombobox")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount(compScope =>
      compScope.props.dispatchLoadList
        >> {
        getUrlParameter("script").filterNot(_ == ScriptsCombobox.none) match {
          case Some(pk: String) =>
            compScope.props.dispatchSelected(Ready(pk))
          case _ =>
            Callback.empty
      }})
    .build

  def apply(p: Props) = component(p)

}

