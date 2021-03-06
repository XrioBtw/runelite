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
package net.runelite.http.api.examine;

import java.io.IOException;
import net.runelite.http.api.RuneliteAPI;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamineClient
{
	private static final Logger logger = LoggerFactory.getLogger(ExamineClient.class);

	private static final MediaType TEXT = MediaType.parse("text");

	public void submitObject(int id, String text) throws IOException
	{
		submit("object", id, text);
	}

	public void submitNpc(int id, String text) throws IOException
	{
		submit("npc", id, text);
	}

	public void submitItem(int id, String text) throws IOException
	{
		submit("item", id, text);
	}

	private void submit(String type, int id, String text) throws IOException
	{
		HttpUrl url = RuneliteAPI.getApiBase().newBuilder()
			.addPathSegment("examine")
			.addPathSegment(type)
			.addPathSegment(Integer.toString(id))
			.build();

		logger.debug("Built URI: {}", url);

		Request request = new Request.Builder()
			.url(url)
			.post(RequestBody.create(TEXT, text))
			.build();

		try (Response response = RuneliteAPI.CLIENT.newCall(request).execute())
		{
			logger.debug("Submitted examine info for {} {}: {}",
				type, id, text);
		}
	}
}
