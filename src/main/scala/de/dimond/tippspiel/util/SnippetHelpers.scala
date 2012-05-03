package de.dimond.tippspiel.util

import de.dimond.tippspiel._
import model._
import scala.xml._
import net.liftweb.http.S

object SnippetHelpers {
  def teamHtml(ref: TeamReference) = ref.team match {
    case Left((str, id)) => Text(S.?(str).format(id))
    case Right(team) => {
      Seq(<img src={"/images/flags/" + team.emblemUrl} />, Text(team.toString()))
    }
  }
}