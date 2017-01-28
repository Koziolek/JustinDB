package justin.db

import akka.actor.Props
import akka.routing.{DefaultResizer, RoundRobinPool}

object RoundRobinCoordinatorRouter {
  def routerName: String = "CoordinatorRouter"

  private val pool = RoundRobinPool(
    nrOfInstances = 5,
    resizer = Some(DefaultResizer(lowerBound = 2, upperBound = 15))
  )

  def props(readCoordinator: ReplicaReadCoordinator, writeCoordinator: ReplicaWriteCoordinator): Props = {
    pool.props(ReplicaCoordinatorActor.props(readCoordinator, writeCoordinator))
  }
}
