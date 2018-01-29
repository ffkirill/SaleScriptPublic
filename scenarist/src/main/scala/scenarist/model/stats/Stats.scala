package scenarist.model.stats

import org.threeten.bp.LocalDateTime

case class ScriptSummaryEntry(scriptId: Int,
                              userId: Option[Int],
                              date: Option[LocalDateTime],
                              runCount: Int,
                              successCount: Int,
                              failCount: Int,
                              noSuchReplyCount: Int,
                              allEventsCount: Int,
                              scriptTitle: Option[String],
                              userName: Option[String],
                              userFirstName: Option[String],
                              userLastName: Option[String])
