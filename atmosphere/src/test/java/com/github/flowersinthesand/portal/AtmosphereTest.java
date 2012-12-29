///*
// * Copyright 2012 Donghwan Kim
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
///*
// * Copyright 2012 Jean-Francois Arcand
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License. You may obtain a copy of
// * the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations under
// * the License.
// */
//package com.github.flowersinthesand.portal;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
//
//import javax.servlet.ServletConfig;
//import javax.servlet.ServletContext;
//import javax.servlet.ServletException;
//
//import org.atmosphere.cpr.Action;
//import org.atmosphere.cpr.AsynchronousProcessor;
//import org.atmosphere.cpr.AtmosphereFramework;
//import org.atmosphere.cpr.AtmosphereHandler;
//import org.atmosphere.cpr.AtmosphereRequest;
//import org.atmosphere.cpr.AtmosphereResourceImpl;
//import org.atmosphere.cpr.AtmosphereResponse;
//import org.junit.Assert;
//import org.mockito.Mockito;
//import org.testng.annotations.AfterMethod;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import com.github.flowersinthesand.portal.handler.ProtocolsHandler;
//
//public class AtmosphereTest {
//
//	List<File> files;
//	AtmosphereFramework framework;
//
//	@BeforeMethod
//	public void before() throws Throwable {
//		files = new ArrayList<File>();
//		files.add(new File(Thread.currentThread().getContextClassLoader().getResource("").toURI()));
//		
//		framework = new AtmosphereFramework();
//		framework.setAsyncSupport(new AsynchronousProcessor(framework.getAtmosphereConfig()) {
//			@Override
//			public Action service(AtmosphereRequest req, AtmosphereResponse res)
//					throws IOException, ServletException {
//				return suspended(req, res);
//			}
//			
//			@Override
//			public void action(AtmosphereResourceImpl r) {
//				try {
//					resumed(r.getRequest(), r.getResponse());
//				} catch (IOException e) {
//					e.printStackTrace();
//				} catch (ServletException e) {
//					e.printStackTrace();
//				}
//			}
//		})
//		.init(new ServletConfig() {
//			@Override
//			public String getServletName() {
//				return "void";
//			}
//
//			@Override
//			public ServletContext getServletContext() {
//				return Mockito.mock(ServletContext.class);
//			}
//
//			@Override
//			public String getInitParameter(String name) {
//				return null;
//			}
//
//			@Override
//			public Enumeration<String> getInitParameterNames() {
//				return null;
//			}
//		});
//
//		App.clear();
//		for (App app : new Initializer().init(files).apps().values()) {
//			framework.addAtmosphereHandler(app.name(), (AtmosphereHandler) app.socketManager());
//		}
//	}
//
//	@AfterMethod
//	public void after() {
//		framework.destroy();
//	}
//
//	@Test
//	public void ws() throws IOException, ServletException {
//		framework.doCometSupport(
//			new AtmosphereRequest.Builder()
//				.pathInfo("/protocols")
//				.queryString("id=b564f6aa-4dd2-45a0-abf3-6c700614198f&transport=ws&heartbeat=false&lastEventId=0&_1333818006226")
//				.build(), 
//			new AtmosphereResponse.Builder().build());
//
//		Assert.assertTrue(ProtocolsHandler.opened);
//	}
//
//}
