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
package com.github.flowersinthesand.samples;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.atmosphere.InitializerListener;
import com.github.flowersinthesand.portal.spi.NewObjectFactory;
import com.github.flowersinthesand.portal.spi.ObjectFactory;

@WebListener
public class PortalListener extends InitializerListener {

	private ServletContext servletContext;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		servletContext = sce.getServletContext();
		super.contextInitialized(sce);
	}

	@Override
	protected void configure(Options options) {
		options.objectFactory(new NewObjectFactory() {
			@Override
			public <T> T create(Class<T> t) {
				System.out.println(t);
				return super.create(t);
			}
		});
	}

}