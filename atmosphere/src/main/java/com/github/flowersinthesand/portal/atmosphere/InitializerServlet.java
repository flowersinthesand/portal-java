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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereServlet;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Initializer;
import com.github.flowersinthesand.portal.spi.SocketManager;

@SuppressWarnings("serial")
public class InitializerServlet extends AtmosphereServlet {

	private final Logger logger = LoggerFactory.getLogger(InitializerServlet.class);
	private ObjectMapper mapper = new ObjectMapper();
	protected Initializer initializer = new Initializer();

	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
		
		Map<String, Object> options = new LinkedHashMap<String, Object>() {{
			String base = getServletContext().getRealPath("");
			put("base", base);
			if (base != null) {
				put("locations", new LinkedHashSet<String>() {{
					add("/WEB-INF/classes");
				}});
			}
		}};

		String userOptions = getServletContext().getInitParameter("portal.options");
		if (userOptions != null) {
			logger.debug("Reading portal.options {}", userOptions);
			try {
				Map<String, Object> map = mapper.readValue(userOptions, new TypeReference<Map<String, Object>>() {});
				options.putAll(map);
			} catch (IOException e) {
				logger.error("Failed to read the JSON which comes from the portal.option " + userOptions, e);
			}
		}
		
		Map<Class<?>, Class<?>> classes = new LinkedHashMap<Class<?>, Class<?>>() {{
			put(SocketManager.class, AtmosphereSocketManager.class);
		}};

		String userClasses = getServletContext().getInitParameter("portal.classes");
		if (userClasses != null) {
			logger.debug("Reading portal.classes {}", userClasses);
			try {
				Map<String, String> map = mapper.readValue(userClasses, new TypeReference<Map<String, String>>() {});
				for (Entry<String, String> entry : map.entrySet()) {
					try {
						classes.put(Class.forName(entry.getKey()), Class.forName(entry.getValue()));
					} catch (ClassNotFoundException e) {
						logger.error("Class " + e.getMessage() + "not found", e);
					}
				}
			} catch (IOException e) {
				logger.error("Failed to read the JSON which comes from the portal.classes " + userClasses, e);
			}
		}

		configure(options, classes);

		initializer.init(options, classes);
		for (Entry<String, App> entry : initializer.apps().entrySet()) {
			framework.addAtmosphereHandler(entry.getKey(), entry.getValue().bean(AtmosphereHandler.class));
		}
	}

	protected void configure(Map<String, Object> options, Map<Class<?>, Class<?>> classes) {}

}
