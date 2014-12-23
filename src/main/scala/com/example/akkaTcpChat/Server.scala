package com.example.akkaTcpChat

import akka.actor.{ Props, Actor }
import akka.io.{ IO, Tcp }
import java.net.InetSocketAddress
import com.example.akkaTcpChat.handler.Hub
import akka.event.Logging

object Server {

  def props(host: InetSocketAddress): Props = {
    Props(new Server(host))
  }

}

class Server(host: InetSocketAddress) extends Actor {

  import context.system

  val log = Logging(context.system, this)

  IO(Tcp) ! Tcp.Bind(self, host)
  val hub = context.actorOf(Props[Hub])

  def receive  = {
    case b @ Tcp.Bound(localAddress) =>
      log.debug("Spawned " + localAddress.toString)

    case CommandFailed(_ : Tcp.Bind) =>
      println("Command failed. Going to stop server.")
      context stop self

    case c @ Tcp.Connected(remote, local) =>
      log.debug("Connected " + remote.toString)
      hub ! Hub.Register(remote.toString, sender)
  }

}
