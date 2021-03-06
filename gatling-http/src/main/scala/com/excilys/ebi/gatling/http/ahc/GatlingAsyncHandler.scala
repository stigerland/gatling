/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.ahc

import java.lang.Void

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.cookie.CookieHandling
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.AsyncHandler.STATE.CONTINUE
import com.ning.http.client.{ Request, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart, AsyncHandler, ProgressAsyncHandler }

import akka.actor.{ Props, ActorRef }
import grizzled.slf4j.Logging

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 *
 * @constructor constructs a GatlingAsyncHandler
 * @param session the session of the scenario
 * @param checks the checks that will be done on response
 * @param next the next action to be executed
 * @param requestName the name of the request
 */
class GatlingAsyncHandler(session: Session, checks: List[HttpCheck], next: ActorRef, requestName: String, originalRequest: Request, followRedirect: Boolean)
		extends AsyncHandler[Void] with ProgressAsyncHandler[Void] with CookieHandling with Logging {

	private val actor = system.actorOf(Props(new GatlingAsyncHandlerActor(session, checks, next, requestName, originalRequest, followRedirect)))

	// only store bodyparts if they are to be analyzed
	val useBodyParts = checks.find(_.phase == CompletePageReceived).isDefined

	def onHeaderWriteCompleted = {
		actor ! new OnHeaderWriteCompleted
		CONTINUE
	}

	def onContentWriteCompleted = {
		actor ! new OnContentWriteCompleted
		CONTINUE
	}

	def onContentWriteProgress(amount: Long, current: Long, total: Long) = CONTINUE

	def onStatusReceived(responseStatus: HttpResponseStatus) = {
		actor ! new OnStatusReceived(responseStatus)
		CONTINUE
	}

	def onHeadersReceived(headers: HttpResponseHeaders) = {
		actor ! new OnHeadersReceived(headers)
		CONTINUE
	}

	def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
		if (useBodyParts) {
			actor ! new OnBodyPartReceived(bodyPart)
		}
		CONTINUE
	}

	def onCompleted: Void = {
		actor ! new OnCompleted
		null
	}

	def onThrowable(throwable: Throwable) {
		warn("Request '" + requestName + "' failed", throwable)
		val errorMessage = Option(throwable.getMessage).getOrElse(EMPTY)
		actor ! new OnThrowable(errorMessage)
	}
}