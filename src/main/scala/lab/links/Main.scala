package lab.links

import uk.co.bigbeeconsultants.http.HttpClient
import uk.co.bigbeeconsultants.http.response.Response
import java.net.URL
import akka.actor._

case class Link(url: String, depth: Int)
case class GetRanking(url: String, depth: Int, client: ActorRef)
case class Get(url: String, level: Int)
case class Hrefs(hrefs: List[String], level: Int)
case class Ranking(ranking: List[(String, Int)])
case object ShutDown

class Controller extends Actor {
  def withDepth(depth: Int, client: ActorRef, waiting: Set[ActorRef], ranking: List[(String, Int)]): Receive = {
    case Hrefs(hrefs, level) =>
      val waiting2 = waiting.filterNot(_ == sender) // lista waiting oprocz tego gettera ktory wyslal te linki
      var getBuf = scala.collection.mutable.Set.empty[ActorRef] // pojemnik tymczasowy na set getterow
      var rankBuf = scala.collection.mutable.ListBuffer.empty[(String, Int)] // pojemnik tymczasowy na liste rankingowa
      // dla każdego linku stwórz gettera i wloz link do pojemnika
      for (href <- hrefs) yield {
        if (level < depth) {
          val getter = context.actorOf(Props[Getter])
          getter ! Get(href, level + 1)
          getBuf = getBuf + getter
        }
        rankBuf = rankBuf :+ (href, 1) // funkcja wkladająca ... nie wiem czemu akurat :+ a nie + ale z plusem nie dziala
      }
      //zaktualizuj liste waitingow i rankingow w zaleznosci od potrzeb
      if (level < depth)
        context.become(withDepth(depth, client, waiting2 ++ getBuf.toSet, ranking ++ rankBuf.toList))
      else
        context.become(withDepth(depth, client, waiting2, ranking ++ rankBuf.toList))
      // warunek skonczenia pracy controllera
      if (waiting2 == Set.empty && level >= depth) {
        println("gettery zakonczyly dzialanie ")
        client ! Ranking(ranking)
        context.stop(self)
      }
  }
  def receive = {
    //ta czesc kodu tylko raz bedzie wywolywana, na
    case GetRanking(url, depth, client) => {
      val getter = context.actorOf(Props[Getter])
      getter ! Get(url, 1)
      context.become(withDepth(depth, client, Set.empty + getter, List.empty))
    }
  }
}

class Getter extends Actor {
  def receive = {
    case Get(url, level) => {
      val linkRE = """href=['"](http[s]?://[^'"]+)""".r // zmodyfikowac zeby bral tylko http://server
      val httpClient = new HttpClient
      val response: Response = httpClient.get(new URL(url))
      val links = linkRE findAllIn response.body.asString
      sender ! Hrefs(links.toList.map(_.substring(6)), level)
      context.stop(self)
    }
  }
}

object Main {
  import akka.actor.ActorDSL._
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("system")
    actor(system, "main")(new Act {
      val controller = context.actorOf(Props[Controller], "Controller")
      controller ! GetRanking("http://www.kubavic.vdl.pl/", 4, self)
      context.become(waitingForRanking())
      def waitingForRanking(): Receive = {
        case Ranking(ranking) => {
          println("wypisuje liste")
          printList(ranking)
          system.shutdown
        }
      }

    })
  }
  def printList(args: List[_]): Unit = {
    args.foreach(println)
  }

}
/*
				ranking.toMap.get(href) match {
					case Some(x) => {
						val indeks = ranking.toMap.keys.toList.indexOf(href)
						context.become(withDepth(depth,client,waiting2 + getter,ranking.updated(indeks,(href,x+1))))
					 }
					case None =>{
						context.become(withDepth(depth,client,waiting2 + getter,ranking.::(href,1)))
					}
				}
				*/ 