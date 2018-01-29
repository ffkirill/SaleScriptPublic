package scenarist.containers

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.collection.immutable.ListMap
import diode.data.{Pot, Ready}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.ext.AjaxException
import upickle.default._
import scenarist.components.ScriptsCombobox
import scenarist.model.questions.{ScriptList, ScriptListItem}
import scenarist.i18n.gettext

import scenarist.model.BackendApi

case class PermissionUser(username: String, email: String)


case class ValidationEmailError(email: Seq[String] = Seq.empty)

case class ValidationError(permission: Seq[String] = Seq.empty,
                           user: ValidationEmailError = ValidationEmailError(),
                           message: String = "")

object Permission {
  val PERM_OWN = "script_own"
  val PERM_VIEW = "script_view"
  val PERM_CHANGE = "script_change"
  val PERM_EXECUTE = "script_execute"
  val perms = ListMap(
    PERM_OWN -> gettext("Owner"),
    PERM_CHANGE -> gettext("Change and view"),
    PERM_EXECUTE -> gettext("Run collecting stats"),
    PERM_VIEW -> gettext("View only")
  )
}

case class UnsavedScriptPermission(user: PermissionUser,
                                   permission: String)

case class ScriptPermission(pk: Option[Int],
                            user: PermissionUser,
                            permission: String)

case class SavedScriptPermission(pk: Int,
                                 user: PermissionUser,
                                 permission: String)

object PreferencesItemView {
  case class Props(permission: ScriptPermission,
                   scriptPk: String,
                   onSaved: Callback)

  case class State(permission: ScriptPermission,
                   message: Option[String])

  class Backend($: BackendScope[Props, State]) {

    def permissionCombobox(s: ScriptPermission) = {
      <.select(^.value := s.permission,
        ^.className := "form-control",
        ^.onChange ==> ((e: ReactEventI) => {
          val newValue = e.target.value
          $.modState(_.copy(permission = s.copy(permission = newValue)))
        }),
          Permission.perms map { perm =>
          <.option(^.key := perm._1, ^.value := perm._1, perm._2)
        }
      )
    }

    def save(p: Props)(s: ScriptPermission): Callback = {
      lazy val resp = s.pk match {
        case Some(pk: Int) =>
          BackendApi.updateScriptPermission(p.scriptPk, pk.toString,
            data = write(SavedScriptPermission(pk, s.user, s.permission))
          )
        case _ =>
          BackendApi.createScriptPermission(p.scriptPk,
            data = write(UnsavedScriptPermission(s.user, s.permission))
          )
      }
      Callback.future {
        resp map { _ =>
          p.onSaved >> $.modState(_.copy(message = Option.empty))
        } recover {
          case AjaxException(xhr) if xhr.status == 400 =>
            val errors = read[ValidationError](xhr.responseText)
            val msg = errors.message + (
              if (errors.user.email.nonEmpty)
                gettext("Email")
                  + ": "
                  + errors.user.email.reduceLeft[String](_.toString + " " + _)
              else
                "") + (
              if (errors.permission.nonEmpty)
                gettext("Permission")
                  + ": "
                  + errors.permission.reduceLeft[String](_.toString + " " + _)
              else
                "").trim
            $.modState(_.copy(
              message = if (msg.nonEmpty) Some(msg) else Option.empty))
          case AjaxException(xhr) =>
            $.modState(_.copy(
              message = Some(s"${gettext("Ajax error")}: ${xhr.status}")))
          case _ =>
            $.modState(_.copy(
              message = Some(gettext("Unknown error"))))
        }
      }
    }

    def delete(p: Props): Callback = p.permission.pk match {
      case Some(pk: Int) =>
        Callback.future {
          BackendApi.deleteScriptPermission(p.scriptPk, pk.toString) map {_ => p.onSaved}
        }
      case _ =>
          p.onSaved
    }

