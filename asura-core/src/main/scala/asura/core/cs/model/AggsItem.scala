package asura.core.cs.model

case class AggsItem(
                     val `type`: String,
                     val id: String,
                     val count: Int,
                     var sub: Seq[AggsItem] = null,
                     var summary: String = null,
                     var description: String = null,
                   ) {

}
