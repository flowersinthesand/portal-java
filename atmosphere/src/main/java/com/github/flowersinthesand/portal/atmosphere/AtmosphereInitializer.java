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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.spi.InitializerAdapter;
import com.github.flowersinthesand.portal.spi.SocketManager;

public class AtmosphereInitializer extends InitializerAdapter {

	private final Logger logger = LoggerFactory.getLogger(AtmosphereInitializer.class);
	private App app;
	private AtmosphereFramework framework;

	@Override
	public Options init(App app, Map<String, Object> props) {
		this.app = app;

		ServletContext servletContext = (ServletContext) props.get("atmosphere.servletContext");
		if (servletContext.getMajorVersion() >= 3) {
			AtmosphereServlet servlet = null;
			try {
				servlet = servletContext.createServlet(AtmosphereServlet.class);
			} catch (ServletException e) {
				logger.error("something goes wrong", e);
				throw new IllegalStateException(e);
			}

			ServletRegistration.Dynamic registration = servletContext.addServlet("portal", servlet);
			registration.setLoadOnStartup(0);
			registration.addMapping(app.name());

			framework = servlet.framework();
		} else {
			framework = (AtmosphereFramework) props.get("atmosphere.framework");
		}

		Options options = new Options();
		if (options.base(servletContext.getRealPath("")).base() != null) {
			options.locations("/WEB-INF/classes");
		}

		return options.classes(SocketManager.class, AtmosphereSocketManager.class);
	}

	@Override
	public void postBeansInstantiation(Map<Class<?>, Object> beans) {
		AtmosphereSocketManager manager = app.unwrap(SocketManager.class);
		
		manager.setApp(app);		
		framework.addAtmosphereHandler(app.name(), manager);
	}

}
