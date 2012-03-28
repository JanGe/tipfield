package de.dimond.tippspiel.model

import org.joda.time.{DateTime, DateTimeZone}

import org.scala_tools.time.Imports._

object GameData {

  def init(forTesting: Boolean = false) {
    def createDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, timeZone: DateTimeZone) = {
      if (forTesting) {
        DateTime.now + (day - 15).minutes + (month - 6).hours
      } else {
        new DateTime(year, month, day, hour, minute, timeZone)
      }
    }
    // Group A
    val poland = Team("Poland", "poland.png", 23806)
    val greece = Team("Greece", "greece.png", 29995)
    val russia = Team("Russia", "russia.png", 30671)
    val czechRepublic = Team("Czech Republic", "czech_republic.png", 26456)

    // Group B
    val netherlands = Team("Netherlands", "netherlands.png", 39660)
    val denmark = Team("Denmark", "denmark.png", 28105)
    val germany = Team("Germany", "germany.png", 37966)
    val portugal = Team("Portugal", "portugal.png", 29677)

    // Group C
    val spain = Team("Spain", "spain.png", 40016)
    val italy = Team("Italy", "italy.png", 32697)
    val ireland = Team("Ireland", "ireland.png", 25743)
    val croatia = Team("Croatia", "croatia.png", 31523)

    // Group D
    val ukraine = Team("Ukraine", "ukraine.png", 28029)
    val sweden = Team("Sweden", "sweden.png", 29235)
    val france = Team("France", "france.png", 28848)
    val england = Team("England", "england.png", 33063)


    val utc3 = DateTimeZone.forOffsetHours(3)
    val utc2 = DateTimeZone.forOffsetHours(2)

    val warsaw = Location("Warsaw")
    val gdansk = Location("Gdańsk")
    val wroclaw = Location("Wrocław")
    val poznan = Location("Poznań")

    val kiev = Location("Kiev")
    val donetsk = Location("Donetsk")
    val kharkiv = Location("Kharkiv")
    val lviv = Location("Lviv")

    val game01 = Game(1, poland.reference, greece.reference, createDateTime(2012, 6, 8, 18, 00, utc2), warsaw)
    val game02 = Game(2, russia.reference, czechRepublic.reference, createDateTime(2012, 6, 8, 20, 45, utc2), wroclaw)
    val game03 = Game(3, netherlands.reference, denmark.reference, createDateTime(2012, 6, 9, 19, 00, utc3), kharkiv)
    val game04 = Game(4, germany.reference, portugal.reference, createDateTime(2012, 6, 9, 21, 45, utc3), lviv)
    val game05 = Game(5, spain.reference, italy.reference, createDateTime(2012, 6, 10, 18, 00, utc2), gdansk)
    val game06 = Game(6, ireland.reference, croatia.reference, createDateTime(2012, 6, 10, 20, 45, utc2), poznan)
    val game07 = Game(7, france.reference, england.reference, createDateTime(2012, 6, 11, 19, 45, utc3), donetsk)
    val game08 = Game(8, ukraine.reference, sweden.reference, createDateTime(2012, 6, 11, 21, 45, utc3), kiev)

    val game09 = Game(9, greece.reference, czechRepublic.reference, createDateTime(2012, 6, 12, 18, 00, utc2), wroclaw)
    val game10 = Game(10, poland.reference, russia.reference, createDateTime(2012, 6, 12, 20, 45, utc2), warsaw)
    val game11 = Game(11, denmark.reference, portugal.reference, createDateTime(2012, 6, 13, 19, 00, utc3), lviv)
    val game12 = Game(12, netherlands.reference, germany.reference, createDateTime(2012, 6, 13, 21, 45, utc3), kharkiv)
    val game13 = Game(13, italy.reference, croatia.reference, createDateTime(2012, 6, 14, 18, 00, utc2), poznan)
    val game14 = Game(14, spain.reference, ireland.reference, createDateTime(2012, 6, 14, 20, 45, utc2), gdansk)
    val game15 = Game(15, ukraine.reference, france.reference, createDateTime(2012, 6, 15, 19, 45, utc3), donetsk)
    val game16 = Game(16, sweden.reference, england.reference, createDateTime(2012, 6, 15, 21, 45, utc3), kiev)

