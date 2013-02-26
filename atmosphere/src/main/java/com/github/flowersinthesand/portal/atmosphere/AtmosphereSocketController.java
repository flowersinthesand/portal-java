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
package com.github.flowersinthesand.portal.atmosphere;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map.Entry;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.Init;
import com.github.flowersinthesand.portal.Wire;
import com.github.flowersinthesand.portal.spi.SocketController;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketController")
public class AtmosphereSocketController implements AtmosphereHandler, SocketController {

	@Wire
	private String url;
	@Wire
	private AtmosphereFramework framework;
	@Wire
	private AtmosphereSocketFactory socketFactory;

	@Init
	public void init() {
		framework.addAtmosphereHandler(url, this);
	}

	@Override
	public void onRequest(AtmosphereResource resource) throws IOException {
		AtmosphereRequest req = resource.getRequest();
		AtmosphereResponse res = resource.getResponse();

		req.setCharacterEncoding("utf-8");
		res.setCharacterEncoding("utf-8");

		for (Entry<String, String> entry : AbstractSocketFactory.noCacheHeader().entrySet()) {
			res.setHeader(entry.getKey(), entry.getValue());			
		}
		for (Entry<String, String> entry : AbstractSocketFactory.corsHeader(req.getHeader("Origin")).entrySet()) {
			res.setHeader(entry.getKey(), entry.getValue());			
		}

		if (req.getMethod().equalsIgnoreCase("GET")) {
			String when = req.getParameter("when");
			if (when.equals("open") || when.equals("poll")) {
				socketFactory.open(resource);
			} else if (when.equals("abort")) {
				socketFactory.abort(req.getParameter("id"));
			}
		} else if (req.getMethod().equalsIgnoreCase("POST")) {
			String raw = read(req.getReader());
			socketFactory.fire(raw.startsWith("data=") ? raw.substring("data=".length()) : raw);
		}
	}

	private String read(Reader in) throws IOException {
		StringWriter out = new StringWriter();
		
		try {
			char[] buffer = new char[4096];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
		} finally {
			try {
				in.close();
			} catch (IOException ex) {}
			try {
				out.close();
			} catch (IOException ex) {}
		}
		
		return out.toString();
	}

	@Override
	public void onStateChange(AtmosphereResourceEvent event) throws IOException {
		if (event.getMessage() == null || event.isCancelled() || event.isResuming() || event.isResumedOnTimeout()) {
			return;
		}

		AtmosphereResource resource = event.getResource();
		AtmosphereRequest req = resource.getRequest();
		AtmosphereResponse res = resource.getResponse();
		PrintWriter writer = res.getWriter();
		
		writer.print((String) event.getMessage());
		writer.flush();
		
		if (req.getParameter("transport").startsWith("longpoll")) {
			req.setAttribute("used", true);
			resource.resume();
		}
	}

	@Override
	public void destroy() {}
	
}