    def render(p: Props, s: State) = {
      val changed = p.permission != s.permission
      <.tr(^.classSet("danger" -> s.message.isDefined),
        <.td(
          <.p(^.className := "form-control", s.permission.user.username)),
        <.td(^.className := "col-email",
          <.input(^.className := "form-control", ^.`type` := "email",
            ^.value := s.permission.user.email,
            ^.onChange ==> ((e: ReactEventI) => {
              val newValue = e.target.value
              $.modState { st =>
                st.copy(permission = st.permission.copy(
                        user = st.permission.user.copy(email = newValue))) }
            })
          ),
          s.message.map(<.p(_)).getOrElse(EmptyTag)
        ),
        <.td(^.className := "col-permission",
          permissionCombobox(s.permission)),
        <.td(^.className := "col-tools",
          <.div(^.className := "form-group",
            <.button(^.className := "btn btn-sm btn-success",
              ^.disabled := !changed,
              ^.onClick --> save(p)(s.permission),
              gettext("Save")),
            <.button(^.className := "btn btn-sm btn-danger", gettext("Remove"),
              ^.onClick --> delete(p)),
            <.button(^.className := "btn btn-sm btn-success", gettext("Revert"),
              ^.onClick --> $.modState(_.copy(permission = p.permission,
                message = Option.empty)),
              ^.disabled := !changed)
          )
        )
      )
    }
  }

  def component(s: State) = ReactComponentB[Props](
    "CPreferencesItem")
    .initialState(s)
    .renderBackend[Backend]
    .build

  def apply(p: ScriptPermission, saved: Callback, scriptPk: String) =
    component(State(p.copy(), Option.empty))(Props(p, scriptPk, saved))
}


object PreferencesView {

  case class Props()

  case class State(scripts: ScriptList,
                   permissions: Seq[ScriptPermission])

  class Backend($: BackendScope[Props, State]) {

    def currentScriptChanged(s: State)(current: Pot[String]): Callback = {
      val st = s.copy(scripts = s.scripts.copy(current = current))
      $.setState(st) >> loadPermissions(st)
    }

    def loadScriptsList = Callback.future {
      BackendApi.getScriptsList map { r =>
        $.modState { state =>
          state.copy(scripts = state.scripts.copy(list =
            ListMap(read[Seq[ScriptListItem]](r.responseText)
              filter {_.hasPermOwn} map { r =>
                (r.pk.toString, r) }: _*)
          ))
        }
      }
    }

    def addPermission: Callback = $.modState {s =>
      s.copy(permissions = s.permissions :+ ScriptPermission(Option.empty,
        PermissionUser("", ""), Permission.PERM_VIEW))
    }

    def loadPermissions(s: State): Callback = {
      s.scripts.current match {
        case Ready(pk: String) =>
          Callback.future {
            BackendApi.getScriptPermissions(pk) map { resp =>
              $.modState(_.copy(
                permissions = read[Seq[SavedScriptPermission]](
                  resp.responseText) map { perm =>
                  ScriptPermission(Some(perm.pk), perm.user, perm.permission)
                })
              )
            }
          }
        case _ =>
          $.modState(_.copy(permissions = Seq.empty))
      }
    }

    def render(p: Props, s: State) = {
      <.div(
        <.div(^.className := "form-group",
          <.label(^.className := "control-label", s"${gettext("Script")}: "),
          ScriptsCombobox(ScriptsCombobox.Props(
            s.scripts,
            currentScriptChanged(s),
            loadScriptsList
        ))),
        <.table(^.className :=
          "table table-bordered table-hover table-permissions",
          <.thead(
            <.tr(
              <.th(gettext("Username")),
              <.th(gettext("Email")),
              <.th(gettext("Permission")),
              <.th())),
          <.tbody(
            s.scripts.current.toOption.map(currentScriptPk =>
              s.permissions map { perm =>
                PreferencesItemView(perm, loadPermissions(s), currentScriptPk)})
          ),
          <.tfoot(
            <.tr(
              <.td(
                <.button(^.className := "btn btn-sm btn-success",
                  ^.onClick --> addPermission,
                  gettext("Add new permission")))
            )
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("CPreferencesView")
    .initialState(
      State(ScriptList(Pot.empty, ListMap.empty),
        Seq.empty))
    .renderBackend[Backend]
    .build

  def apply() =
    component(Props())
}
