package controllers

import javax.inject._

import model.User
import play.api.libs.json._
import play.api.mvc._
import com.dcnc.homedashboard._
import play.api.Logger
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class UsersController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit val userFormat: Format[User] = Json.format[User]
  private val collection = FlatDatabase.collection[User]("users", userFormat)
  /**
    * Create an Action to list users
    * GET `/api/users`.
    */
  def list() = Action { implicit request: Request[AnyContent] =>
    collection.find() match {
        case Some(jsObj) => Ok(jsObj)
        case None => NotFound
    }
  }

  /**
    * Create an Action to create a new user.
    * POST `/api/users`.
    */
  def create() = Action { implicit request: Request[AnyContent] =>
    request.body.asJson.map { json =>
      collection.create(json) match {
        case Left(jsValue) => Ok(jsValue)
        case Right(symbol) => Logger.error(symbol.toString())
          InternalServerError
      }
    }.getOrElse(BadRequest)
  }

  /**
    * Create an Action to retrieve a user by id
    * GET `/api/users/:userId`
    */
  def retrieve(id: Long) = Action { implicit request: Request[AnyContent] =>
    collection.find(id) match {
      case Some(jsValue) => Ok(jsValue)
      case None => NotFound
    }
  }

  /**
    * Create an Action to update a user
    * PUT `/api/users/:userId`
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
