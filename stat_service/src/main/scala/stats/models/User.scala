package stats.models

case class User(
  id: Long,
  username: String,
  email: String,
  isSuperuser: Boolean,
  ownScripts: Map[Long, ScriptShortEntity]
)

case class UserEntity(
   id: Long,
   username: String,
   email: String,
   firstName: String,
   lastName: String
)
