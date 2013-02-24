/*
 * Copyright 2012-2013 Donghwan Kim
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
package com.github.flowersinthesand.portal.vertx;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

import com.github.flowersinthesand.portal.Options;
import com.github.flowersinthesand.portal.spi.Module;

public class VertxModule implements Module {

	private Vertx vertx;
	private HttpServer httpServer;

	public VertxModule(Vertx vertx, HttpServer httpServer) {
		this.vertx = vertx;
		this.httpServer = httpServer;
	}

	@Override
	public void configure(Options options) {
		options.bean("url", options.url()).bean(Vertx.class.getName(), vertx).bean(HttpServer.class.getName(), httpServer);
	}

}