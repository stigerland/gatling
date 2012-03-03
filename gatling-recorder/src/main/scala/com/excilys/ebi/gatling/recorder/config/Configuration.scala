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
package com.excilys.ebi.gatling.recorder.config

import java.io.FileWriter
import java.io.IOException

import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.File

import org.apache.commons.io.IOUtils.closeQuietly

import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationsFolder
import com.excilys.ebi.gatling.recorder.ui.Commons.GATLING_RECORDER_FILE_NAME
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterStrategy.NONE
import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.XStream

import grizzled.slf4j.Logging

object Configuration extends Logging {

	val configuration = new Configuration
	lazy val DEFAULT_CLASS_NAME = "Simulation"

	private val XSTREAM = new XStream(new DomDriver)
	private val CONFIGURATION_FILE = File(System.getProperty("user.home") / GATLING_RECORDER_FILE_NAME)

	XSTREAM.alias("configuration", classOf[Configuration])
	XSTREAM.alias("pattern", classOf[Pattern])
	XSTREAM.alias("proxy", classOf[ProxyConfig])

	def apply(options: Options) {
		initFromDisk
		initFromCli(options)
	}

	def apply(c: Configuration) {
		configuration.port = c.port
		configuration.sslPort = c.sslPort
		configuration.proxy = c.proxy
		configuration.filterStrategy = c.filterStrategy
		configuration.patterns = c.patterns
		configuration.outputFolder = c.outputFolder 
		configuration.simulationClassName = c.simulationClassName
		configuration.simulationPackage = c.simulationPackage
		configuration.followRedirect = c.followRedirect
		configuration.saveConfiguration = true
		configuration.encoding = c.encoding
	}

	def saveToDisk {
		var fw: FileWriter = null
		try {
			fw = new FileWriter(CONFIGURATION_FILE.jfile)
			XSTREAM.toXML(configuration, fw)
		} catch {
			case e: IOException => error(e.getMessage)
		} finally {
			closeQuietly(fw)
		}
	}
	
	private def initFromDisk {
		if (CONFIGURATION_FILE.exists) {
			try {
				val c = XSTREAM.fromXML(CONFIGURATION_FILE.jfile).asInstanceOf[Configuration]
				Configuration(c)
			} catch {
				case e: Exception => error(e.getMessage)
			}
		}
	}

	private def initFromCli(o: Options) {
		o.localPort.map(configuration.port = _)
		o.localPortSsl.map(configuration.sslPort = _)
		o.proxyHost.map{ host =>
			o.proxyPort.map{ port =>
				o.proxyPortSsl.map{ sslPort =>
					configuration.proxy = new ProxyConfig(host, port, sslPort, "", "")
				}
			}
		}
		o.outputFolder.map(configuration.outputFolder = _)
		o.simulationClassName.map(configuration.simulationClassName = _)
		o.simulationPackage.map(pkg => configuration.simulationPackage = Some(pkg))
		o.requestBodiesFolder.map(configuration.requestBodiesFolder = _)
		o.encoding.map(configuration.encoding = _)
		o.followRedirect.map(configuration.followRedirect = _)
	}
}

class Configuration{

	var port = 8000
	var sslPort = 8001
	var proxy = ProxyConfig()
	var filterStrategy = NONE
	var patterns: List[Pattern] = Nil
	var outputFolder: String = Option(System.getenv("GATLING_HOME")).map(_ => simulationsFolder.toString).getOrElse(System.getProperty("user.home"))
	var saveConfiguration = false
	var encoding = "UTF-8"
	var requestBodiesFolder: String = null
	var simulationPackage: Option[String] = None
	var simulationClassName: String = Configuration.DEFAULT_CLASS_NAME
	var followRedirect: Boolean = false 

	override def toString =
		new StringBuilder("Configuration [")
			.append("port=").append(port).append(", ")
			.append("sslPort=").append(sslPort).append(", ")
			.append("proxy=").append(proxy).append(", ")
			.append("filterStrategy=").append(filterStrategy).append(", ")
			.append("patterns=").append(patterns).append(", ")
			.append("outputFolder=").append(outputFolder).append(", ")
			.append("saveConfiguration=").append(saveConfiguration).append(", ")
			.append("encoding=").append(encoding).append(", ")
			.append("requestBodiesFolder=").append(encoding).append(", ")
			.append("simulationPackage=").append(simulationPackage).append(", ")
			.append("simulationClassName=").append(simulationClassName).append(", ")
			.append("followRedirect=").append(followRedirect)
			.append("]").toString
}
