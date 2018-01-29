package scenarist.model.questions

import java.util.UUID
import java.util.concurrent.TimeoutException

import scala.collection.mutable
import scala.language.existentials
import diode.Action
import diode.data.Pot
import scenarist.ScriptAsPayload
import scenarist.i18n._

import scala.collection.immutable.ListMap
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.annotation.JSExportAll
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.timers._

object `package` {
  type ObjectId = String
  val Entry = EntryT()
  val EntrySuccess = EntrySuccessT()
  val EntryFail = EntryFailT()
}

object ObjectId {
  def fromString(id: String) = id
  def random = UUID.randomUUID().toString
}


sealed trait Node extends {
  def id: ObjectId
}

sealed trait DisplayableNode extends Node {
  def text: String
}

abstract class BuiltInNode extends DisplayableNode {
  def description: String
}

// Graph nodes
@JSExportAll
case class Phrase(id: ObjectId, text: String, replies: Seq[ObjectId]) extends DisplayableNode {
  def repliesList = replies.toJSArray
}

@JSExportAll
case class Reply(id: ObjectId, text: String) extends DisplayableNode

@JSExportAll
case class EntryT(id: ObjectId = ObjectId.fromString("__entry__"),
  text: String = gettext("Entry"),
  description: String = gettext("This is the entry of the script")
) extends BuiltInNode

@JSExportAll
case class EntrySuccessT(
  id: ObjectId = ObjectId.fromString("__success__"),
  text: String = gettext("Success"),
  description: String = gettext("Success! Next Run.")
) extends BuiltInNode

case class EntryFailT(
  id: ObjectId = ObjectId.fromString("__fail__"),
  text: String = gettext("Fail"),
  description: String = gettext("Unsuccessful termination. Next Run.")
) extends BuiltInNode

// Metadata
case class ScriptMetadata(title: String,
                          description: String) extends Action

case class ScriptListItem(pk: Int, title: String,
                          hasPermChange: Boolean,
                          hasPermOwn: Boolean) extends Action

case class ScriptList(current: Pot[String], list: ListMap[String, ScriptListItem]) extends Action


//Actions

sealed trait AffectsData extends Action {}

case class AddQuestion(x: Int, y: Int, text: String,
                       id: Option[ObjectId] = Option.empty) extends AffectsData
case class UpdateQuestion(id: ObjectId, question: Phrase) extends AffectsData
case class RemoveQuestion(id: ObjectId) extends AffectsData

case class AddAnswer(phraseId: ObjectId, text: String,
                     id: Option[ObjectId] = Option.empty) extends AffectsData
case class UpdateAnswer(id: ObjectId, answer: Reply) extends AffectsData
case class RemoveAnswer(phraseId: ObjectId, replyId: ObjectId) extends AffectsData


case class AddConnection(left: ObjectId, right: ObjectId) extends AffectsData
case class RemoveConnection(left: ObjectId) extends AffectsData


case class ScriptTitleChanged(text: String) extends AffectsData
case class ScriptDescriptionChanged(text: String) extends AffectsData
case class ToggleSaved(saved: Boolean) extends Action

case object SaveScript extends Action
case class ScriptSaved(pk: String) extends Action

case class LoadedScriptList(list: Seq[ScriptListItem]) extends Action
case object LoadScriptList extends Action

case class ScriptSelected(current: Pot[String]) extends Action
case class ScriptLoaded(pk: String, payload: ScriptAsPayload) extends Action

case class ScaleChanged(scale: Int) extends Action

//Throttled Action
case class ThrottledActionParams(handle: SetTimeoutHandle, promise: Promise[Action]) extends Action

trait IThrottledAction extends Action {
  def apply(): Future[Action]
}

class ThrottledAction(action: Action, timeout: Int, key: String) extends IThrottledAction{
  def apply(): Future[Action] = {
    ThrottledAction.timeoutHandles.get(key).foreach(params => {
      clearTimeout(params.handle)
      params.promise.failure(new TimeoutException)
    })
    val promise = Promise[Action]()
    ThrottledAction.timeoutHandles += key -> ThrottledActionParams(
      setTimeout(timeout)({
        promise.success(action)
        ThrottledAction.timeoutHandles -= key
      }),
      promise)

    promise.future
  }
}

object ThrottledAction {
  val timeoutHandles: mutable.Map[String, ThrottledActionParams] = mutable.Map.empty

  def apply(action: Action, timeout: Int, key: String) = {
    new ThrottledAction(action, timeout, key)
  }
}