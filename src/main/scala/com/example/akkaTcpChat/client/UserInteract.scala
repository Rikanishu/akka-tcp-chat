package com.example.akkaTcpChat.client

import akka.actor.{ReceiveTimeout, Actor}
import com.example.akkaTcpChat._

case class InputUserMessage(msg: String)

class UserInteract extends Actor {

  import context.system
  import scala.concurrent.duration._

  val client = system.actorOf(Client.props(self, Main.addr), "client")
  context.setReceiveTimeout(30.seconds)
  client ! DoConnect()

  def receive = {

    case CommandSuccess(_) =>
      println("Enter your name:")
      context.become({

        case InputUserMessage(name) =>
          if (name != null && name.length > 1) {
            client ! UserName(name.take(32).mkString)
            context.become({

              case CommandSuccess(_) =>
                context.become({

                  case OtherUserMessage(otherUserName, otherUserMessage) =>
                    println(otherUserName + ": " + otherUserMessage)

                  case InputUserMessage(msg) =>
                    client ! UserInput(msg)

                  case ConnectionClosed =>
                    exit("Connection with server closed")
                })

              case CommandFailed(_, msg) =>
                exit("Problems while sending user info to server: " + msg)
            })
          } else {
            println("Invalid name")
          }
      })

    case CommandFailed(_, msg) =>
      exit("Problems with connect to server: " + msg)

    case ReceiveTimeout =>
      exit("Connection timed out")
  }

  def exit(msg: String) {
    println(msg)
    context.stop(self)
  }

}
