package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordFieldDefinition, MappingDefinition}

import scala.collection.mutable

/** group and id should composite a unique key */
case class Project(
                    val id: String,
                    val summary: String,
                    val description: String,
                    val group: String,
                    var creator: String = null,
                    var createdAt: String = null,
                  ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    m.toMap
  }
}

object Project extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}project"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_ID),
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
    )
  )
}
