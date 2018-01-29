package scenarist.containers

import diode.data.Pot
import diode.Action
import diode.react._
import japgolly.scalajs.react._
import org.scalajs.dom.raw.BeforeUnloadEvent
import org.scalajs.dom.window
import org.scalajs.dom.ext.KeyCode
import japgolly.scalajs.react.vdom.prefix_<^._
import scenarist.components.{NodePropertiesEditor, ScriptGraph, ScriptsCombobox}
import scenarist.i18n.gettext
import scenarist.model.questions._
import scenarist.{AppCircuit, AppModel}


object EditorView {

  case class Props(proxy: ModelProxy[AppModel])

  case class State(selection: Option[DisplayableNode], linkTriggered: Boolean)

  val editorRef = Ref("editor")

  def saveScript(dispatch: Action => Callback, e: ReactEvent): Callback = {
    dispatch(SaveScript) >>
      Callback{e.preventDefault()}
  }

  class Backend($: BackendScope[Props, State]) {

    def addQuestion(dispatch: Action => Callback, p: Props, s: State): Callback = {
      val id = ObjectId.random
      val effect = dispatch(
        AddQuestion(100, 100, gettext("New question"), Some(id)))
      (s.selection match {
        case Some(reply: Reply) =>
          effect >> dispatch(AddConnection(reply.id, id))
        case _ =>
          effect
      }) >> $.modState(_.copy(selection = p.proxy.zoom(_.phrases.get(id)).value))
    }

    def addOption(dispatch: Action => Callback, p: Props, phrase: Phrase): Callback = {
      val id = ObjectId.random
      (dispatch(AddAnswer(phrase.id, gettext("New option"), Some(id)))
       >> $.modState(_.copy(selection = p.proxy.zoom(_.replies.get(id)).value)))
    }

    def removeItemTriggered(dispatch: Action => Callback, p: Props, s: State): Callback = (s.selection match {
      case Some(phrase: Phrase) => dispatch(RemoveQuestion(phrase.id))
      case Some(reply: Reply) => p.proxy.zoom(_.phrases.find(_._2.replies.exists(_==reply.id)).map(pair =>
        dispatch(RemoveAnswer(pair._1, reply.id)))
        getOrElse Callback.empty).value
      case _ => Callback.empty
    }) >> $.modState(_.copy(selection = Option.empty))

    def hotKey(dispatch: Action => Callback, p: Props, s: State, e: ReactKeyboardEvent): Callback = {
      def plainKey = CallbackOption.keyCodeSwitch(e) {
        case KeyCode.F2 => saveScript(dispatch, e)
        case KeyCode.F7 => addQuestion(dispatch, p, s)
        case KeyCode.F8 => removeItemTriggered(dispatch, p, s)
        case KeyCode.F9 => triStateLinkButtonSelector(p, s,
          cancel = cancelLinkTriggered(dispatch, p, s),
          unlink = unlinkTriggered(dispatch, p, s),
          link = linkTriggered(dispatch, p, s),
          default = Callback.empty
        )
      }
      def shiftKey = CallbackOption.keyCodeSwitch(e, shiftKey = true) {
        case KeyCode.F7 => (s.selection match {
          case Some(phrase: Phrase) => addOption(dispatch, p, phrase)
          case _ => Callback.empty
        }) >> Callback{println("Shift-F7")}
      }
      (plainKey orElse shiftKey) >> e.preventDefaultCB
    }

    def onScaleChanged(dispatch: Action => Callback, e: ReactEventI): Callback = {
      dispatch(ScaleChanged(e.target.value.toInt))
    }

    def onSelectionChanged(dispatch: Action => Callback, p: Props, s: State,
                           sel: Option[DisplayableNode]): Callback = {
      if (s.linkTriggered) {
        def addConnection(from: Reply, to: DisplayableNode): Callback = {
          dispatch(AddConnection(from.id, to.id)) >>
            $.modState(_.copy(selection = Some(to), linkTriggered = false))
        }
        (s.selection, sel) match {
          case (Some(from: Reply), Some(to: Phrase)) =>
            addConnection(from, to)
          case (Some(from: Reply), Some(EntrySuccess)) =>
            addConnection(from, EntrySuccess)
          case (_, None) =>
            $.modState(_.copy(linkTriggered = false))
          case (_, _) =>
            Callback.empty
        }
      } else
        $.modState(_.copy(selection = sel))
    }

