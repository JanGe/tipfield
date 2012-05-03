package de.dimond.tippspiel.model

import de.dimond.tippspiel.model.PersistanceConfiguration._

object Group {
  private var _groups: Map[String, Group] = Map()

  def init(groups: Seq[Group]) = _groups = _groups ++ (for (group <- groups) yield (group.name, group))

  def forName(groupName: String) = _groups.getOrElse(groupName,
                                                     throw new IllegalArgumentException("Group not available"))

  def all = _groups.values
}

case class Group(name: String, teams: Seq[Team], games: Seq[Game]) {
  for (game <- games) {
    if (!teams.contains(game.teamHome.team_!)) {
      throw new IllegalArgumentException("Group does not contain Team " + game.teamHome.team)
    }
    if (!teams.contains(game.teamAway.team_!)) {
      throw new IllegalArgumentException("Group does not contain Team " + game.teamAway.team)
    }
  }

  val initialStandings = for (team <- teams) yield Standing(team, 0, 0, 0, 0, 0, 0, 0)

  private def isFinished = games.filter(Result.forGame(_).isEmpty).size == 0

  def winner: Option[Team] = {
    if (isFinished) Some(standings(0).team)
    else None
  }

  def runnerUp: Option[Team] = {
    if (isFinished) Some(standings(1).team)
    else None
  }

  def standings: Seq[Standing] = {
    val results = games.map(game => ((game, Result.forGame(game))))
    val standingsAgg = for { resultBox <- results; result <- resultBox._2; game = resultBox._1 } yield {
      val gh = result.goalsHome
      val ga = result.goalsAway
      val teamHome = game.teamHome.team
      val teamAway = game.teamAway.team
      (teamHome, teamAway) match {
        case (Right(th), Right(ta)) => {
          (gh - ga) match {
            case 0 => Seq(Standing(th, 1, 0, 1, 0, gh, ga, 1), Standing(ta, 1, 0, 1, 0, ga, gh, 1))
            case x if (x > 0) => Seq(Standing(th, 1, 1, 0, 0, gh, ga, 3), Standing(ta, 1, 0, 0, 1, ga, gh, 0))
            case _ => Seq(Standing(th, 1, 0, 0, 1, gh, ga, 0), Standing(ta, 1, 1, 0, 0, ga, gh, 3))
          }
        }
        case _ => Seq()
      }
    }
    val standings = (initialStandings ++ standingsAgg.flatten).groupBy(_.team).values.map(_.reduce(_ + _))
    lazy val standingsTieBreaker = standings.groupBy(_.points).values.filter(_.size > 1).map(x => {
        val teams = x.map(_.team).toList
        /* Check if we would run into endless recursion */
        if (teams.size < this.teams.size) {
          Group("", teams, games.filter(g => (g.teamAway.team, g.teamHome.team) match {
              case (Right(a), Right(b)) => teams.contains(a) && teams.contains(b)
              case _ => false
          })).standings
        } else {
          Seq()
        }
      }).flatten

    standings.toList.sortWith((x, y) => {
      (x.points > y.points) || (x.points == y.points && {
        val x2 = standingsTieBreaker.filter(s => s.team == x.team).headOption
        val y2 = standingsTieBreaker.filter(s => s.team == y.team).headOption
        val tiebreaker = (x2, y2) match {
          case (Some(x3), Some(y3)) => x3.compareTo(y3)
          case _ => 0
        }
        (tiebreaker < 0) || (tiebreaker == 0 && {
          val diffX = x.goalsScored - x.goalsReceived
          val diffY = y.goalsScored - y.goalsReceived
          (diffX > diffY || (diffX == diffY && (
            (x.goalsScored > y.goalsScored) || ((x.goalsScored == y.goalsScored) &&
              x.team.uefaCoefficient > y.team.uefaCoefficient
            )
          )))
        })
      })
    })
  }
}

case class Standing(team: Team, gamesPlayed: Int, won: Int, drawn: Int, lost: Int, goalsScored: Int,
                    goalsReceived: Int, points: Int) extends Ordered[Standing] {
  def +(s: Standing) = {
    if (s.team != team) {
      throw new IllegalArgumentException("Teams do not match: %s != %s".format(team, s.team))
    }
    Standing(team, gamesPlayed + s.gamesPlayed, won + s.won, drawn + s.drawn, lost + s.lost,
             goalsScored + s.goalsScored, goalsReceived + s.goalsReceived, points + s.points)
  }

  override def compare(that: Standing) = {
    (this.points - that.points) match {
      case x if x > 0 => -1
      case x if x < 0 => 1
      case _ => ((this.goalsScored - this.goalsReceived) - (that.goalsScored - that.goalsReceived)) match {
        case y if y > 0 => -1
        case y if y < 0 => 1
        case y => that.goalsScored - this.goalsScored
      }
    }
  }
}