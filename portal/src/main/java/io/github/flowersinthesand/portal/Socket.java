/*
 * Copyright 2012-2014 Donghwan Kim
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
package io.github.flowersinthesand.portal;

import io.github.flowersinthesand.wes.Action;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Socket is a connectivity between the two portal endpoints.
 * <p>
 * Sockets may be accessed by multiple threads.
 * 
 * @author Donghwan Kim
 */
public interface Socket {

	/**
	 * The id.
	 */
	String id();

	/**
	 * The URI used to connect.
	 */
	String uri();

	/**
	 * The tag set.
	 */
	Set<String> tags();

	/**
	 * Adds a given event handler for a given event.
	 * <p>
	 * The allowed types for <code>T</code> are Java types corresponding to JSON types.
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>JSON</th>
	 * <th>Java</th>
	 * </tr>
	 * </thead>
	 * <tbody>
	 * <tr>
	 * <td>Number</td>
	 * <td>{@link Integer} or {@link Double}</td>
	 * </tr>
	 * <tr>
	 * <td>String</td>
	 * <td>{@link String}</td>
	 * </tr>
	 * <tr>
	 * <td>Boolean</td>
	 * <td>{@link Boolean}</td>
	 * </tr>
	 * <tr>
	 * <td>Array</td>
	 * <td>{@link List}, <code>List&lt;T&gt;</code> in generic</td>
	 * </tr>
	 * <tr>
	 * <td>Object</td>
	 * <td>{@link Map}, <code>Map&lt;String, T&gt;</code> in generic</td>
	 * </tr>
	 * <tr>
	 * <td>null</td>
	 * <td>null, {@link Void} for convenience</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * 
	 * If the counterpart sends an event with callback, <code>T</code> should be {@link Reply}.
	 */
	<T> Socket on(String event, Action<T> action);

	/**
	 * Removes a given added event handler for a given event.
	 */
	<T> Socket off(String event, Action<T> action);

	/**
	 * Sends a given event without data.
	 */
	Socket send(String event);

	/**
	 * Sends a given event with data.
	 */
	Socket send(String event, Object data);

	/**
	 * Sends a given event with data registering callback.
	 * <p>
	 * For the allowed types for <code>T</code>, see {@link Socket#on(String, Action)}. 
	 */
	<T> Socket send(String event, Object data, Action<T> reply);

	/**
	 * Closes the session.
	 */
	Socket close();
	
	/**
	 * Adds a close event handler.
	 */
	Socket closeAction(Action<Void> action);

	/**
	 * Interface to deal with reply.
	 * <p>
	 * For the allowed types for <code>T</code>, see {@link Socket#on(String, Action)}. 
	 * 
	 * @author Donghwan Kim
	 */
	interface Reply<T> {

		/**
		 * The original data.
		 */
		T data();

		/**
		 * Replies with success.
		 */
		void done();

		/**
		 * Replies with success attaching data.
		 */
		void done(Object data);

		/**
		 * Replies with failure.
		 */
		void fail();

		/**
		 * Replies with failure attaching data.
		 */
		void fail(Object error);

	}

}