    def triStateLinkButtonSelector[T](p: Props, s: State, cancel: T, unlink: T, link: T,
                                      default: T): T =  s.selection.map(sel =>
      (sel, p.proxy.zoom(_.connections contains sel.id).value)).map {
        case (reply: Reply, _) if s.linkTriggered => cancel
        case (reply: Reply, true) => unlink
        case (reply: Reply, false) => link
        case _ => default
    } getOrElse default

    def cancelLinkTriggered(dispatch: Action => Callback, p: Props, s: State): Callback =
      $.modState(_.copy(linkTriggered = false))

    def unlinkTriggered(dispatch: Action => Callback, p: Props, s: State): Callback = s.selection.map(sel =>
      dispatch(RemoveConnection(sel.id))).getOrElse(Callback.empty) >> $.modState(_.copy(linkTriggered = false))

    def linkTriggered(dispatch: Action => Callback, p: Props, s: State): Callback =
      $.modState(_.copy(linkTriggered = true))

    def toolbar(p: Props, s: State) = {
      val scale = p.proxy.zoom(_.scale).value
      val dispatch: Action => Callback = p.proxy.dispatchCB
      <.div(^.className := "editor-toolbar",
        //Add question button
        <.button(
          gettext("Add question") + " (F7) ",
          ^.tpe := "button",
          ^.className := "btn btn-default",
          ^.disabled := s.linkTriggered,
          ^.onClick --> addQuestion(dispatch, p, s),
          ^.onKeyDown --> Callback{println("key")}),
        //Add option optional button
        s.selection.map {
          case phrase: Phrase =>
            <.button(
              gettext("Add option")  + " (Shift+F7) ",
              ^.tpe := "button",
              ^.className := "btn btn-default",
              ^.onClick --> addOption(dispatch, p, phrase))
          case _ =>
            EmptyTag
        },
        //Remove optional button
        s.selection.map {
          case phrase: Phrase =>
            <.button(
              gettext("Remove question") + " (F8) ",
              ^.tpe := "button",
              ^.className := "btn btn-default",
              ^.disabled := s.linkTriggered,
              ^.onClick --> removeItemTriggered(dispatch, p, s))
          case reply: Reply =>
            <.button(
              gettext("Remove option") + " (F8) ",
              ^.tpe := "button",
              ^.disabled := s.linkTriggered,
              ^.className := "btn btn-default",
              ^.onClick --> removeItemTriggered(dispatch, p, s))
          case _ =>
            EmptyTag
        },
        //Link button
        triStateLinkButtonSelector(p, s,
          cancel = <.button(gettext("Cancel") + " (F9) ",
            ^.tpe := "button",
            ^.className := "btn btn-danger",
            ^.onClick --> cancelLinkTriggered(dispatch, p, s)),
          unlink = <.button(gettext("Unlink") + " (F9) ",
            ^.tpe := "button",
            ^.className := "btn btn-default",
            ^.onClick --> unlinkTriggered(dispatch, p, s)),
          link = <.button(gettext("Link") + " (F9) ",
            ^.tpe := "button",
            ^.className := "btn btn-default",
            ^.onClick --> linkTriggered(dispatch, p, s)),
          default = EmptyTag),
          <.span(^.className := "form-group form-inline pull-right",
          ^.marginBottom := 0,
          <.span(gettext("Zoom") + s": $scale% "),
          <.input(^.`type` := "range", ^.step := 10, ^.min := 10, ^.max := 200,
            ^.value := scale,
            ^.className := "form-control",
            ^.onChange ==> ((e: ReactEventI) => onScaleChanged(dispatch, e)))
        ))
    }

