package com.github.flowersinthesand.samples;

import javax.servlet.annotation.WebServlet;

import com.github.flowersinthesand.portal.atmosphere.InitializerServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = { "/chat/*" }, loadOnStartup = 0)
public class PortalServlet extends InitializerServlet {}