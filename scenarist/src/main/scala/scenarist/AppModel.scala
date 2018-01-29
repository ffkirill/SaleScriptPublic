package scenarist

import model.questions._



case class AppModel(
  phrases: Map[ObjectId, Phrase],
  replies: Map[ObjectId, Reply],
  connections: Map[ObjectId, ObjectId],
  metadata: ScriptMetadata,
  scale: Int,
  saved: Boolean = true,
  scripts: ScriptList)
