package com.example.akkaTcpChat

import akka.actor.{Props, ActorSystem}
import java.net.InetSocketAddress
import com.example.akkaTcpChat.client.{InputUserMessage, UserInteract}

object Main extends App {

  lazy val addr = {
    new InetSocketAddress("localhost", 8842)
  }

	override def main(args: Array[String]) = {

    try {
      if (args.length < 1) {
        exit("Run option is not specified")
      }

      println("Started as " + args(0))

      args(0) match {
        case "server" => runServer()
        case "client" => runClient()
        case _ =>
          exit("Unknown run option")
      }
    } catch {
      case _ : InterruptedException =>
        exit("We got an interrupted exception")
      case e : DisplayException =>
        exit(e.getMessage)
    }

	}

  def exit(msg: String, code: Int) {
    println(msg)
    sys.exit(code)
  }

  def exit(msg: String) {
    exit(msg, 1)
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

    val userInteract = actorSystem.actorOf(Props[UserInteract], "user-interact")
    while (true) {
      userInteract ! InputUserMessage(readLine())
    }
  }

  protected def createActorSystem() = {
    ActorSystem("client-server")
  }

}

class DisplayException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)