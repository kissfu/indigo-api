package asura.core.es.model

import io.swagger.models.ModelImpl
import io.swagger.models.properties.{ObjectProperty, Property}

import scala.collection.JavaConverters.mapAsScalaMap
import scala.collection.mutable

/** only support type now */
case class JsonSchema(
                       var description: String,
                       var `type`: String,
                       var properties: Map[String, JsonSchema] = null
                     ) extends SchemaObject {

}

object JsonSchema {

  def toJsonSchema(model: ModelImpl): JsonSchema = {
    val properties = toJsonSchema(mapAsScalaMap(model.getProperties))
    JsonSchema(
      description = model.getDescription,
      `type` = SchemaObject.translateOpenApiType(model.getType, model.getFormat),
      properties = properties
    )
  }

  def toJsonSchema(properties: mutable.Map[String, Property]): Map[String, JsonSchema] = {
    val map = mutable.Map[String, JsonSchema]()
    if (null != properties) {
      for ((name, property) <- properties) {
        val schema = property match {
          case p: ObjectProperty =>
            JsonSchema(
              description = property.getDescription,
              `type` = SchemaObject.translateOpenApiType(property.getType, property.getFormat),
              properties = toJsonSchema(mapAsScalaMap(p.getProperties))
            )
          case _ =>
            JsonSchema(
              description = property.getDescription,
              `type` = SchemaObject.translateOpenApiType(property.getType, property.getFormat)
            )
        }
        map += (name -> schema)
      }
    }
    map.toMap
  }
}
