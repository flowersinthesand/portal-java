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

import java.io.Serializable;

/**
 * {@code Sentence} is a series of predicates that a group of socket have to
 * follow. It makes easy to write one-liner action and uses internally built
 * actions implementing {@link Serializable} that is typically needed in cluster
 * environments. Use of {@code Sentence} is preferred to that of action if the
 * goal is the same.
 * 
 * @author Donghwan Kim
 */
public class Sentence implements AbstractSocket<Sentence>{
	
	private final Action<Action<Socket>> serverAction;

	Sentence(Action<Action<Socket>> serverAction) {
		this.serverAction = serverAction;
	}

	@Override
	public Sentence send(String event) {
		return send(event, null);
	}

	@Override
	public Sentence send(String event, Object data) {
		execute(new SendAction(event, data));
		return this;
	}

	@Override
	public Sentence close() {
		execute(new CloseAction());
		return this;
	}

	private void execute(Action<Socket> action) {
		serverAction.on(action);
	}

	static interface SerializableAction<T> extends Action<T>, Serializable {}

	static class SendAction implements SerializableAction<Socket> {
		private static final long serialVersionUID = 2178442626501531717L;
		final String event;
		final Object data;

		SendAction(String event, Object data) {
			this.event = event;
			this.data = data;
		}

		@Override
		public void on(Socket socket) {
			socket.send(event, data);
		}
	}

	static class CloseAction implements SerializableAction<Socket> {
		private static final long serialVersionUID = 8154281469036373698L;

		@Override
		public void on(Socket socket) {
			socket.close();
		}
	}

}