    val game17 = Game(17, czechRepublic.reference, poland.reference, createDateTime(2012, 6, 16, 20, 45, utc2), wroclaw)
    val game18 = Game(18, greece.reference, russia.reference, createDateTime(2012, 6, 16, 20, 45, utc2), warsaw)
    val game19 = Game(19, portugal.reference, netherlands.reference, createDateTime(2012, 6, 17, 21, 45, utc3), lviv)
    val game20 = Game(20, denmark.reference, germany.reference, createDateTime(2012, 6, 17, 21, 45, utc3), kharkiv)
    val game21 = Game(21, croatia.reference, spain.reference, createDateTime(2012, 6, 18, 20, 45, utc2), gdansk)
    val game22 = Game(22, italy.reference, ireland.reference, createDateTime(2012, 6, 18, 20, 45, utc2), poznan)
    val game23 = Game(23, england.reference, ukraine.reference, createDateTime(2012, 6, 19, 21, 45, utc3), donetsk)
    val game24 = Game(24, sweden.reference, france.reference, createDateTime(2012, 6, 19, 21, 45, utc3), kiev)

    val groupA = Group("A", Seq(poland, greece, russia, czechRepublic),
                       Seq(game01, game02, game09, game10, game17, game18))
    val groupB = Group("B", Seq(netherlands, denmark, germany, portugal),
                       Seq(game03, game04, game11, game12, game19, game20))
    val groupC = Group("C", Seq(spain, italy, ireland, croatia),
                       Seq(game05, game06, game13, game14, game21, game22))
    val groupD = Group("D", Seq(ukraine, sweden, france, england),
                       Seq(game07, game08, game15, game16, game23, game24))

    Group.init(Seq(groupA, groupB, groupC, groupD))

    val game25 = Game(25, GroupWinner(groupA), GroupRunnerUp(groupB),
                      createDateTime(2012, 6, 21, 20, 45, utc2), warsaw)
    val game26 = Game(26, GroupWinner(groupB), GroupRunnerUp(groupA),
                      createDateTime(2012, 6, 22, 20, 45, utc2), gdansk)
    val game27 = Game(27, GroupWinner(groupC), GroupRunnerUp(groupD),
                      createDateTime(2012, 6, 23, 21, 45, utc3), donetsk)
    val game28 = Game(28, GroupWinner(groupD), GroupRunnerUp(groupC),
                      createDateTime(2012, 6, 24, 21, 45, utc3), kiev)

    val game29 = Game(29, GameWinner(game25), GameWinner(game27),
                      createDateTime(2012, 6, 27, 21, 45, utc3), donetsk)
    val game30 = Game(30, GameWinner(game26), GameWinner(game28),
                      createDateTime(2012, 6, 28, 20, 45, utc2), warsaw)

    val game31 = Game(31, GameWinner(game29), GameWinner(game30),
                      createDateTime(2012, 7, 1, 21, 45, utc3), kiev)

    val matchDay1 = MatchDay("matchday1", "Matchday 1",
                             Seq(game01, game02, game03, game04, game05, game06, game07, game08))
    val matchDay2 = MatchDay("matchday2", "Matchday 2",
                             Seq(game09, game10, game11, game12, game13, game14, game15, game16))
    val matchDay3 = MatchDay("matchday3", "Matchday 3",
                             Seq(game17, game18, game19, game20, game21, game22, game23, game24))
    val quarterFinals = MatchDay("quarter-finals", "Quarter-finals", Seq(game25, game26, game27, game28))
    val semiFinals = MatchDay("semi-finals", "Semi-finals", Seq(game29, game30))
    val matchDayFinal = MatchDay("final", "Final", Seq(game31))

    MatchDay.init(Seq(matchDay1, matchDay2, matchDay3, quarterFinals, semiFinals, matchDayFinal))
  }
}
