package com.example.akkaTcpChat


import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import java.net.InetSocketAddress
import akka.util.Timeout
import scala.concurrent._

class DisplayException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

object Main extends App {

  lazy val addr = {
    new InetSocketAddress("localhost", 8842)
  }

	override def main(args: Array[String]) = {

    try {
      if (args.length < 1) {
        throw new Exception("Run option is not specified")
      }

      println("Started as " + args(0))

      args(0) match {
        case "server" => runServer()
        case "client" => runClient()
        case _ =>
          throw new Exception("Unknown run option")
      }
    } catch {
      case _ : InterruptedException =>
        println("We got an interrupted exception")
        sys.exit(0)
      case e : DisplayException =>
        println(e.getMessage)
        sys.exit(0)
    }

	}

  /**
   * Server role
   */
  def runServer() {
    println(addr)
    val actorSystem = createActorSystem()
    actorSystem.actorOf(Server.props(addr), "server")
  }

  /**
   * Client role
   */
  def runClient() {
    val actorSystem = createActorSystem()
    implicit def system: ActorSystem = actorSystem
    val clientActor: ActorRef = actorSystem.actorOf(Client.props(addr), "client")
    try {
      import scala.concurrent.duration._
      val timeout = new Timeout(30 seconds)
      val future = clientActor.ask(DoConnect())(timeout.duration)
      val result = Await.result(future, timeout.duration).asInstanceOf[ConnectedResult]
      println(result)
    } catch {
      case _ : TimeoutException =>
        throw new DisplayException("Connection timed out")
    }

    def readName() {
      while (true) {
        println("Enter your name:")
        val name = readLine()
        if (name != null && name.length > 1) {
          clientActor ! UserName(name.take(32).mkString)
          return
        }
      }
    }

    def readInput() {
      while (true) {
        clientActor ! UserInput(readLine())
      }
    }

    readName()
    readInput()
  }

  protected def createActorSystem() = {
    ActorSystem("client-server")
  }

}