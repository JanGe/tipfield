package de.dimond.tippspiel.util
import java.util.Locale

object Util {
  def parseInt(s: String) = try { Some(s.toInt) } catch { case _ => None }

  def localeFromString(in: String): Locale = {
        val x = in.split("_").toList; new Locale(x.head,x.last)
      }
}

object Int {
  def unapply(s : String) : Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}
object Long {
  def unapply(s : String) : Option[Long] = try {
    Some(s.toLong)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}