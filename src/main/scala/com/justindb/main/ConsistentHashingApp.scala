package com.justindb.main

import akka.actor.ActorSystem
import com.justindb.actors.ConsistentHashingActor
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.justindb.{Key, Record}
import scala.concurrent.duration._
import scala.util.Random
import scala.language.postfixOps

object ConsistentHashingApp extends App {

  val port = if (args.isEmpty) "0" else args(0)
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [ringrole]")).
    withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterSystem", config)
  val consistentHashingActor = system.actorOf(Props[ConsistentHashingActor], name = "ringrole")

}
