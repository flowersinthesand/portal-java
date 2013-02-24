/*
 * Portal Vert.x
 * http://github.com/flowersinthesand/portal-java
 * 
 * Copyright 2012-2013, Donghwan Kim 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
(function(portal) {
	portal.defaults.heartbeat = 20000;
	portal.defaults.notifyAbort = true;
	portal.defaults._urlBuilder = portal.defaults.urlBuilder;
	portal.defaults.urlBuilder = function(url, params, when) {
		var value = portal.defaults._urlBuilder.apply(this, arguments);
		return (when === "open" && params.transport === "ws") ? value.replace("?", "@") : value;
	};
})(window.portal);