    def sidebar(p: Props, s: State) = {
      val saved = p.proxy.zoom(_.saved).value
      val dispatch: Action => Callback = p.proxy.dispatchCB
      val selection = s.selection match {
        case Some(node: Phrase) =>
          p.proxy.zoom(_.phrases.get(node.id)).value
        case Some(node: Reply) =>
          p.proxy.zoom(_.replies.get(node.id)).value
        case _ =>
          s.selection
      }
      val metadata = p.proxy.zoom(_.metadata).value
      <.div(
        ^.className := "editor-sidebar",
        <.label(gettext("Scripts")),
        ScriptsCombobox(ScriptsCombobox.Props(
          p.proxy.zoom { m => m.scripts.copy(
            list=m.scripts.list.filter(_._2.hasPermChange)) } .value,
          (current: Pot[String])  => dispatch(ScriptSelected(current)), dispatch(LoadScriptList)
        )),
        <.form(
          <.div(^.className := "form-group",
            <.label(^.htmlFor := "inputTitle", gettext("Script name")),
            <.input(^.`type` := "text",
              ^.className := "form-control",
              ^.id := "inputTitle", ^.placeholder := gettext("Script name"),
              ^.value := metadata.title,
              ^.onChange ==> {e: ReactEventI =>
                dispatch(ScriptTitleChanged(e.target.value))}
            )
          ),
          <.div(^.className := "form-group",
            <.label(^.htmlFor := "inputDescription", gettext("Description")),
            <.textarea(^.rows := 3,
              ^.className := "form-control",
              ^.id := "inputDescription", ^.placeholder := gettext("Description"),
              ^.value := metadata.description,
              ^.onChange ==> {e: ReactEventTA =>
                dispatch(ScriptDescriptionChanged(e.target.value))}
            )
          ),
          <.button(^.`type` := "submit",
            ^.className := "btn btn-primary",
            ^.disabled := metadata.title.trim.isEmpty || saved,
            ^.onClick ==> ((e: ReactEventI) => saveScript(dispatch, e)),
             saved ?= (^.title := gettext("There's no changes to save")),
            gettext("Save") + " (F2) "
          )
        ),
        <.p(),
        NodePropertiesEditor(NodePropertiesEditor.Props(
          selection,
          p.proxy
        ))
      )
    }

    def paper(p: Props, s: State) = {
      val dispatch: Action => Callback = p.proxy.dispatchCB
      val ScriptGraphProps = p.proxy.zoom(model => ScriptGraph.Props(
        phrases = model.phrases,
        replies = model.replies,
        connections = model.connections,
        scale = model.scale,
        onSelected = node => onSelectionChanged(dispatch, p, s, node),
        select = s.selection.map(_.id.toString),
        highlightTarget = s.linkTriggered
      )).value
      <.div(^.className := "editor-paper",
        toolbar(p, s),
        <.div(^.className := "editor-viewport-scroller",
          ^.onClick ==> ((e:ReactEventH) =>
            onSelectionChanged(dispatch, p, s, Option.empty)),
          ScriptGraph(ScriptGraphProps)))
    }

    def render(p: Props, s: State) = {
      val dispatch: Action => Callback = p.proxy.dispatchCB
      <.div(^.className := "editor-wrapper",
        ^.ref := editorRef,
        ^.tabIndex := 0,
        ^.onKeyDown ==> ((e: ReactKeyboardEvent) => hotKey(dispatch, p, s, e)),
        paper(p, s),
        sidebar(p, s))
    }
  }

  val component = ReactComponentB[Props]("CEditorPaper")
    .initialState(State(selection = Option.empty, linkTriggered = false))
    .renderBackend[Backend]
    .componentDidMount(scope => Callback {
      window.onbeforeunload = (e:BeforeUnloadEvent) =>
        if (!scope.props.proxy.zoom(_.saved).value) gettext("There are unsaved data")
    } >> editorRef(scope).tryFocus)
    .build

  def apply() = {
    val modelData = AppCircuit.connect(p => p)
    modelData(proxy => component(Props(proxy)))
  }
}