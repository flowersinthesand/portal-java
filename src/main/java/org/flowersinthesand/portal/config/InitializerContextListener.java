/*
 * Copyright 2012 Donghwan Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowersinthesand.portal.config;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.flowersinthesand.portal.Options;
import org.flowersinthesand.portal.dispatcher.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializerContextListener implements ServletContextListener {

	private final Logger logger = LoggerFactory.getLogger(InitializerContextListener.class);

	@Override
	public void contextInitialized(ServletContextEvent event) {
		Dispatcher dispatcher = new Dispatcher();
		event.getServletContext().setAttribute(Dispatcher.class.getName(), dispatcher);

		try {
			new Initializer(dispatcher).init(event.getServletContext().getInitParameter(Options.BASE_PACKAGE));
		} catch (IOException e) {
			logger.warn("", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {

	}

}
