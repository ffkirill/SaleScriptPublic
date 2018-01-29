package scenarist.components

import diode.Action
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scenarist.AppModel
import scenarist.i18n._
import scenarist.model.questions._

object NodePropertiesEditor {
  case class Props(currentNode: Option[DisplayableNode],
                   proxy: ModelProxy[AppModel])
  case class State(currentText: String)

  val inputRef = Ref("input")

  class Backend($: BackendScope[Props, State]) {

    def questionChanged(dispatch: Action => Callback, phrase: Phrase, text: String): Callback = {
      $.modState(_.copy(currentText = text)) >> dispatch(
        ThrottledAction(
          UpdateQuestion(phrase.id, phrase.copy(text=text)),
          500,
          "question"
        ))
    }

    def optionChanged(dispatch: Action => Callback, reply: Reply, text: String): Callback = {
      $.modState(_.copy(currentText = text)) >> dispatch(
        ThrottledAction(
          UpdateAnswer(reply.id, reply.copy(text=text)),
          500,
          "answer"
        ))
    }

    def render(p: Props, s: State) = {
      val selection = p.currentNode
      val dispatch: Action => Callback = p.proxy.dispatchCB
      <.form(
        <.div(^.className := "form-group",
          <.label(^.htmlFor := "inputNodeText", gettext("Node properties")),
          <.textarea(
            ^.ref := inputRef,
            ^.className := "form-control",
            ^.id := "inputNodeText",
            ^.rows := 7,
            ^.placeholder := gettext("Select node to change text"),
            ^.disabled := selection.isEmpty || selection.exists(_.isInstanceOf[BuiltInNode]),
            ^.value :=  s.currentText,
            selection.map {
              case phrase: Phrase =>
                ^.onChange ==> {e: ReactEventTA => questionChanged(dispatch, phrase, e.target.value)}
              case reply: Reply =>
                ^.onChange ==> {(e:ReactKeyboardEventTA) => optionChanged(dispatch, reply, e.target.value)}
              case _ =>
                EmptyTag
            }
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("CNodeProperties")
    .initialState(State(""))
    .renderBackend[Backend]
    .componentWillReceiveProps(scope =>
      if (scope.nextProps.currentNode != scope.$.props.currentNode)
        scope.$.modState(_.copy(currentText =
          scope.nextProps.currentNode.map(_.text).getOrElse("")))
      else
        Callback.empty)
    .shouldComponentUpdate(scope =>
      scope.$.state.currentText != scope.nextState.currentText ||
      scope.$.props.currentNode != scope.nextProps.currentNode)
//    .componentDidUpdate(scope =>
//      scope.currentProps.currentNode.map { _ => inputRef(scope.$).tryFocus }
//        .getOrElse(Callback.empty)
//    )
    .build

  def apply(p: Props) = component(p)
}
