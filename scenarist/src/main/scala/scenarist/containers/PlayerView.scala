package scenarist.containers

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import upickle.default._
import org.scalajs.dom.{Event, WebSocket}
import org.scalajs.dom.raw.HTMLLinkElement
import scenarist.components.ScriptsCombobox
import scenarist.model.questions._
import scenarist.i18n.gettext
import scenarist.{AppCircuit, AppModel}
import utils.Metrics

object WsStatAgent {

  private var wsConnection: Option[WebSocket] = None
  private var url = ""
  private val queue = collection.mutable.Queue[String]()
  private val host = dom.window.location.host

  case class Payload(from: String, to: String, textFrom: String, textTo: String)

  def sync(): Unit = {
    wsConnection match {
      case Some(wsc: WebSocket) if wsc.readyState == WebSocket.OPEN =>
        queue.foreach(wsc.send)
        queue.clear()
      case Some(wsc: WebSocket) if wsc.readyState == WebSocket.CONNECTING =>
        None
      case _ =>
        createConnection()
    }
  }

  def createConnection(): Unit = {
    if (url.nonEmpty) {
      val _wsConnection = new WebSocket(url)
      _wsConnection.onopen = (e: Event) => sync()
      wsConnection = Some(_wsConnection)
    }
  }

  def setScriptId(scriptId: String): Unit = {
    url = s"wss://$host/collect?scriptId=$scriptId"
    createConnection()
  }

  def sendEvent(payload: Payload): Unit = {
    queue.enqueue(write(payload))
    sync()
  }

}

object AnswerView {
  def apply(question: String, answer: String): ReactTagOf[_ <: TopNode] = {
    <.div(^.className := "answer",
      <.h4(^.dangerouslySetInnerHtml(question.trim.replace("\n", "<br/>"))),
      <.p(^.dangerouslySetInnerHtml(answer.trim.replace("\n", "<br/>")))
    )
  }
}

object QuestionView {
  def apply(question: String)(children: Iterable[ReactTagOf[_ <: TopNode]]):
  ReactTagOf[_ <: TopNode] = {
    <.div(^.className := "question panel",
      <.div(^.className := "phrase",
        <.label(gettext("Your phrase") + ":"),
        <.h4(^.dangerouslySetInnerHtml(question.trim.replace("\n", "<br/>")))),
      <.div(^.className := "reply",
        <.label(gettext("Response options") + ":"),
        <.ul(children.toSeq))
    )
  }
}

object ReplyView {
  def apply(reply: Reply)(tagMod: TagMod*): ReactTagOf[_ <: TopNode] = {
    <.li(^.key := reply.id,
      <.a(
        ^.href := "#",
        ^.dangerouslySetInnerHtml(reply.text.trim.replace("\n", "<br/>")),
        tagMod
      ))
  }
}


object PlayerView {
  val noSuchReply = Reply(ObjectId.fromString("_no_such_reply"),
    gettext("No suitable option"))

  case class Props(proxy: ModelProxy[AppModel])

  case class State(question: DisplayableNode, answers: Seq[(String, String)])

  class Backend($: BackendScope[Props, State]) {
    val lastReply = Ref[HTMLLinkElement]("last-reply")

    def question(p: Props, s:State): Option[DisplayableNode] = {
      s.question match {
        case Entry =>
          for (
            phraseId <- p.proxy.zoom(_.connections.get(s.question.id)).value;
            phrase <- p.proxy.zoom(_.phrases.get(phraseId)).value
          ) yield phrase
        case p: DisplayableNode =>
          Some(p)
      }
    }

    def replies(p: Props, s: State): Iterable[Reply] = {
      question(p, s) match {
        case Some(phrase: Phrase) =>
          phrase.replies.flatMap(replyId =>
            p.proxy.zoom(_.replies.get(replyId)).value) :+ noSuchReply
        case Some(EntrySuccess) =>
          Seq(Reply(EntrySuccess.id, EntrySuccess.description))
        case _ =>
          Seq(noSuchReply)
      }
    }

