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
package com.github.flowersinthesand.portal.play;

import play.api.mvc.Codec;
import play.core.j.JavaResults;
import play.libs.F;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Results.Chunks;
import play.mvc.WebSocket;

import com.github.flowersinthesand.portal.Bean;
import com.github.flowersinthesand.portal.support.AbstractSocketFactory;

@Bean("socketFactory")
public class PlaySocketFactory extends AbstractSocketFactory {

	WebSocket<String> openWs(Request request) {
		WsSocket socket = new WsSocket(request);
		sockets.put(socket.id(), socket);

		return socket.webSocket;
	}

	Chunks<String> openHttp(Request request, Response response) {
		String when = request.queryString().get("when")[0];
		String id = request.queryString().get("id")[0];
		String transport = request.queryString().get("transport")[0];

		HttpSocket socket = null;
		if (transport.equals("sse") || transport.startsWith("stream")) {
			socket = new StreamSocket(request, response);
			sockets.put(id, socket);
		} else if (transport.startsWith("longpoll")) {
			if (when.equals("open")) {
				socket = new LongPollSocket(request, response);
				sockets.put(id, socket);
			} else if (when.equals("poll")) {
				socket = (LongPollSocket) sockets.get(id);
				((LongPollSocket) socket).refresh(request, response, false);
			}
		}

		return socket.chunks;
	}

	class WsSocket extends AbstractSocket {

		private WebSocket<String> webSocket;
		private WebSocket.Out<String> out;

		public WsSocket(Request request) {
			this.params = params(request.queryString());
			this.webSocket = new WebSocket<String>() {
				@Override
				public void onReady(WebSocket.In<String> in, Out<String> oout) {
					out = oout;
					in.onClose(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});
					in.onMessage(new F.Callback<String>() {
						@Override
						public void invoke(String message) throws Throwable {
							fire(message);
						}
					});
					onOpen();
				}
			};
		}

		@Override
		protected void transmit(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
		}

	}

	abstract class HttpSocket extends AbstractSocket {

		protected Chunks<String> chunks;
		protected Chunks.Out<String> out;

	}

	class StreamSocket extends HttpSocket {

		public StreamSocket(Request request, Response response) {
			this.params = params(request.queryString());
			this.isAndroid = isAndroid(request.getHeader("user-agent"));
			this.chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					out.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							onClose();
						}
					});

					out.write(padding2K);
					if (isAndroid) {
						out.write(padding2K);
					}
					out.write("\n");
					onOpen();
				}
			};
			response.setContentType(("text/" + ("sse".equals(param("transport")) ? "event-stream" : "plain") + "; charset=utf-8"));
		}

		@Override
		protected void transmit(String it) {
			out.write(it);
		}

		@Override
		protected void disconnect() {
			out.close();
		}

	}

	class LongPollSocket extends HttpSocket {
		
		public LongPollSocket(Request request, Response response) {
			this.params = params(request.queryString());
			refresh(request, response, true);
		}

		private void refresh(final Request request, Response response, final boolean open) {
			this.chunks = new Chunks<String>(JavaResults.writeString(Codec.utf_8()), JavaResults.contentTypeOfString((Codec.utf_8()))) {
				@Override
				public void onReady(Chunks.Out<String> oout) {
					out = oout;
					out.onDisconnected(new F.Callback0() {
						@Override
						public void invoke() throws Throwable {
							if (!open && out != null) {
								onClose();
							}
						}
					}); 
					
					if (open) {
						out.close();
						onOpen();
					} else {
						retrieveCache(request.queryString().get("lastEventIds")[0]);
					}
				}
			};
			response.setContentType(("text/" + ("longpolljsonp".equals(param("transport")) ? "javascript" : "plain") + "; charset=utf-8"));
		}

		@Override
		protected void transmit(String it) {
			if (out != null) {
				out.write(it);
				out.close();
				out = null;
			}
		}

		@Override
		protected void disconnect() {
			if (out != null) {
				out.close();
			}
		}

	}

}
