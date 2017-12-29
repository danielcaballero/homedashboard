package com.dcnc.homedashboard

import java.io.{BufferedWriter, File, FileInputStream, FileWriter}

import play.api.Logger
import play.api.libs.json._


class FlatDatabase[A](val collection: String, implicit val format: Format[A]) {
  private val collectionPath: Symbol = Symbol(collection)
  def find(id: Long): Option[JsValue] = {
    find() match {
      case Some(json) => (json \ s"$collection").get match {
        case jsArray: JsArray => jsArray.value.find(jsValue => (jsValue \ "id").as[Long] == id)
        case _ => None
      }
      case None => None
    }
  }

  def find(): Option[JsValue] = {
    val stream = getClass().getResourceAsStream(s"/data/$collection.json")
    try {
      Some(Json.parse(stream))
    } catch {
      case e: Exception => None
    } finally {
      stream.close()
    }
  }

  def save(json: JsObject): Option[JsValue] = {
    val bw = new BufferedWriter(new FileWriter(getClass().getResource(s"/data/$collection.json").getFile))
    try {
      bw.write(Json.prettyPrint(Json.toJson(json)))
      Some(json)
    } catch {
      case e: Exception => None
    } finally {
      bw.close()
    }
  }

  def create(payload: JsValue): Either[JsValue, Symbol] = {
    val appendDocumentTransformer = (nextId: Long, document: JsValue) => (__).json.pickBranch(
      (__ \ collectionPath).json.update(__.read[JsArray].map(_ :+ document.transform(
        (__).json.update(__.read[JsObject].map { a => a + ("id" ->  JsNumber(nextId)) })).get))
        andThen
        (__ \ 'nextId).json.update(__.read[JsNumber].map(a => JsNumber(a.as[Long] + 1))))

    payload.validate[A] match {
      case s: JsSuccess[A] =>
        find() match {
          case Some(documents) => {
            val nextId = (documents \ "nextId").as[Long]
            documents.transform(appendDocumentTransformer(nextId, Json.toJson(s.value))) match {
              case s: JsSuccess[JsObject] => save(s.value) match {
                case Some(document) => Left(document)
                case None => Right(WriteIOError)
              }
              case e: JsError =>
                Logger.error(JsError.toJson(e).toString())
                Right(JSONTransformError)
            }
          }
          case None => Right(DataCorruptionError)
        }
      case e:
        JsError =>
        Logger.error(JsError.toJson(e).toString())
        Right(ValidationError)
    }
  }

  def delete(id: Long): Either[JsValue, Symbol] = {
    val deleteTransformer =
      (__ \ collectionPath).json.update(
        __.read[JsArray].map(
          jsArray => Json.toJson(
            jsArray.value.filter(
              jsObj => {
                jsObj.as[JsObject].value("id").as[Long] != id
              }
            )
          )
        )
      )

    find(id) match {
      case Some(_) =>
        find() match {
          case Some(json) =>
            json.transform(deleteTransformer) match {
              case s: JsSuccess[JsObject] =>
                save(s.value)
                Left(s.value)
              case _: JsError => Right(JSONTransformError)
            }
          case None => Right(DataCorruptionError)
        }
      case None =>
        Right(DocumentDoesNotExistError)
    }
  }

  def update(id: Long, json: JsValue): Either[JsValue, Symbol] = {
    def deleteAndInsert(l: Long, json: JsValue): Either[JsValue, Symbol] = {

      val updateTransformer = (id: Long) => (__).json.pickBranch(
        (__ \ collectionPath).json.update(__.read[JsArray].map(jsArray => Json.toJson(jsArray.value.filter(jsObj => {
          jsObj.as[JsObject].value("id").as[Long] != id
        }))))
          andThen
          (__ \ collectionPath).json.update(__.read[JsArray].map(_ :+ json.transform(
            (__).json.update(__.read[JsObject].map { a => a + ("id" -> JsNumber(id)) })).get)))

      find() match {
        case Some(collection) => {
          collection.transform(updateTransformer(id)) match {
            case s: JsSuccess[JsObject] =>
                save(s.value) match {
                  case Some(updatedCollection) => Left(updatedCollection)
                  case None => Right(WriteIOError)
                }
            case e: JsError =>
              Logger.error(JsError.toJson(e).toString())
              Right(JSONTransformError)
          }
        }
        case None => Right(DataCorruptionError)
      }
    }

    json.validate[A] match {
      case s: JsSuccess[A] =>
        find(id) match {
          case Some(jsValue) => {
            deleteAndInsert(id, json)
          }
          case _ => {
            Right(DocumentDoesNotExistError)
          }
        }
      case e:
        JsError =>
        Logger.error(JsError.toJson(e).toString())
        Right(ValidationError)
    }
  }
}

object FlatDatabase {
  def collection[A](collection: String, format: Format[A]) = new FlatDatabase(collection, format)
}
