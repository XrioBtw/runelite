/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.runelite.client.account.AccountSession;
import net.runelite.http.api.RuneliteAPI;
import net.runelite.http.api.ws.messages.Handshake;
import net.runelite.http.api.ws.messages.Ping;
import net.runelite.http.api.ws.WebsocketGsonFactory;
import net.runelite.http.api.ws.WebsocketMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSClient extends WebSocketListener implements AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(WSClient.class);

	private static final Duration PING_TIME = Duration.ofSeconds(30);

	private static final Gson gson = WebsocketGsonFactory.build();
	private static final EventBus eventBus = RuneLite.getRunelite().getEventBus();
	private static final ScheduledExecutorService executor = RuneLite.getRunelite().getExecutor();

	private final OkHttpClient client = new OkHttpClient();

	private final AccountSession session;
	private WebSocket webSocket;
	private final ScheduledFuture pingFuture;

	public WSClient(AccountSession session)
	{
		this.session = session;
		this.pingFuture = executor.scheduleWithFixedDelay(this::ping, PING_TIME.getSeconds(), PING_TIME.getSeconds(), TimeUnit.SECONDS);
	}

	public AccountSession getSession()
	{
		return session;
	}

	public void connect()
	{
		Request request = new Request.Builder()
			.url(RuneliteAPI.getWsEndpoint())
			.build();

		webSocket = client.newWebSocket(request, this);

		Handshake handshake = new Handshake();
		handshake.setSession(session.getUuid());
		send(handshake);
	}

	public void ping()
	{
		Ping ping = new Ping();
		ping.setTime(Instant.now());
		send(ping);
	}

	public void send(WebsocketMessage message)
	{
		if (webSocket == null)
		{
			logger.debug("Reconnecting to server");

			connect();
		}

		String json = gson.toJson(message, WebsocketMessage.class);
		webSocket.send(json);

		logger.debug("Sent: {}", json);
	}

	@Override
	public void close()
	{
		if (pingFuture != null)
		{
			pingFuture.cancel(true);
		}

		if (webSocket != null)
		{
			webSocket.close(1000, null);
		}
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response)
	{
		logger.info("Websocket {} opened", webSocket);
	}

	@Override
	public void onMessage(WebSocket webSocket, String text)
	{
		WebsocketMessage message = gson.fromJson(text, WebsocketMessage.class);
		logger.debug("Got message: {}", message);

		eventBus.post(message);
	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason)
	{
		logger.info("Websocket {} closed: {}/{}", webSocket, code, reason);
		this.webSocket = null;
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response)
	{
		logger.warn("Error in websocket", t);
		this.webSocket = null;
	}
}
