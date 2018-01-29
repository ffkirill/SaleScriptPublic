package utils

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

import upickle.Js

object OptionPickler extends upickle.AttributeTagged {
  override implicit def OptionW[T: Writer]: Writer[Option[T]] = Writer {
    case None    => Js.Null
    case Some(s) => implicitly[Writer[T]].write(s)
  }

  override implicit def OptionR[T: Reader]: Reader[Option[T]] = Reader {
    case Js.Null     => None
    case v: Js.Value => Some(implicitly[Reader[T]].read.apply(v))
  }
}


trait Protocols {
  implicit val datetime2Writer = OptionPickler.Writer[LocalDateTime]{
    case t => Js.Str(t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }
  implicit val datetime2Reader = OptionPickler.Reader[LocalDateTime]{
    case Js.Str(str) =>
      LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }
}
