/*
 * Copyright (c) 2017, Seth <Sethtroll3@gmail.com>
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
package net.runelite.client.plugins.timers;

import com.google.common.eventbus.Subscribe;
import net.runelite.api.ChatMessageType;
import net.runelite.client.RuneLite;
import net.runelite.client.events.ChatMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import static net.runelite.client.plugins.timers.GameTimer.*;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@PluginDescriptor(
	name = "Timers plugin"
)
public class Timers extends Plugin
{
	private final TimersConfig config = RuneLite.getRunelite().getConfigManager().getConfig(TimersConfig.class);

	private final InfoBoxManager infoBoxManager = RuneLite.getRunelite().getInfoBoxManager();

	@Override
	protected void startUp() throws Exception
	{
	}

	@Override
	protected void shutDown() throws Exception
	{
	}

	public TimersConfig getConfig()
	{
		return config;
	}

	@Subscribe
	public void updateConfig(ConfigChanged event)
	{
		if (!config.showStamina())
		{
			removeGameTimer(STAMINA);
		}

		if (!config.showAntiFire())
		{
			removeGameTimer(ANTIFIRE);
		}

		if (!config.showExAntiFire())
		{
			removeGameTimer(EXANTIFIRE);
		}

		if (!config.showOverload())
		{
			removeGameTimer(OVERLOAD);
		}

		if (!config.showCannon())
		{
			removeGameTimer(CANNON);
		}

		if (!config.showMagicImbue())
		{
			removeGameTimer(MAGICIMBUE);
		}

		if (!config.showAntiVenom())
		{
			removeGameTimer(SUPERANTIVENOM);
		}
		if (!config.showTeleblock())
		{
			removeGameTimer(FULLTB);
			removeGameTimer(HALFTB);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.FILTERED && event.getType() != ChatMessageType.SERVER)
		{
			return;
		}

		if (event.getMessage().equals("You drink some of your stamina potion.") && config.showStamina())
		{
			createGameTimer(STAMINA);
		}

		if (event.getMessage().equals("<col=8f4808>Your stamina potion has expired.</col>"))
		{
			removeGameTimer(STAMINA);
		}

		if (event.getMessage().equals("You drink some of your antifire potion.") && config.showAntiFire())
		{
			createGameTimer(ANTIFIRE);
		}

		if (event.getMessage().equals("You drink some of your extended antifire potion.") && config.showExAntiFire())
		{
			createGameTimer(EXANTIFIRE);
		}

		if (event.getMessage().equals("<col=7f007f>Your antifire potion has expired.</col>"))
		{
			//they have the same expired message
			removeGameTimer(ANTIFIRE);
			removeGameTimer(EXANTIFIRE);
		}

		if (event.getMessage().contains("You drink some of your overload potion") && config.showOverload())
		{
			createGameTimer(OVERLOAD);
		}

		if ((event.getMessage().equals("You add the furnace.") || event.getMessage().contains("You repair your cannon, restoring it to working order.")) && config.showCannon())
		{
			createGameTimer(CANNON);
		}

		if (event.getMessage().equals("You pick up the cannon. It's really heavy."))
		{
			removeGameTimer(CANNON);
		}

		if (event.getMessage().contains("You drink some of your super antivenom potion") && config.showAntiVenom())
		{
			createGameTimer(SUPERANTIVENOM);
		}

		if (event.getMessage().equals("You are charged to combine runes!") && config.showMagicImbue())
		{
			createGameTimer(MAGICIMBUE);
		}

		if (event.getMessage().equals("Your Magic Imbue charge has ended."))
		{
			removeGameTimer(MAGICIMBUE);
		}

		if (event.getMessage().equals("<col=4f006f>A teleblock spell has been cast on you. It will expire in 5 minutes, 0 seconds.</col>") && config.showTeleblock())
		{
			createGameTimer(FULLTB);
		}

		if (event.getMessage().equals("<col=4f006f>A teleblock spell has been cast on you. It will expire in 2 minutes, 30 seconds.</col>") && config.showTeleblock())
		{
			createGameTimer(HALFTB);
		}
	}

	public void createGameTimer(GameTimer timer)
	{
		removeGameTimer(timer);

		TimerTimer t = new TimerTimer(timer);
		infoBoxManager.addInfoBox(t);
	}

	public void removeGameTimer(GameTimer timer)
	{
		infoBoxManager.removeIf(t -> t instanceof TimerTimer && ((TimerTimer) t).getTimer() == timer);
	}
}
