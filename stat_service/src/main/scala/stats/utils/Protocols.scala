package stats.utils

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import PostgresProfileWithExtensions.api.DateTruncKind

import akka.http.scaladsl.unmarshalling.Unmarshaller
import io.circe.Encoder
import io.circe.java8.time._

trait Protocols {
  implicit val encodeUUID: Encoder[UUID] = Encoder.encodeString.contramap[UUID](_.toString)

  implicit val encodeTimestamp: Encoder[Timestamp] =
    t => encodeLocalDateTimeDefault(t.toLocalDateTime)

  implicit val localDateTimeUnmarshaller: Unmarshaller[String, LocalDateTime] =
    Unmarshaller.strict[String, LocalDateTime] { string =>
      LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

  implicit val dateTruncKindUnmarshaller: Unmarshaller[String, DateTruncKind] =
    Unmarshaller.strict[String, DateTruncKind] { string =>
      DateTruncKind.withName(string)
    }

}
