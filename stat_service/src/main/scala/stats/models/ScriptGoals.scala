package stats.models

object ScriptGoals extends Enumeration {
  val success = Value(0)
  val failure = Value(1)
  val noSuchReply = Value(2)
  val scriptRan = Value(3)
}
