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
import java.util.Collection;
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
import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.spi.SocketManager;

@SuppressWarnings("serial")
public class InitializerServlet extends AtmosphereServlet {

	private final Logger logger = LoggerFactory.getLogger(InitializerServlet.class);
	private ObjectMapper mapper = new ObjectMapper();
	protected Initializer initializer = new Initializer();

	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);

		Options options = new Options();
		if (options.base(getServletContext().getRealPath("")).base() != null) {
			options.locations("/WEB-INF/classes");
		}
		options.classes(SocketManager.class, AtmosphereSocketManager.class);

		String userOptions = getServletContext().getInitParameter("portal.options");
		if (userOptions != null) {
			applyUserOptions(options, userOptions);
		}
		
		configure(options);
		initializer.init(options);
		for (Entry<String, App> entry : initializer.apps().entrySet()) {
			framework.addAtmosphereHandler(entry.getKey(), entry.getValue().bean(AtmosphereHandler.class));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void applyUserOptions(Options options, String userOptions) {
		logger.debug("Reading portal.options {}", userOptions);
		Map<String, Object> map;
		
		try {
			map = mapper.readValue(userOptions, new TypeReference<Map<String, Object>>() {});
		} catch (IOException e) {
			logger.error("Failed to read the JSON which comes from the portal.option " + userOptions, e);
			return;
		}
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (map.containsKey("controllers")) {
			Collection<String> controllers = (Collection<String>) map.get("controllers");
			for (String className : controllers) {
				try {
					options.controllers(classLoader.loadClass(className));
				} catch (ClassNotFoundException e) {
					logger.error("Controller class " + className + " not found", e);
				}
			}
		}
		if (map.containsKey("packages")) {
			options.packages(((Collection<?>) map.get("packages")).toArray(new String[]{}));
		}
		if (map.containsKey("base")) {
			options.base((String) map.get("base"));
		}
		if (map.containsKey("locations")) {
			options.locations(((Collection<?>) map.get("locations")).toArray(new String[]{}));
		}
		if (map.containsKey("classes")) {
			Map<String, String> classes = (Map<String, String>) map.get("classes");
			for (Entry<String, String> entry : classes.entrySet()) {
				try {
					Class spec = null, impl = null;
					try {
						spec = classLoader.loadClass(entry.getKey());
					} catch (ClassNotFoundException e) {
						logger.error("Spec class " + entry.getKey() + " not found", e);
						throw e;
					}
					try {
						impl = classLoader.loadClass(entry.getValue());
					} catch (ClassNotFoundException e) {
						logger.error("Impl class " + entry.getKey() + " not found", e);
						throw e;
					}
					options.classes(spec, impl);
				} catch (ClassNotFoundException e) {}
			}
		}
	}

	protected void configure(Options options) {}

}
