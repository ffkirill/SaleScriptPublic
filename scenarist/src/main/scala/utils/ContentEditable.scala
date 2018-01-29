package utils

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object ContentEditable {
  type ChangedCb = String => Callback

  def changed(cb: ChangedCb)(e:ReactEventH): Callback = {
      cb(e.target.innerHTML)
  }

  def apply(onChanged: ChangedCb,
            disabled: Boolean = false)(tag: ReactTagOf[_ <: TopNode]) = {
    tag(^.contentEditable := !disabled,
      ^.onChange ==> changed(onChanged),
      ^.onBlur ==> changed(onChanged))
  }
}