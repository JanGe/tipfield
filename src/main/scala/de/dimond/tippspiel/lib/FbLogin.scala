package de.dimond.tippspiel
package lib

import scala.io.Source

import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.json._

import java.util.UUID
import java.io.InputStream

import org.joda.time._

import de.dimond.tippspiel.model._

object FbLogin {
  val appId = Props get("fb.AppId") open_!
  val appSecret = Props get("fb.AppSecret") open_!
  val redirectUri = Props get("fb.CallbackUrl") open_!

  case class FbUser(id: String,
                    name: String,
                    username: Option[String],
                    first_name: Option[String],
                    middle_name: Option[String],
                    last_name: Option[String],
                    gender: Option[String],
                    locale: Option[String],
                    timezone: Option[Int])

  object csrfSessionCode extends SessionVar[Box[String]](Empty)

  def authorize(req: Req): Box[LiftResponse] = {
    val code = UUID.randomUUID().toString()
    csrfSessionCode.set(Full(code))
    val dialogUrl = ("https://www.facebook.com/dialog/oauth?client_id=%s&redirect_uri=%s" +
                    "&state=%s").format(appId, redirectUri, code)
    Full(RedirectResponse(dialogUrl, req))
  }

  private def queryParams(params: String): Map[String, List[String]] = {
    val kvPairs = for {
      nameVal <- params.split("&").toList.map(_.trim).filter(_.length > 0)
      (name, value) <- nameVal.split("=").toList match {
        case Nil => Empty
        case n :: v :: _ => Full((Helpers.urlDecode(n), Helpers.urlDecode(v)))
        case n :: _ => Full((Helpers.urlDecode(n), ""))
      }
    } yield (name, value)

    kvPairs.foldLeft(Map[String, List[String]]()) {
      case (map, (name, value)) => map + (name -> (map.getOrElse(name, Nil) ::: List(value)))
    }
  }


  private def call[T](url: String, f: InputStream => T): T = {
    import java.net._
    new URL(url).openConnection match {
      case conn: HttpURLConnection => {
        conn.connect()
        f(conn.getInputStream())
      }
    }
  }

  def callback(req: Req): Box[LiftResponse] = {
    def isToString(is: InputStream) = {
      Source.fromInputStream(is).getLines().mkString("\n")
    }
    val sessionCode = csrfSessionCode.is.openOr("")
    val requestCode = req.param("state").openOr("")
    if (sessionCode.length > 0 && sessionCode.equals(requestCode)) {
      val accessCode = req.param("code").openOr("")
      val tokenUrl = ("https://graph.facebook.com/oauth/access_token?client_id=%s&redirect_uri=%s" +
                      "&client_secret=%s&code=%s").format(appId, redirectUri, appSecret, accessCode)
      val values = call(tokenUrl, x => queryParams(isToString(x)))
      val accessToken = (values.getOrElse("access_token", Nil) ::: List("")).head
      val expires = (values.getOrElse("expires", Nil) ::: List("0")).head

      val meUrl = "https://graph.facebook.com/me?access_token=%s".format(accessToken)

      implicit val formats = DefaultFormats
      val json = call(meUrl, x => JsonParser.parse(isToString(x)))
      val fbUser = json.extract[FbUser]

      val user = User.findByFbId(fbUser.id).openOr(User.create)

      user.facebookId(fbUser.id)
      user.fbAccessToken(accessToken)
      user.fbAccessTokenExpires(new DateTime().plusSeconds(expires.toInt).toDate)

      user.fullName(fbUser.name)

      fbUser.username.map(user.fbUserName(_))
      fbUser.first_name.map(user.firstName(_))
      fbUser.middle_name.map(user.middleName(_))
      fbUser.last_name.map(user.lastName(_))
      fbUser.gender.map(user.gender(_))
      fbUser.locale.map(user.locale(_))
      fbUser.timezone.map(user.fbTimeZone(_))

      if (user.save) {
        User.logUserIn(user)
      }

      S.redirectTo("/")
    } else {
      error("State doesn't match. You are probably a victim of CSRF" + sessionCode + "/" + requestCode)
    }
  }

  def error(text: String) = {
    Full(PlainTextResponse(text, Nil, 400))
  }
}
