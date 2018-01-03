/*
 * Copyright 2017 Barclays Africa Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.core.streaming

import org.apache.spark.sql.execution.streaming.{StreamExecution, StreamingQueryWrapper}
import org.apache.spark.sql.streaming.{StreamingQuery, StreamingQueryListener, StreamingQueryManager}
import org.slf4s.Logging
import za.co.absa.spline.persistence.api.DataLineageWriter

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Success
import scala.language.postfixOps

/**
  * The class represents a handler listening on Spark Structured Streaming events.
  * @param manager An instance managing various Spark Structured Streaming queries
  * @param harvester An harvester capturing lineage information from the stream execution
  * @param persistenceWriter A writer pushing lineage information to the underlying persistence layer
  */
class StructuredStreamingListener(manager : StreamingQueryManager, harvester : StructuredStreamingLineageHarvester, persistenceWriter: DataLineageWriter)
  extends StreamingQueryListener with Logging {

  import scala.concurrent.ExecutionContext.Implicits._

  /**
    * The method handles events representing a start of the structured streaming application.
    * @param event An instance representing a start of the structured streaming application
    */
  override def onQueryStarted(event: StreamingQueryListener.QueryStartedEvent): Unit = {
    log debug s"Structured streaming query(id: ${event.id}, runId: ${event.runId}) has started."
    val query = manager.get(event.id)
    processQuery(query)
  }

  private def processQuery(query: StreamingQuery): Unit = query match {
    case se : StreamExecution =>
      val lineage = harvester.harvestLineage(se)
      val eventuallyStored = persistenceWriter.store(lineage) andThen { case Success(_) => log debug s"Lineage is persisted" }
      Await.result(eventuallyStored, 10 minutes)
    case sw : StreamingQueryWrapper => processQuery(sw.streamingQuery)
    case x => log error s"Trying to process unknown query '$x'."
  }

  /**
    * The method handles events representing a finished processing of a micro batch.
    * @param event An instance representing a finished processing of a micro batch
    */
  override def onQueryProgress(event: StreamingQueryListener.QueryProgressEvent): Unit = {}

  /**
    * The method handles events representing a termination of the structured streaming application.
    * @param event An instance representing a termination of the structured streaming application
    */
  override def onQueryTerminated(event: StreamingQueryListener.QueryTerminatedEvent): Unit = {
    val queryStringRep = s"Structured streaming query(id: ${event.id}, runId: ${event.runId})"

    event.exception match {
      case None => log debug s"'$queryStringRep' successfully terminated."
      case Some(err) => log debug s"'$queryStringRep' terminated with the exception '$err'"
    }
  }
}
