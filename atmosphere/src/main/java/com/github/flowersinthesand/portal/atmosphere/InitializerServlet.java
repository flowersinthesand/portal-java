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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.flowersinthesand.portal.App;
import com.github.flowersinthesand.portal.Initializer;

@SuppressWarnings("serial")
public class InitializerServlet extends AtmosphereServlet {

	private final Logger logger = LoggerFactory.getLogger(InitializerServlet.class);
	private ObjectMapper mapper = new ObjectMapper();
	protected Initializer initializer = new Initializer();

	@SuppressWarnings("unchecked")
	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
		
		Map<String, Object> option = new LinkedHashMap<String, Object>() {{
			put("base", getServletContext().getRealPath(""));
			put("locations", new LinkedHashSet<String>() {{
				add("/WEB-INF/classes");
			}});
			put("socketManager", AtmosphereSocketManager.class.getName());
		}};

		String userJSON = getServletContext().getInitParameter("portal.options");
		if (userJSON != null) {
			try {
				option.putAll(mapper.readValue(userJSON, Map.class));
			} catch (IOException e) {
				logger.error("Failed to read the JSON which comes from the portal.option " + userJSON, e);
			}
		}

		initializer.init(option);
		for (Entry<String, App> entry : initializer.apps().entrySet()) {
			framework.addAtmosphereHandler(entry.getKey(), (AtmosphereHandler) entry.getValue().socketManager());
		}
	}

}
