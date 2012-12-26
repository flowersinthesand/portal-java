package org.flowersinthesand.portal.atmosphere;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowersinthesand.portal.App;
import org.flowersinthesand.portal.Fn;
import org.flowersinthesand.portal.Socket;

public class AtmosphereSocket extends Socket {

	private AtomicInteger eventId = new AtomicInteger(0);
	private Map<Integer, Fn.Callback1<Object>> callbacks = new ConcurrentHashMap<Integer, Fn.Callback1<Object>>();

	public AtmosphereSocket(String id, App app, Map<String, String[]> params) {
		super(id, app, params);
	}

	public AtomicInteger eventId() {
		return eventId;
	}

	public Map<Integer, Fn.Callback1<Object>> callbacks() {
		return callbacks;
	}

}
