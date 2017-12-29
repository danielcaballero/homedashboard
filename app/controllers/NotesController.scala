package controllers

import javax.inject._

import play.api.mvc._
import play.api.libs.json._
import model.Note
import com.dcnc.homedashboard._
import play.api.Logger

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class NotesController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit val noteFormat = Json.format[Note]
  private val collection = FlatDatabase.collection[Note]("notes", noteFormat)
  /**
    * Create an Action to list notes
    * GET `/api/notes`.
    */
  def list() = Action { implicit request: Request[AnyContent] =>
    collection.find match {
        case Some(jsObj) => Ok(jsObj)
        case None => NotFound
    }
  }

  /**
    * Create an Action to create a new note
    * POST `/api/notes`
    */
  def create() = Action { implicit request: Request[AnyContent] =>
    request.body.asJson.map { json =>
      collection.create(json) match {
        case Left(jsValue) => Ok(jsValue)
        case Right(symbol) => symbol match {
          case ValidationError => BadRequest
          case _ =>
            Logger.error(symbol.toString())
            InternalServerError
        }
      }
    }.getOrElse(BadRequest)
  }

  /**
    * Create an Action to retrieve a note by id
    * GET `/api/notes/:noteId`
    */
  def retrieve(id: Long) = Action { implicit request: Request[AnyContent] =>
    collection.find(id) match {
      case Some(document) => Ok(document)
      case None => NotFound
    }
  }

  /**
    * Create an Action to update a note
    * PUT `/api/notes/:noteId`
    */
  def update(id: Long) = Action { implicit request: Request[AnyContent] =>
    request.body.asJson.map { json =>
      collection.update(id, json) match {
        case Left(jsValue) => Ok(jsValue)
        case Right(s) => s match {
          case DocumentDoesNotExistError => NotFound
          case ValidationError => BadRequest
          case _ => InternalServerError
        }
      }
    }.getOrElse(BadRequest)
  }

  /**
    * Create an Action to update a note by id
    * PUT `/api/notes/:noteId`
    */
  def delete(id: Long) = Action { implicit request: Request[AnyContent] =>
    collection.delete(id) match {
      case Left(jsValue) => Ok(jsValue)
      case Right(s) => s match {
        case DocumentDoesNotExistError => NotFound
        case _ => InternalServerError
      }
      case _ => InternalServerError
    }
  }
}
