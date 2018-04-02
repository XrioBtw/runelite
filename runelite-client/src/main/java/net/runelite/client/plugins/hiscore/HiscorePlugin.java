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
package net.runelite.client.plugins.hiscore;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginToolbar;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "HiScore",
	loadWhenOutdated = true
)
public class HiscorePlugin extends Plugin
{
	private static final String LOOKUP = "Lookup";

	@Inject
	private PluginToolbar pluginToolbar;

	@Nullable
	@Inject
	private Client client;

	@Inject
	private MenuManager menuManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private HiscoreConfig config;

	private NavigationButton navButton;
	private HiscorePanel hiscorePanel;

	@Provides
	HiscoreConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HiscoreConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		hiscorePanel = injector.getInstance(HiscorePanel.class);

		BufferedImage icon;
		synchronized (ImageIO.class)
		{
			icon = ImageIO.read(getClass().getResourceAsStream("hiscore.gif"));
		}

		navButton = NavigationButton.builder()
			.name("Hiscore")
			.icon(icon)
			.panel(hiscorePanel)
			.build();

		pluginToolbar.addNavigation(navButton);

		if (config.playerOption())
		{
			menuManager.addPlayerMenuItem(LOOKUP);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		pluginToolbar.removeNavigation(navButton);
		menuManager.removePlayerMenuItem(LOOKUP);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("hiscore"))
		{
			menuManager.removePlayerMenuItem(LOOKUP);

			if (config.playerOption())
			{
				menuManager.addPlayerMenuItem(LOOKUP);
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption().equals(LOOKUP))
		{
			lookup(Text.removeLevels(Text.removeTags(event.getMenuTarget())));
		}
	}
	
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		String option = event.getOption();

		if (config.playerOption() &&
			(option.equals("Message") ||
			option.equals("Delete") ||
			option.equals("Add ignore") ||
			option.equals("Remove friend") ||
			option.equals("Remove ignore")))
		{
			addMenuEntry(option, LOOKUP, true);
		}
	}
	
	private void lookup(String target)
	{
		executor.execute(() ->
		{
			try
			{
				SwingUtilities.invokeAndWait(() ->
				{
					if (!navButton.isSelected())
					{
						navButton.getOnSelect().run();
					}
				});
			}
			catch (InterruptedException | InvocationTargetException e)
			{
				throw new RuntimeException(e);
			}

			hiscorePanel.lookup(target);
		});
	}
	
	private void addMenuEntry(String refOption, String newOption, boolean after)
	{
		if (client == null)
		{
			return;
		}
		
		MenuEntry[] entries = client.getMenuEntries();

		List<MenuEntry> list = new ArrayList<>(entries.length + 1);

		for (MenuEntry entry : entries)
		{
			if (entry.getOption().equals(refOption))
			{
				MenuEntry newEntry = new MenuEntry();

				newEntry.setOption(newOption);
				newEntry.setTarget(entry.getTarget());
				newEntry.setType(MenuAction.RUNELITE.getId());
				newEntry.setIdentifier(entry.getIdentifier());
				newEntry.setParam0(entry.getParam0());
				newEntry.setParam1(entry.getParam1());

				if (after)
				{
					list.add(newEntry);
					list.add(entry);
				}
				else
				{
					list.add(entry);
					list.add(newEntry);
				}
			}
			else
			{
				if (!entry.getOption().equals(newOption))
				{
					list.add(entry);
				}
			}
		}

		MenuEntry[] newEntries = list.toArray(new MenuEntry[list.size()]);
		client.setMenuEntries(newEntries);
	}
}
