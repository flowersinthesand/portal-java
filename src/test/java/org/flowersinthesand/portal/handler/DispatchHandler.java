/*
 * Copyright 2012 Donghwan Kim
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
package org.flowersinthesand.portal.handler;

import org.flowersinthesand.portal.Data;
import org.flowersinthesand.portal.Fn;
import org.flowersinthesand.portal.Handler;
import org.flowersinthesand.portal.On;
import org.flowersinthesand.portal.Reply;
import org.flowersinthesand.portal.Socket;

@Handler("/dispatch")
public class DispatchHandler {

	public Object[] args;

	@On("load")
	public void onLoad() {}
	
	@On("socket")
	public void onSocket(Socket socket) {
		args = new Object[] { socket };
	}
	
	@On("data")
	public void onData(@Data Object data) {
		args = new Object[] { data };
	}

	@On("repli")
	public void onRepli(@Reply Fn.Callback reply) {
		args = new Object[] { reply };
		reply.call();
	}

	@On("repli-data")
	public void onRepliData(@Reply Fn.Callback1<Object> reply, @Data Object data) {
		args = new Object[] { reply, data };
		reply.call(data);
	}

	@On("socket-data-repli")
	public void onSocketDataRepli(Socket socket, @Data Object data, @Reply Fn.Callback1<Object> reply) {
		args = new Object[] { socket, data, reply };
		reply.call(data);
	}
	
}
