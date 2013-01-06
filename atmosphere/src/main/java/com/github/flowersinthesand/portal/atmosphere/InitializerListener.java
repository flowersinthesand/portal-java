package com.github.flowersinthesand.portal.atmosphere;

import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import com.github.flowersinthesand.portal.App;

public class InitializerListener implements ServletContextListener {

	@SuppressWarnings("serial")
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		sce.getServletContext().addServlet("portal", new InitializerServlet() {
			@Override
			public void init(ServletConfig sc) throws ServletException {
				super.init(sc);
				ServletRegistration registration = getServletContext().getServletRegistration("portal");
				for (Entry<String, App> entry : initializer.apps().entrySet()) {
					registration.addMapping(entry.getKey());
				}
			}
		})
		.setLoadOnStartup(0);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {}

}