    def setupWsConnection(scriptId: String): Callback = {
      Callback{WsStatAgent.setScriptId(scriptId)}
    }

    def sendEventToStat(from: String, to: String, textFrom: String, textTo: String): Callback = {
      Callback{WsStatAgent.sendEvent(WsStatAgent.Payload(from, to, textFrom, textTo))}
    }

    def onReplySelected(p: Props, s: State,
                        phrase: DisplayableNode,
                        reply: Reply)(e: ReactEventH): Callback = {
      val cb = if (s.question == EntrySuccess)
        $.modState(s => s.copy(question = Entry, answers = Seq())) >> Callback{Metrics.countPageView()}
      else {
        val nextPhrase = p.proxy.zoom(_.connections.get(reply.id)).value
          .flatMap(phraseId =>
            if (phraseId == EntrySuccess.id)
              Some(EntrySuccess)
            else
              p.proxy.zoom(_.phrases.get(phraseId)).value)

        val clickCb = nextPhrase match {
          case Some(nextPhrase: DisplayableNode) =>
            ($.modState(s => s.copy(question = nextPhrase,
              answers = s.answers :+ (
                phrase.text,
                reply.text)))
              >> sendEventToStat(phrase.id, reply.id, phrase.text, reply.text)
              >> sendEventToStat(reply.id, nextPhrase.id, reply.text, nextPhrase.text))
          case _ =>
            ($.modState(s => s.copy(question = Entry, answers = Seq()))
              >> sendEventToStat(phrase.id, reply.id, phrase.text, reply.text)
              >> sendEventToStat(reply.id, EntryFail.id, reply.text, EntryFail.text)
              >> Callback{Metrics.countPageView()})
        }
        if (s.question == Entry)
          sendEventToStat(s.question.id, phrase.id, s.question.text, phrase.text) >> clickCb
        else
          clickCb
      }
      cb >> Callback{e.preventDefault()}
    }

    def onScriptSelected(p: Props, s: State)(current: Pot[String]): Callback = {
      ($.modState(_.copy(answers = Seq.empty, question = Entry))
        >> p.proxy.dispatchCB(ScriptSelected(current))
        >> current.map(setupWsConnection).getOrElse(Callback.empty))
    }

    def render(p: Props, s: State) = {
      val _replies = replies(p, s)
      <.div(^.className := "viewer-wrapper",
        <.div(^.className := "viewer",
          s.answers.map(answer => AnswerView(answer._1, answer._2)),
          question(p, s).map(phrase =>
            QuestionView(phrase.text)(
              _replies.map(reply =>
                ReplyView(reply)(
                  _replies.lastOption.contains(reply) ?= (^.ref := lastReply),
                  ^.onClick ==> onReplySelected(p, s, phrase, reply)_))))
            .getOrElse(<.p(gettext("Scenario is not selected")))
        ),
        <.div(^.className := "viewer-sidebar",
          <.form(
            ScriptsCombobox(ScriptsCombobox.Props(
              p.proxy.zoom(_.scripts).value,
              (current: Pot[String])  => onScriptSelected(p, s)(current),
              p.proxy.dispatchCB(LoadScriptList)
            )),
            <.div(^.className := "form-group",
              <.label(gettext("Script name")),
              <.p(^.className := "form-control-static",
                p.proxy.zoom(_.metadata.title).value),
              <.label(gettext("Description")),
              <.p(^.className := "form-control-static",
                p.proxy.zoom(_.metadata.description).value))
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("CPlayerView")
    .initialState(State(question = Entry, answers = Seq()))
    .renderBackend[Backend]
    .componentDidMount(compScope => Callback{compScope.backend
      .lastReply(compScope).foreach(_.scrollIntoView())})
    .componentDidUpdate(compScope => Callback{compScope.$.backend
      .lastReply(compScope.$).foreach(_.scrollIntoView())})
    .build

  def apply() = {
    val modelData = AppCircuit.connect(p => p)
    modelData(proxy => component(Props(proxy)))
  }
}
