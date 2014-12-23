package com.example.akkaTcpChat

import java.net.InetSocketAddress
import akka.actor.{Actor, Props, ActorRef}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import akka.event.Logging
import scala.util.{Failure, Success}

/* Client Actor Inputs */
case class DoConnect()
case class UserInput(msg: String)
case class UserName(name: String)
case class Disconnect()

/* Client Actor Outputs */
case class CommandSuccess(cmd: Any)
case class CommandFailed(cmd: Any, reason: String)
case class OtherUserMessage(name: String, msg: String)
case class ConnectionClosed(why: String)

object Client {

  def props(interact: ActorRef, remote: InetSocketAddress): Props = {
    Props(new Client(interact, remote))
  }

}

class Client(interact: ActorRef, remote: InetSocketAddress) extends Actor {

  import context.system
  import scala.concurrent.duration._

  val log = Logging(context.system, this)
  val connectionTimeout = 30.seconds


  def receive = {
    case cmd@DoConnect() =>
      IO(Tcp) ! Tcp.Connect(remote, timeout = Some(connectionTimeout))
      context.become(waitConnectionResult(cmd))
  }

  def waitConnectionResult(cmd: DoConnect): Actor.Receive = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      interact ! CommandFailed(cmd, "Connect command failed")
      context.stop(self)

    case Tcp.Connected(_, _) =>
      val connection = sender
      connection ! Tcp.Register(self)
      interact ! CommandSuccess(cmd)
      context.become(waitClientInit(connection))
  }

  def waitClientInit(connection: ActorRef): Actor.Receive = {
    case cmd@UserName(name) =>
      val req = new Common.Request(Common.CLIENT_INIT)
      req("name") = name
      req.serializeAsByteString match {
        case Success(b) =>
          connection ! Tcp.Write(b)
          interact ! CommandSuccess(cmd)
          context.become(chatLoop(connection))
        case Failure(e) =>
          interact ! CommandFailed(cmd, e.getMessage)
      }

    case _: Tcp.ConnectionClosed =>
      log.debug("Connection closed")
      interact ! ConnectionClosed("Closed by TCP")
      context.stop(self)

  }

  def chatLoop(connection: ActorRef): Actor.Receive = {
    case cmd @ UserInput(msg) =>
      val req = new Common.Request(Common.CLIENT_MESSAGE)
      req("msg") = msg
      req.serializeAsByteString match {
        case Success(b) =>
          connection ! Tcp.Write(b)
          interact ! CommandSuccess(cmd)
        case Failure(e) =>
          interact ! CommandFailed(cmd, e.getMessage)
      }

    case Disconnect =>
      log.debug("Disconnecting...")
      connection ! Tcp.Close
      context.stop(self)

    case Tcp.CommandFailed(w: Tcp.Write) =>
      log.debug("Write failed")

    case Tcp.Received(data) =>
      handleReceivedData(data, connection)

    case _: Tcp.ConnectionClosed =>
      log.debug("Connection closed")
      interact ! ConnectionClosed("Closed by TCP")
      context.stop(self)
  }

  def handleReceivedData(data: ByteString, connection: ActorRef) {
    Common.Request.deserializeFromByteString(data) match {
      case Success(req) =>
        req.request match {
          case Common.OTHER_CLIENT_MESSAGE =>
            val name = req("name").asInstanceOf[String]
            val message = req("msg").asInstanceOf[String]
            interact ! OtherUserMessage(name, message)
        }
      case Failure(e) =>
        throw e
    }
  }

}
