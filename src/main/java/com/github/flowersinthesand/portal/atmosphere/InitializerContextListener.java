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
package com.github.flowersinthesand.portal.atmosphere;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Initializer;
import com.github.flowersinthesand.portal.Options;

public class InitializerContextListener implements ServletContextListener {

	private final Logger logger = LoggerFactory.getLogger(InitializerContextListener.class);

	@Override
	public void contextInitialized(ServletContextEvent event) {
		try {
			Initializer i = new Initializer().init(event.getServletContext().getInitParameter(Options.BASE_PACKAGE));
			for (Entry<String, App> entry : i.apps().entrySet()) {
				event.getServletContext().setAttribute("com.github.flowersinthesand.portal.App#" + entry.getKey(), entry.getValue());
				// TODO register atmosphere-handler
			}
		} catch (IOException e) {
			// TODO file scan error
			logger.error("", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {}

}
