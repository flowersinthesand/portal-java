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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

public class InitializerListener implements ServletContextListener {

	@SuppressWarnings("serial")
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletRegistration.Dynamic registration = sce.getServletContext().addServlet("portal", new InitializerServlet() {
			@Override
			public void init(ServletConfig sc) throws ServletException {
				super.init(sc);

				ServletRegistration registration = getServletContext().getServletRegistration("portal");				
				registration.addMapping(initializer.apps().keySet().toArray(new String[] {}));
			}
		});

		registration.setLoadOnStartup(0);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {}

}
