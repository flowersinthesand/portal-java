package io.github.flowersinthesand.portal.testsuite;

import io.github.flowersinthesand.portal.DefaultServer;
import io.github.flowersinthesand.portal.Server;
import io.github.flowersinthesand.portal.Socket;
import io.github.flowersinthesand.portal.Socket.Reply;
import io.github.flowersinthesand.wes.Action;
import io.github.flowersinthesand.wes.VoidAction;
import io.github.flowersinthesand.wes.atmosphere.AtmosphereBridge;

import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Bootstrap to start the test suite server.
 * <p>
 * To start the server on http://localhost:8080/,
 * <p>
 * {@code $ mvn jetty:run-war}
 * <p>
 * Then connect to http://localhost:8080/test/ using any test suite client.
 * Also, this web server serves up test suite client running on browser written
 * in JavaScript that is used to develop portal.js. Open http://localhost:8080
 * in your browser to run the test suite in same-origin.
 * <p>
 * To run the test suite in cross-origin, start another server on
 * http://localhost:8090/,
 * <p>
 * {@code $ mvn jetty:run-war -Djetty.port=8090}
 * <p>
 * Then open http://localhost:8090 in your browser. Test suite on 8090 will
 * connect to 8080, cross-origin.
 * 
 * @author Donghwan Kim
 */
@WebListener
public class ServerBootstrap implements ServletContextListener {
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		// Create a portal server
		Server server = new DefaultServer();
		
		// This action is equivalent to socket handler of server.js in the portal repo
		// https://github.com/flowersinthesand/portal/blob/1.1.1/test/server.js#L593-L611
		server.socketAction(new Action<Socket>() {
			@Override
			public void on(final Socket socket) {
				socket.on("echo", new Action<Object>() {
					@Override
					public void on(Object data) {
						socket.send("echo", data);
					}
				})
				.on("disconnect", new VoidAction() {
					@Override
					public void on() {
						new Timer(true).schedule(new TimerTask() {
							@Override
							public void run() {
								socket.close();
							}
						}, 100);
					}
				})
				.on("reply-by-server", new Action<Reply<Boolean>>() {
					@Override
					public void on(Reply<Boolean> reply) {
						if (reply.data()) {
							reply.done(reply.data());
						} else {
							reply.fail(reply.data());
						}
					}
				})
				.on("reply-by-client", new VoidAction() {
					@Override
					public void on() {
						socket.send("reply-by-client", 1, new Action<String>() {
							@Override
							public void on(String type) {
								socket.send(type);
							}
						});
					}
				});
			}
		});
		
		// Install portal server by specifying path and attaching its wes actions to wes bridge
		new AtmosphereBridge(event.getServletContext(), "/test").httpAction(server.httpAction()).websocketAction(server.websocketAction());
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {}

}
