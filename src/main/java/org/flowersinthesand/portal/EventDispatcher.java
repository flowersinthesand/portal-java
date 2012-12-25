package org.flowersinthesand.portal;

import java.lang.reflect.Method;

public interface EventDispatcher {

	void on(String event, Object handler, Method method);

	void on(String event, Socket socket, Fn.Callback handler);

	void on(String event, Socket socket, Fn.Callback1<?> handler);

	void on(String event, Socket socket, Fn.Callback2<?, ?> handler);

	void fire(String on, Socket socket);

	void fire(String on, Socket socket, Object data);

	void fire(String on, Socket socket, Object data, Fn.Callback1<Object> reply);

}