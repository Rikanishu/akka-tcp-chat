package com.example.akkaTcpChat

import java.net.InetSocketAddress
import akka.actor.{Actor, Props, ActorRef}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import akka.io.Tcp.Connected
import akka.io.Tcp.Received
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.event.Logging
import scala.util.{Failure, Success}

/* Client Actor Request */
case class DoConnect()
case class UserInput(msg: String)
case class UserName(name: String)
case class CloseConnection()

/* Client Actor Response */
case class ConnectedResult(success: Boolean)

object Client {

  def props(remote: InetSocketAddress): Props = {
    Props(new Client(remote))
  }

}

class Client(remote: InetSocketAddress) extends Actor {

  import context.system

  val log = Logging(context.system, this)

  def receive = {
    case DoConnect() =>
      IO(Tcp) ! Connect(remote)
      context.become(waitConnectionResult(sender))
  }

  def waitConnectionResult(depend: ActorRef): Actor.Receive = {
      case CommandFailed(_: Connect) =>
        depend ! ConnectedResult(success = false)
        context.stop(self)

      case Connected(_, _) =>
        val connection = sender
        connection ! Register(self)
        depend ! ConnectedResult(success = true)
        context.become(waitClientInit(connection))
  }

  def waitClientInit(connection: ActorRef): Actor.Receive = {
      case UserName(name) =>
        val req = new Common.Request(Common.CLIENT_INIT)
        req("name") = name
        req.serializeAsByteString match {
          case Success(b) =>
            connection ! Write(b)
            context.become(chatLoop(connection))
          case Failure(e) =>
            throw e
        }
  }

  def chatLoop(connection: ActorRef): Actor.Receive = {
      case UserInput(msg) =>
        val req = new Common.Request(Common.CLIENT_MESSAGE)
        req("msg") = msg
        req.serializeAsByteString match {
          case Success(b) =>
            connection ! Write(b)
          case Failure(e) =>
            throw e
        }

      case CommandFailed(w: Write) =>
        log.debug("Write failed")

      case Received(data) =>
        handleReceivedData(data, connection)

      case CloseConnection =>
        log.debug("Close connection")
        connection ! Close

      case _ : ConnectionClosed =>
        log.debug("Connection closed")
        context.stop(self)
  }

  def handleReceivedData(data: ByteString, connection: ActorRef) {
    Common.Request.deserializeFromByteString(data) match {
      case Success(req) =>
        req.request match {
          case Common.OTHER_CLIENT_MESSAGE =>
            val name = req("name").asInstanceOf[String]
            val message = req("msg").asInstanceOf[String]
            println(name + ": " + message)
        }
      case Failure(e) =>
        throw e
    }
  }

}
