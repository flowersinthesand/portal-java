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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.spi.Initializer;

public class AtmosphereInitializer implements Initializer {

	private final Logger logger = LoggerFactory.getLogger(AtmosphereInitializer.class);

	@Override
	public void init(Options options) {
		ServletContext context = options.bean(ServletContext.class);
		if (context != null && context.getMajorVersion() >= 3) {
			installAtmosphereServlet(context, options);
		}

		if (options.bean(AtmosphereFramework.class) == null) {
			throw new IllegalArgumentException("There is no AtmosphereFramework");
		}
	}

	private void installAtmosphereServlet(ServletContext context, Options options) {
		AtmosphereServlet servlet = null;
		try {
			servlet = context.createServlet(AtmosphereServlet.class);
		} catch (ServletException e) {
			throw new IllegalStateException(e);
		}

		ServletRegistration.Dynamic registration = context.addServlet("portal#" + options.name(), servlet);
		registration.setLoadOnStartup(0);
		registration.addMapping(options.url());
		logger.info("AtmosphereServlet '{}' is installed in accordance with the registration '{}'", servlet, registration);

		options.beans("url", options.url()).beans(servlet.framework());
	}

}
