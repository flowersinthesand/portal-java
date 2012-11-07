package org.flowersinthesand.portal;

public interface Socket {

	boolean opened();

	String param(String key);

	Socket on(String event, Fn.Callback handler);

	<A> Socket on(String event, Fn.Callback1<A> handler);

	<A, B> Socket on(String event, Fn.Callback2<A, B> handler);

	Socket send(String event);

	Socket send(String event, Object data);

	Socket send(String event, Object data, Fn.Callback callback);

	<A> Socket send(String event, Object data, Fn.Callback1<A> callback);

	void close();

}
