package scenarist.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}
import scenarist.model.questions._
import scenarist.model.questions.`package`.ObjectId

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

@js.native
trait ScenarioGraphView extends js.Object {
  def initViewport(selector: String): Unit = js.native
  def draw(graph: js.Dictionary[Object], select: js.UndefOr[String],
           scroll: Boolean = false,
           highlight: Boolean = false): Unit = js.native
  var onNodeSelected: js.Function1[js.UndefOr[DisplayableNode], Unit]
}

object ScenarioGraphView {
  def apply() = js.Dynamic.newInstance(js.Dynamic.global.ScenarioGraphView)()
    .asInstanceOf[ScenarioGraphView]
}


object ScriptGraph {
  case class Props(
    phrases: Map[ObjectId, Phrase],
    replies: Map[ObjectId, Reply],
    connections: Map[ObjectId, ObjectId],
    onSelected: Option[DisplayableNode] => Callback,
    scale: Int,
    select: Option[ObjectId] = Option.empty,
    highlightTarget: Boolean = false)

  case class State()

  class Backend($: BackendScope[Props, State]) {

    val viewer = ScenarioGraphView()

    def render(p: Props, s: State) = {
      val scale = "zoom".reactStyle
      val mozScale = "-moz-transform".reactStyle

      <.svg.svg(^.className := "back",
        scale := " "+ (p.scale.toDouble / 100).toString,
        mozScale := s"scale(${(p.scale.toDouble / 100).toString})"
      )
    }
  }

    def toJSModel(p: Props): js.Dictionary[Object] = { js.Dictionary(
      "phrases" -> p.phrases.toJSDictionary,
      "replies" -> p.replies.toJSDictionary,
      "entry" -> Entry,
      "success" -> EntrySuccess,
      "connections" -> p.connections.toJSDictionary)
  }

  val component = ReactComponentB[Props]("CScriptGraph")
    .initialState(State())
    .renderBackend[Backend]
    .shouldComponentUpdate(scope =>
      scope.$.props.connections != scope.nextProps.connections ||
      scope.$.props.replies != scope.nextProps.replies ||
      scope.$.props.phrases != scope.nextProps.phrases ||
      scope.$.props.scale != scope.nextProps.scale ||
      scope.$.props.select != scope.nextProps.select ||
      scope.$.props.highlightTarget != scope.nextProps.highlightTarget
    )
    .componentDidMount(componentScope =>
      Callback{
        componentScope.backend.viewer.initViewport("svg")
        componentScope.backend.viewer.onNodeSelected = {
          x:js.UndefOr[DisplayableNode] =>
            componentScope.props.onSelected(x.toOption).runNow()}
        componentScope.backend.viewer.draw(toJSModel(componentScope.props),
          componentScope.props.select.orUndefined,
          componentScope.props.highlightTarget)
      })
    .componentDidUpdate(componentScope =>
      Callback{componentScope.$.backend.viewer
        .draw(toJSModel(componentScope.$.props),
          componentScope.$.props.select.orUndefined,
          componentScope.$.props.select.exists(selection =>
            !componentScope.prevProps.select.contains(selection)) &&
            (componentScope.$.props.phrases.size !=
              componentScope.prevProps.phrases.size ||
              componentScope.$.props.replies.size !=
                componentScope.prevProps.replies.size),
          componentScope.$.props.highlightTarget
        )})
    .build

  def apply(p: Props) = component(p)

}
