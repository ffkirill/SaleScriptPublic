package scenarist

import diode.data.{Pot, Ready}

import scala.collection.immutable.{List, ListMap, Map}
import diode._
import diode.react.ReactConnector
import upickle.default._
import scenarist.model.BackendApi
import scenarist.model.questions._


case class SerializedScript(phrases: Map[ObjectId, Phrase],
                            replies: Map[ObjectId, Reply],
                            connections: Map[ObjectId, ObjectId],
                            metadata: ScriptMetadata)

case class SerializedScriptList(scripts: Seq[ScriptListItem])

case class ScriptAsPayload(title: String,
                           description: String,
                           text: String)

case class ScriptSaveResponse(pk: Int)


/**
  * AppCircuit provides the actual instance of the `AppModel` and all the action
  * handlers we need. Everything else comes from the `Circuit`
  */
object AppCircuit extends Circuit[AppModel] with ReactConnector[AppModel] {

  // define initial value for the application model

  def initialModel = {
      AppModel(
      connections = Map(EntrySuccess.id -> Entry.id),
      phrases = Map(),
      replies = Map(),
      metadata = ScriptMetadata(
        title = "", description = ""),
      scripts = ScriptList(
        current = Pot.empty,
        list = ListMap()
      ),
      scale = 100)
  }

  override val actionHandler = composeHandlers(
    new QuestionsHandler(zoomRW(x=>x)((m, v) => v)),
    new MetadataHandler(zoomRW(_.metadata)((m, v) => m.copy(metadata=v)))
  )
}

class QuestionsHandler[M](modelRW: ModelRW[M, AppModel])
  extends ActionHandler(modelRW) {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  override def handle = {
    //Add new question
    case AddQuestion(x, y, text, id) =>
      val newQuestion = Phrase(
        id = id getOrElse ObjectId.random,
        text = text,
        replies = List())
      val connections = if (modelRW.zoom(_.phrases).value.nonEmpty)
        value.connections
      else
        value.connections + (Entry.id -> newQuestion.id)
      updated(value.copy(
        phrases = value.phrases + (newQuestion.id -> newQuestion),
        connections = connections))

    //Change question
    case UpdateQuestion(id, question) =>
      updated(value.copy(phrases = value.phrases + (id -> question)))

    //Change reply
    case UpdateAnswer(id, answer) =>
      updated(value.copy(replies = value.replies + (id -> answer)))

    //Add question answer
    case AddAnswer(phraseId, text, id) =>
      value.phrases get phraseId map {
        phrase =>
          val reply = Reply(id getOrElse ObjectId.random, text)
          val updatedPhrase = phrase.copy(
            replies = phrase.replies :+ reply.id)
          updated(value.copy(
            phrases = value.phrases + (phraseId -> updatedPhrase),
            replies = value.replies + (reply.id -> reply)))
      } getOrElse noChange

    //Remove question answer
    case RemoveAnswer(phraseId, replyId) =>
      value.phrases get phraseId map {
        phrase =>
          val updatedPhrase = phrase.copy(
            replies = phrase.replies filterNot {_ == replyId})
          updated(value.copy(
            phrases = value.phrases + (phraseId -> updatedPhrase),
            replies = value.replies - replyId,
            connections = value.connections - replyId
          ))
      } getOrElse noChange

    //Add connection
    case AddConnection(left, right) =>
      if (((value.replies contains left) || left == Entry.id)
        && ((value.phrases contains right) || right == EntrySuccess.id
        || right == EntryFail.id))
        updated(value.copy(connections = value.connections
          + (left -> right)))
      else
        noChange

    //Remove connection (at conn source side)
    case RemoveConnection(left) =>
      updated(value.copy(connections = value.connections - left))

    //Remove question
    case RemoveQuestion(id) =>
      val replies = value.phrases get id map {p =>
        p.replies.toSet} getOrElse Set()
      updated(value.copy(
        phrases = value.phrases - id,
        replies = value.replies -- replies,
        connections = value.connections filterNot {p =>
          p._2 == id || replies.contains(p._1)}
      ))

    //Save script to server
    case SaveScript =>
      if (value.scripts.current.isPending)
        noChange
      else {
        val dataToSave = SerializedScript(
          phrases = value.phrases,
          replies = value.replies,
          connections = value.connections,
          metadata = value.metadata
        )
        val scriptText = write(dataToSave: SerializedScript)
        val payload = write(ScriptAsPayload(value.metadata.title,
          value.metadata.description,
          scriptText))
        value.scripts.current match {
          case Ready(pk: String) =>
            effectOnly(
              Effect(BackendApi.updateScript(pk, payload) map { r => LoadScriptList})
                  >> Effect.action(ToggleSaved(true)))
          case _ =>
            effectOnly(
              Effect(BackendApi.createScript(payload) map { r =>
                  ScriptSaved(
                    read[ScriptSaveResponse](r.responseText).pk.toString)}))
        }
      }

    //Script saved
    case ScriptSaved(pk: String) =>
      updated(value.copy(
        saved = true,
        scripts = value.scripts.copy(current = Ready(pk))),
        Effect.action(LoadScriptList)
      )

    //Loaded server's scripts list
    case LoadedScriptList(scripts) =>
      updated(value.copy(
        scripts = value.scripts.copy(
          list = ListMap(scripts.map{r=>(r.pk.toString, r)}:_*))))

    //Load scripts list
    case LoadScriptList =>
      effectOnly(
        Effect(BackendApi.getScriptsList map { r =>
          LoadedScriptList(
            read[Seq[ScriptListItem]](r.responseText))})
      )

    //Current script is selected by user
    case ScriptSelected(current) =>
      current match {
        case Ready(pk: String) =>
          effectOnly(
            Effect(BackendApi.getScriptDetail(pk) map { r =>
              ScriptLoaded(pk, read[ScriptAsPayload](r.responseText))}))
        case _ =>
          updated(value.copy(scripts = value.scripts.copy(current = current)))
      }

    //Currently selected script is loaded
    case ScriptLoaded(pk, payload) =>
      val data = read[SerializedScript](payload.text)
      updated(value.copy(
        scripts = value.scripts.copy(current = Ready(pk)),
        phrases = data.phrases,
        replies = data.replies,
        connections = data.connections,
        saved = true,
        metadata = data.metadata))

    case ScaleChanged(scale) =>
      updated(value.copy(scale=scale))

    case ToggleSaved(saved) =>
      updated(value.copy(saved = saved))

    case (f: IThrottledAction) =>
      effectOnly(Effect(f()))
  }
}

class MetadataHandler[M](modelRW: ModelRW[M, ScriptMetadata])
  extends ActionHandler(modelRW) {
  override def handle = {
    case ScriptTitleChanged(text) =>
      updated(value.copy(title = text))

    case ScriptDescriptionChanged(text) =>
      updated(value.copy(description = text))

  }
}

class DataAffectionsProcessor[M <: AnyRef] extends ActionProcessor[M] {
  def process(dispatch: Dispatcher, action: Any,
              next: (Any) => ActionResult[M], currentModel: M): ActionResult[M] = {
    action match {
      case action: AffectsData =>
        dispatch(ToggleSaved(false))
        next(action)
      case _ =>
        next(action)
    }
  }
}

