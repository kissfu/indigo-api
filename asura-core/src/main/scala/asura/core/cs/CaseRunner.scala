package asura.core.cs

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import asura.common.util.StringUtils
import asura.core.CoreConfig._
import asura.core.cs.asserts.HttpResponseAssert
import asura.core.es.model.{Case, KeyValueObject}
import asura.core.es.service.{ApiService, EnvironmentService}
import asura.core.http.{HttpContentTypes, HttpEngine}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object CaseRunner {

  val logger = Logger("CaseRunner")

  def test(id: String, cs: Case, context: CaseContext = CaseContext()): Future[CaseResult] = {
    context.eraseCurrentData()
    if ((cs.useEnv && StringUtils.isNotEmpty(cs.env)) || (Option(cs.useProxy).isDefined && cs.useProxy)) {
      val apiEnv = for {
        api <- ApiService.getApiById(cs.api)
        env <- EnvironmentService.getEnvById(cs.env)
      } yield (api, env)
      apiEnv
        .flatMap(apiEnv => CaseParser.toHttpRequest(cs, context, apiEnv._1, apiEnv._2))
        .flatMap(toCaseRequestTuple)
        .flatMap(tuple => {
          if (Option(cs.useProxy).isDefined && cs.useProxy) {
            HttpEngine.singleRequestWithProxy(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[String].map(resBody => {
                HttpResponseAssert.generateCaseReport(id, cs.assert, res, resBody, tuple._2, context)
              })
            })
          } else {
            HttpEngine.singleRequest(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[String].map(resBody => {
                HttpResponseAssert.generateCaseReport(id, cs.assert, res, resBody, tuple._2, context)
              })
            })
          }
        })
    } else {
      ApiService.getApiById(cs.api).flatMap(api => {
        CaseParser.toHttpRequest(cs, context, api)
          .flatMap(toCaseRequestTuple)
          .flatMap(tuple => {
            HttpEngine.singleRequest(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[String].map(resBody => {
                HttpResponseAssert.generateCaseReport(id, cs.assert, res, resBody, tuple._2, context)
              })
            })
          })
      })
    }
  }

  def toCaseRequestTuple(req: HttpRequest): Future[(HttpRequest, CaseRequest)] = {
    Unmarshal(req.entity).to[String].map(reqBody => {
      val mediaType = req.entity.contentType.mediaType.value
      val headers = req.headers
        .map(h => KeyValueObject(h.name(), h.value()))
        .+:(KeyValueObject(HttpContentTypes.KEY_CONTENT_TYPE, mediaType))
      (req, CaseRequest(req.method.value, req.getUri().toString, headers, reqBody))
    })
  }
}
