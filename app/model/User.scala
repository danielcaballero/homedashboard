package model
case class User(firstName: String, lastName: String, email: String, id: Option[Long] = None)