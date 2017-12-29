package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class NotesControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "NotesController GET" should {

    "render the index page from a new instance of controller" in {
      val controller = new NotesController(stubControllerComponents())
      val notes = controller.list().apply(FakeRequest(GET, "/"))

      status(notes) mustBe OK
      contentType(notes) mustBe Some("application/json")
      contentAsString(notes) must include("""{"nextId":1,"notes":[]}""")
    }
  }
}
