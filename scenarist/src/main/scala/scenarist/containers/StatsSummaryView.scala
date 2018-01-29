package scenarist.containers

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.threeten.bp.{LocalDateTime, LocalTime, LocalDate}
import org.threeten.bp.format.DateTimeFormatter

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scenarist.i18n.gettext
import scenarist.model.BackendApi
import scenarist.model.stats.ScriptSummaryEntry
import utils.Protocols
import utils.OptionPickler._

object StatsSummaryView extends Protocols {
  case class State(rows: Seq[ScriptSummaryEntry],
                   rangeStart: Option[LocalDateTime] = None,
                   rangeEnd: Option[LocalDateTime] = None,
                   groupByUser: Boolean = false,
                   groupByDate: Option[String] = None)
  case class Props()

  class Backend($: BackendScope[Props, State]) {

    lazy val notAvailableText: String = gettext("N/A")

    val selectNoneVal = "none"

    def loadSummary(s: State) = Callback.future {
      BackendApi.getOwnScriptsStats(
        s.rangeStart,
        s.rangeEnd,
        s.groupByUser,
        s.groupByDate
      ) map { r =>
        $.modState { state =>
          state.copy(rows = read[Seq[ScriptSummaryEntry]](r.responseText))
        }
    }}

    def parseDate(str: String, endOf: Boolean = false): Option[LocalDateTime] = {
      if (str.isEmpty) None else {
        val date = Some(LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE))
        val time = if (endOf)
          LocalTime.MAX
        else
          LocalTime.MIN
        date.map(LocalDateTime.of(_, time))
      }
    }

    def formatDate(date: Option[LocalDateTime]): String =
      date map { _.format(DateTimeFormatter.ISO_LOCAL_DATE)} getOrElse ""

    def setStateThenReload(n: State): Callback = {
      $.setState(n) >> loadSummary(n)
    }

    def onRangeStartChange(s: State)(rawValue: String): Callback = {
      val newState = s.copy(rangeStart = parseDate(rawValue))
      setStateThenReload(newState)
    }

    def onRangeEndChange(s: State)(rawValue: String): Callback = {
      val newState = s.copy(rangeEnd = parseDate(rawValue, endOf = true))
      setStateThenReload(newState)
    }

    def onGroupByUserChange(s: State)(checked: Boolean): Callback = {
      setStateThenReload(s.copy(groupByUser = checked))
    }

    def onGroupByDateChange(s: State)(value: String): Callback = {
      setStateThenReload(s.copy(groupByDate = value match {
        case `selectNoneVal` =>
          None
        case _ =>
          Some(value)
      }))
    }


    def sidebar(p: Props, s: State) = {
      <.div(^.className := "panel panel-default form-horizontal"
        , <.fieldset(^. className := "panel-body"
          , <.legend(gettext("Filters"))

          , <.div(^.className := "form-group"
            , <.label(^.`for` := "rangeStart", ^.className := "col-md-2 control-label"
              , gettext("Range start"))
            , <.div(^.className := "col-md-10"
              , <.input(^.`type` := "date"
                , ^.className := "form-control"
                , ^.id := "rangeStart"
                , ^.value := formatDate(s.rangeStart)
                , ^.onChange ==> ((e: ReactEventI) => onRangeStartChange(s)(e.target.value)))
            ))

          , <.div(^.className := "form-group"
            , <.label(^.`for` := "rangeEnd", ^.className := "col-md-2 control-label"
              , gettext("Range end"))
            , <.div(^.className := "col-md-10"
              , <.input(^.`type` := "date"
                , ^.className := "form-control"
                , ^.id := "rangeEnd"
                , ^.value := formatDate(s.rangeEnd)
                , ^.onChange ==> ((e: ReactEventI) => onRangeEndChange(s)(e.target.value)))
            ))
        )
        , <.fieldset(^. className := "panel-body"
          , <.legend(gettext("Group"))

          , <.div(^.className := "checkbox"
            , <.label(
                <.input(^.`type` := "checkbox"
                  , ^.id := "groupByUser"
                  , ^.checked := s.groupByUser
                  , ^.onChange ==> ((e: ReactEventI) =>
                    onGroupByUserChange(s)(!s.groupByUser)))
                , gettext("By User")
            ))
          , <.div(^.className := "form-group"
            , <.label(^.`for` := "groupByDate", ^.className := "col-md-2 control-label"
              , gettext("By Date"))
            , <.div(^.className := "col-md-10"
              , <.select(^.id := "groupByDate"
                , ^.value := s.groupByDate.getOrElse(selectNoneVal)
                , ^.className := "form-control"
                , ^.onChange ==> ((e: ReactEventI) =>
                  onGroupByDateChange(s)(e.target.value))
                , <.option(^.value := "hour", gettext("hour"))
                , <.option(^.value := "day", gettext("day"))
                , <.option(^.value := "week", gettext("week"))
                , <.option(^.value := "month", gettext("month"))
                , <.option(^.value := "quarter", gettext("quarter"))
                , <.option(^.value := "year", gettext("year"))
                , <.option(^.value := "none", "----")
              )
            ))

        )
      )
    }

    def render(p: Props, s: State) = {
      <.div(^.className := "viewer-wrapper",
        <.div(^.className := "viewer",
          <.table(^.className :=
            "panel panel-default table table-bordered table-hover",
            <.thead(^.className := "panel-heading",
              <.tr(
                <.th("#")
                , <.th(gettext("Script"))
                , <.th(gettext("Date"))
                , <.th(gettext("User"))
                , <.th(gettext("Run count"))
                , <.th(gettext("Success count"))
                , <.th(gettext("Failure count"))
                , <.th(gettext("No such reply count"))
                , <.th(gettext("Events total"))))
            , <.tbody(^.className := "panel-body",
              s.rows zip (1 to s.rows.length + 1) map { case (row, num) =>
                <.tr(
                  <.td(s"$num.")
                  , <.td(row.scriptTitle.getOrElse[String](notAvailableText))
                  , <.td(row.date.map(_.toString).getOrElse[String](notAvailableText))
                  , <.td((for {
                    username <- row.userName
                    firstName <- row.userFirstName
                    lastName <- row.userLastName
                  } yield {
                    s"$username ($firstName $lastName)"
                  }) getOrElse[String] notAvailableText)
                  , <.td(row.runCount)
                  , <.td(row.successCount)
                  , <.td(row.failCount)
                  , <.td(row.noSuchReplyCount)
                  , <.td(row.allEventsCount)
                )
              }
            )
          )
        ),
        <.div(^.className := "viewer-sidebar", ^.paddingLeft := 10
          , sidebar(p, s)
        )
      )
    }

  }

  def component(s: State) = ReactComponentB[Props](
    "CStatsSummaryView")
    .initialState(s)
    .renderBackend[Backend]
      .componentDidMount(scope => scope.backend.loadSummary(scope.state))
    .build

  def apply() =
    component(State(Seq()))(Props())
}
