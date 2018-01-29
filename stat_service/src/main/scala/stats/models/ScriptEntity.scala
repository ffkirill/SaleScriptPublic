package stats.models

case class ScriptShortEntity(id: Long,
                            title: String)

case class ScriptEntity(id: Long,
                        title: String,
                        text: String,
                        description: String)
