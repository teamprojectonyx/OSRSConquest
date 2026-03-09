package net.osrsconquest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map;

@Slf4j
public class OSRSConquestPanel extends PluginPanel
{
	private final ConquestApiClient apiClient;
	private final OSRSConquestConfig config;

	private static final String[] EVENT_TYPES = {
		"JOIN", "LEAVE", "KICK", "LEVEL_UP", "DROP", "PET",
		"QUEST", "COLLECTION_LOG", "PERSONAL_BEST", "RANK_CHANGE",
		"COMBAT_ACHIEVEMENT", "DIARY"
	};

	private static final String[] EVENT_TYPE_LABELS = {
		"Join", "Leave", "Kick", "Level Up", "Drop", "Pet",
		"Quest", "Collection Log", "Personal Best", "Rank Change",
		"Combat Achievement", "Diary"
	};

	private JPanel eventsContent;
	private JPanel membersContent;
	private JPanel leaderboardContent;
	private JPanel statsContent;
	private JPanel discordContent;
	private JTabbedPane tabbedPane;

	private JTextField webhookUrlField;
	private final Map<String, JCheckBox> eventTypeCheckboxes = new LinkedHashMap<>();
	private JLabel discordStatusLabel;

	private String leaderboardPeriod = "week";
	private String leaderboardType = "xp";
	private Runnable syncMembersCallback;

	@Inject
	public OSRSConquestPanel(ConquestApiClient apiClient, OSRSConquestConfig config)
	{
		this.apiClient = apiClient;
		this.config = config;
		buildPanel();
	}

	private void buildPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header
		JLabel title = new JLabel("OSRS Conquest");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		title.setForeground(Color.WHITE);
		title.setBorder(new EmptyBorder(8, 8, 8, 8));
		add(title, BorderLayout.NORTH);

		// Tabbed pane
		tabbedPane = new JTabbedPane();
		tabbedPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

		eventsContent = createContentPanel();
		membersContent = createContentPanel();
		leaderboardContent = createContentPanel();
		statsContent = createContentPanel();
		discordContent = createContentPanel();

		tabbedPane.addTab("Events", createScrollTab(eventsContent));
		tabbedPane.addTab("Members", createScrollTab(membersContent));
		tabbedPane.addTab("Board", createScrollTab(leaderboardContent));
		tabbedPane.addTab("Stats", createScrollTab(statsContent));
		tabbedPane.addTab("Discord", createScrollTab(discordContent));

		tabbedPane.addChangeListener(e -> onTabChanged());

		add(tabbedPane, BorderLayout.CENTER);

		// Refresh button
		JButton refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(e -> refreshCurrentTab());
		refreshBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		refreshBtn.setForeground(Color.WHITE);
		add(refreshBtn, BorderLayout.SOUTH);

		showPlaceholder(eventsContent, "Loading events...");
		showPlaceholder(membersContent, "Loading members...");
		showPlaceholder(leaderboardContent, "Loading leaderboard...");
		showPlaceholder(statsContent, "Loading stats...");
		buildDiscordTab();
	}

	/**
	 * Creates a content panel using BoxLayout Y_AXIS for stacking rows.
	 */
	private JPanel createContentPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(2, 0, 2, 0));
		return panel;
	}

	/**
	 * Wraps a content panel in a scroll pane using the BorderLayout.NORTH
	 * wrapper pattern. This ensures content is always top-aligned and scrollable.
	 * Without the wrapper, BoxLayout distributes extra space evenly among children,
	 * causing them to spread across the full panel height.
	 */
	private JScrollPane createScrollTab(JPanel contentPanel)
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(contentPanel, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		return scroll;
	}

	private void onTabChanged()
	{
		refreshCurrentTab();
	}

	public void refreshCurrentTab()
	{
		if (!config.dataConsent())
		{
			showConsentPrompt();
			return;
		}

		if (config.apiKey().isEmpty())
		{
			showPlaceholder(eventsContent, "Not registered. Join a clan in-game to auto-register.");
			return;
		}

		int tab = tabbedPane.getSelectedIndex();
		switch (tab)
		{
			case 0:
				refreshEvents();
				break;
			case 1:
				refreshMembers();
				break;
			case 2:
				refreshLeaderboard();
				break;
			case 3:
				refreshStats();
				break;
			case 4:
				refreshDiscord();
				break;
		}
	}

	// ─── Events Tab ───

	private void refreshEvents()
	{
		showPlaceholder(eventsContent, "Loading events...");
		apiClient.fetchRecentEvents(events ->
			SwingUtilities.invokeLater(() -> renderEvents(events))
		);
	}

	private void renderEvents(List<Map<String, Object>> events)
	{
		eventsContent.removeAll();

		if (events.isEmpty())
		{
			showPlaceholder(eventsContent, "No events yet.");
			return;
		}

		for (Map<String, Object> event : events)
		{
			eventsContent.add(createEventCard(event));
			eventsContent.add(Box.createVerticalStrut(4));
		}

		eventsContent.revalidate();
		eventsContent.repaint();
	}

	private JPanel createEventCard(Map<String, Object> event)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(8, 6, 8, 6));

		String type = stringVal(event, "type");
		String actor = stringVal(event, "actor");
		String detail = stringVal(event, "detail");

		// Type label
		JLabel typeLabel = new JLabel(formatEventType(type));
		typeLabel.setForeground(getEventColor(type));
		typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD, 13f));
		card.add(typeLabel, BorderLayout.NORTH);

		// Actor + detail — JTextArea wraps text naturally at the component boundary
		String text = actor != null ? actor : "";
		if (detail != null && !detail.isEmpty())
		{
			text += (text.isEmpty() ? "" : " \u2014 ") + detail;
		}
		if (!text.isEmpty())
		{
			JTextArea detailArea = new JTextArea(text);
			detailArea.setForeground(Color.LIGHT_GRAY);
			detailArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			detailArea.setFont(detailArea.getFont().deriveFont(12f));
			detailArea.setLineWrap(true);
			detailArea.setWrapStyleWord(true);
			detailArea.setEditable(false);
			detailArea.setFocusable(false);
			detailArea.setBorder(new EmptyBorder(2, 0, 0, 0));
			card.add(detailArea, BorderLayout.CENTER);
		}

		return card;
	}

	private String formatEventType(String type)
	{
		if (type == null) return "?";
		switch (type)
		{
			case "JOIN": return "\u2795 JOIN";
			case "LEAVE": return "\u2796 LEAVE";
			case "KICK": return "\u274c KICK";
			case "LEVEL_UP": return "\u2b06 LEVEL UP";
			case "DROP": return "\u2728 DROP";
			case "PET": return "\u2764 PET";
			case "QUEST": return "\u2705 QUEST";
			case "COLLECTION_LOG": return "\ud83d\udcda COLL. LOG";
			case "PERSONAL_BEST": return "\u23f1 PB";
			case "RANK_CHANGE": return "\u2694 RANK";
			case "COMBAT_ACHIEVEMENT": return "\u2694 COMBAT";
			case "DIARY": return "\ud83d\udcd6 DIARY";
			default: return type;
		}
	}

	private Color getEventColor(String type)
	{
		if (type == null) return Color.WHITE;
		switch (type)
		{
			case "JOIN": return new Color(0, 200, 83);
			case "LEAVE": return new Color(255, 152, 0);
			case "KICK": return new Color(244, 67, 54);
			case "LEVEL_UP": return new Color(33, 150, 243);
			case "DROP": return new Color(156, 39, 176);
			case "PET": return new Color(233, 30, 99);
			case "QUEST": return new Color(76, 175, 80);
			default: return Color.WHITE;
		}
	}

	// ─── Members Tab ───

	public void setSyncMembersCallback(Runnable callback)
	{
		this.syncMembersCallback = callback;
	}

	private void refreshMembers()
	{
		showPlaceholder(membersContent, "Loading members...");
		apiClient.fetchMembers(members ->
			SwingUtilities.invokeLater(() -> renderMembers(members))
		);
	}

	private void renderMembers(List<Map<String, Object>> members)
	{
		membersContent.removeAll();

		// Sync button — user-initiated member sync
		if (syncMembersCallback != null)
		{
			JButton syncBtn = new JButton("Sync Clan Members");
			syncBtn.setToolTipText("Update the member list on the dashboard with current in-game clan data");
			syncBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			syncBtn.setForeground(Color.WHITE);
			syncBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
			syncBtn.addActionListener(e ->
			{
				syncBtn.setEnabled(false);
				syncBtn.setText("Syncing...");
				syncMembersCallback.run();
				javax.swing.Timer timer = new javax.swing.Timer(3000, ev ->
				{
					syncBtn.setEnabled(true);
					syncBtn.setText("Sync Clan Members");
					refreshMembers();
				});
				timer.setRepeats(false);
				timer.start();
			});
			membersContent.add(syncBtn);
			membersContent.add(Box.createVerticalStrut(6));
		}

		if (members.isEmpty())
		{
			showPlaceholder(membersContent, "No members found.");
			return;
		}

		for (Map<String, Object> member : members)
		{
			JPanel row = new JPanel(new BorderLayout());
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setBorder(new EmptyBorder(5, 6, 5, 6));

			String rsn = stringVal(member, "rsn");
			boolean online = Boolean.TRUE.equals(member.get("online"));

			JLabel nameLabel = new JLabel(rsn);
			nameLabel.setForeground(online ? new Color(0, 200, 83) : Color.LIGHT_GRAY);
			nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
			row.add(nameLabel, BorderLayout.WEST);

			String rank = stringVal(member, "rank");
			if (rank != null)
			{
				JLabel rankLabel = new JLabel(rank);
				rankLabel.setForeground(Color.GRAY);
				rankLabel.setFont(rankLabel.getFont().deriveFont(11f));
				row.add(rankLabel, BorderLayout.EAST);
			}

			membersContent.add(row);
			membersContent.add(Box.createVerticalStrut(2));
		}

		membersContent.revalidate();
		membersContent.repaint();
	}

	// ─── Board (Leaderboard) Tab ───

	private void refreshLeaderboard()
	{
		leaderboardContent.removeAll();

		// Toggle buttons row
		JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
		togglePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		for (String period : new String[]{"day", "week", "month"})
		{
			JButton btn = new JButton(period.substring(0, 1).toUpperCase() + period.substring(1));
			btn.setBackground(period.equals(leaderboardPeriod) ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
			btn.setForeground(Color.WHITE);
			btn.setFont(btn.getFont().deriveFont(10f));
			btn.addActionListener(e -> {
				leaderboardPeriod = period;
				refreshLeaderboard();
			});
			togglePanel.add(btn);
		}

		JButton xpBtn = new JButton("XP");
		xpBtn.setBackground("xp".equals(leaderboardType) ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
		xpBtn.setForeground(Color.WHITE);
		xpBtn.addActionListener(e -> { leaderboardType = "xp"; refreshLeaderboard(); });
		togglePanel.add(xpBtn);

		JButton timeBtn = new JButton("Time");
		timeBtn.setBackground("playtime".equals(leaderboardType) ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
		timeBtn.setForeground(Color.WHITE);
		timeBtn.addActionListener(e -> { leaderboardType = "playtime"; refreshLeaderboard(); });
		togglePanel.add(timeBtn);

		leaderboardContent.add(togglePanel);

		// Loading indicator
		JLabel loading = new JLabel("Loading...");
		loading.setForeground(Color.GRAY);
		loading.setBorder(new EmptyBorder(8, 4, 0, 0));
		loading.setAlignmentX(Component.LEFT_ALIGNMENT);
		leaderboardContent.add(loading);

		leaderboardContent.revalidate();
		leaderboardContent.repaint();

		apiClient.fetchLeaderboard(leaderboardType, leaderboardPeriod, entries ->
			SwingUtilities.invokeLater(() -> renderLeaderboard(entries))
		);
	}

	private void renderLeaderboard(List<Map<String, Object>> entries)
	{
		// Keep the toggle panel (first component), remove the rest
		while (leaderboardContent.getComponentCount() > 1)
		{
			leaderboardContent.remove(leaderboardContent.getComponentCount() - 1);
		}

		if (entries.isEmpty())
		{
			JLabel empty = new JLabel("No data for this period.");
			empty.setForeground(Color.GRAY);
			empty.setBorder(new EmptyBorder(8, 4, 0, 0));
			empty.setAlignmentX(Component.LEFT_ALIGNMENT);
			leaderboardContent.add(empty);
		}
		else
		{
			int rank = 1;
			for (Map<String, Object> entry : entries)
			{
				JPanel row = new JPanel(new BorderLayout());
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				row.setBorder(new EmptyBorder(5, 6, 5, 6));

				String rsn = stringVal(entry, "rsn");
				JLabel nameLabel = new JLabel("#" + rank + "  " + rsn);
				nameLabel.setForeground(rank <= 3 ? new Color(255, 215, 0) : Color.LIGHT_GRAY);
				nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
				row.add(nameLabel, BorderLayout.WEST);

				String value;
				if ("playtime".equals(leaderboardType))
				{
					double seconds = doubleVal(entry, "playtimeSeconds");
					int hours = (int) (seconds / 3600);
					int mins = (int) ((seconds % 3600) / 60);
					value = hours + "h " + mins + "m";
				}
				else
				{
					double xpGain = doubleVal(entry, "xpGain");
					value = formatXp(xpGain);
				}

				JLabel valLabel = new JLabel(value);
				valLabel.setForeground(Color.WHITE);
				valLabel.setFont(valLabel.getFont().deriveFont(Font.BOLD, 12f));
				row.add(valLabel, BorderLayout.EAST);

				leaderboardContent.add(row);
				leaderboardContent.add(Box.createVerticalStrut(2));
				rank++;
			}
		}

		leaderboardContent.revalidate();
		leaderboardContent.repaint();
	}

	// ─── Stats Tab ───

	private void refreshStats()
	{
		showPlaceholder(statsContent, "Loading stats...");
		apiClient.fetchClanSummary(summary ->
			SwingUtilities.invokeLater(() -> renderStats(summary))
		);
	}

	private void renderStats(Map<String, Object> summary)
	{
		statsContent.removeAll();

		addStatRow(statsContent, "Clan", stringVal(summary, "name"));
		addStatRow(statsContent, "Total Members", String.valueOf((int) doubleVal(summary, "totalMembers")));
		addStatRow(statsContent, "Active Members", String.valueOf((int) doubleVal(summary, "activeMembers")));
		addStatRow(statsContent, "Online Now", String.valueOf((int) doubleVal(summary, "onlineNow")));
		addStatRow(statsContent, "Events (24h)", String.valueOf((int) doubleVal(summary, "eventsLast24h")));

		statsContent.revalidate();
		statsContent.repaint();
	}

	private void addStatRow(JPanel panel, String label, String value)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(8, 6, 8, 6));

		JLabel labelComp = new JLabel(label);
		labelComp.setForeground(Color.GRAY);
		labelComp.setFont(labelComp.getFont().deriveFont(12f));
		row.add(labelComp, BorderLayout.WEST);

		JLabel valueComp = new JLabel(value != null ? value : "\u2014");
		valueComp.setForeground(Color.WHITE);
		valueComp.setFont(valueComp.getFont().deriveFont(Font.BOLD, 13f));
		row.add(valueComp, BorderLayout.EAST);

		panel.add(row);
		panel.add(Box.createVerticalStrut(3));
	}

	// ─── Discord Tab ───

	private void buildDiscordTab()
	{
		discordContent.removeAll();

		// Open Dashboard button
		JButton dashboardBtn = new JButton("Open Dashboard");
		dashboardBtn.setBackground(ColorScheme.BRAND_ORANGE);
		dashboardBtn.setForeground(Color.WHITE);
		dashboardBtn.setFont(dashboardBtn.getFont().deriveFont(Font.BOLD, 12f));
		dashboardBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		dashboardBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		dashboardBtn.addActionListener(e -> openDashboard(dashboardBtn));
		discordContent.add(dashboardBtn);
		discordContent.add(Box.createVerticalStrut(8));

		// Status label
		discordStatusLabel = new JLabel("Loading...");
		discordStatusLabel.setForeground(Color.GRAY);
		discordStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		discordStatusLabel.setBorder(new EmptyBorder(4, 6, 6, 6));
		discordContent.add(discordStatusLabel);

		// Webhook URL label
		JLabel urlLabel = new JLabel("Webhook URL");
		urlLabel.setForeground(Color.LIGHT_GRAY);
		urlLabel.setFont(urlLabel.getFont().deriveFont(Font.BOLD, 12f));
		urlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		urlLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
		discordContent.add(urlLabel);

		// Webhook URL field
		webhookUrlField = new JTextField();
		webhookUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		webhookUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);
		discordContent.add(webhookUrlField);
		discordContent.add(Box.createVerticalStrut(8));

		// Event types header
		JLabel typesLabel = new JLabel("Event Types");
		typesLabel.setForeground(Color.LIGHT_GRAY);
		typesLabel.setFont(typesLabel.getFont().deriveFont(Font.BOLD, 12f));
		typesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		typesLabel.setBorder(new EmptyBorder(0, 6, 2, 6));
		discordContent.add(typesLabel);

		// Checkboxes
		eventTypeCheckboxes.clear();
		for (int i = 0; i < EVENT_TYPES.length; i++)
		{
			JCheckBox cb = new JCheckBox(EVENT_TYPE_LABELS[i]);
			cb.setSelected(true);
			cb.setBackground(ColorScheme.DARK_GRAY_COLOR);
			cb.setForeground(Color.LIGHT_GRAY);
			cb.setFont(cb.getFont().deriveFont(11f));
			cb.setAlignmentX(Component.LEFT_ALIGNMENT);
			cb.setBorder(new EmptyBorder(1, 6, 1, 6));
			eventTypeCheckboxes.put(EVENT_TYPES[i], cb);
			discordContent.add(cb);
		}

		discordContent.add(Box.createVerticalStrut(8));

		// Buttons row
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton saveBtn = new JButton("Save");
		saveBtn.setBackground(new Color(0, 150, 60));
		saveBtn.setForeground(Color.WHITE);
		saveBtn.addActionListener(e -> saveWebhook());
		buttonPanel.add(saveBtn);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setBackground(new Color(180, 40, 40));
		removeBtn.setForeground(Color.WHITE);
		removeBtn.addActionListener(e -> removeWebhook());
		buttonPanel.add(removeBtn);

		discordContent.add(buttonPanel);

		discordContent.revalidate();
		discordContent.repaint();
	}

	private void refreshDiscord()
	{
		discordStatusLabel.setText("Loading...");
		discordStatusLabel.setForeground(Color.GRAY);

		apiClient.fetchWebhook(
			webhook -> SwingUtilities.invokeLater(() ->
			{
				if (webhook == null)
				{
					discordStatusLabel.setText("Not configured");
					discordStatusLabel.setForeground(Color.GRAY);
					webhookUrlField.setText("");
					for (JCheckBox cb : eventTypeCheckboxes.values())
					{
						cb.setSelected(true);
					}
					return;
				}

				Object urlObj = webhook.get("webhookUrl");
				webhookUrlField.setText(urlObj != null ? urlObj.toString() : "");

				boolean active = Boolean.TRUE.equals(webhook.get("active"));
				if (active)
				{
					discordStatusLabel.setText("Active");
					discordStatusLabel.setForeground(new Color(0, 200, 83));
				}
				else
				{
					discordStatusLabel.setText("Disabled (auto-disabled after failures)");
					discordStatusLabel.setForeground(new Color(244, 67, 54));
				}

				// Update checkboxes from enabledTypes
				@SuppressWarnings("unchecked")
				List<String> enabledTypes = (List<String>) webhook.get("enabledTypes");
				Set<String> enabledSet = enabledTypes != null ? new HashSet<>(enabledTypes) : new HashSet<>(Arrays.asList(EVENT_TYPES));
				for (Map.Entry<String, JCheckBox> entry : eventTypeCheckboxes.entrySet())
				{
					entry.getValue().setSelected(enabledSet.contains(entry.getKey()));
				}
			}),
			error -> SwingUtilities.invokeLater(() ->
			{
				discordStatusLabel.setText("Error: " + error);
				discordStatusLabel.setForeground(new Color(244, 67, 54));
			})
		);
	}

	private void saveWebhook()
	{
		String url = webhookUrlField.getText().trim();
		if (url.isEmpty())
		{
			discordStatusLabel.setText("Enter a webhook URL");
			discordStatusLabel.setForeground(new Color(255, 152, 0));
			return;
		}

		List<String> enabledTypes = new ArrayList<>();
		for (Map.Entry<String, JCheckBox> entry : eventTypeCheckboxes.entrySet())
		{
			if (entry.getValue().isSelected())
			{
				enabledTypes.add(entry.getKey());
			}
		}

		if (enabledTypes.isEmpty())
		{
			discordStatusLabel.setText("Select at least one event type");
			discordStatusLabel.setForeground(new Color(255, 152, 0));
			return;
		}

		discordStatusLabel.setText("Saving...");
		discordStatusLabel.setForeground(Color.GRAY);

		apiClient.saveWebhook(url, enabledTypes,
			result -> SwingUtilities.invokeLater(() ->
			{
				discordStatusLabel.setText("Active");
				discordStatusLabel.setForeground(new Color(0, 200, 83));
			}),
			error -> SwingUtilities.invokeLater(() ->
			{
				discordStatusLabel.setText("Error: " + error);
				discordStatusLabel.setForeground(new Color(244, 67, 54));
			})
		);
	}

	private void removeWebhook()
	{
		discordStatusLabel.setText("Removing...");
		discordStatusLabel.setForeground(Color.GRAY);

		apiClient.deleteWebhook(
			() -> SwingUtilities.invokeLater(() ->
			{
				discordStatusLabel.setText("Not configured");
				discordStatusLabel.setForeground(Color.GRAY);
				webhookUrlField.setText("");
				for (JCheckBox cb : eventTypeCheckboxes.values())
				{
					cb.setSelected(true);
				}
			}),
			error -> SwingUtilities.invokeLater(() ->
			{
				discordStatusLabel.setText("Error: " + error);
				discordStatusLabel.setForeground(new Color(244, 67, 54));
			})
		);
	}

	// ─── Dashboard ───

	private void openDashboard(JButton btn)
	{
		btn.setEnabled(false);
		btn.setText("Opening...");

		apiClient.generateManagementLink(
			url -> SwingUtilities.invokeLater(() ->
			{
				LinkBrowser.browse(url);
				btn.setEnabled(true);
				btn.setText("Open Dashboard");
			}),
			error -> SwingUtilities.invokeLater(() ->
			{
				btn.setEnabled(true);
				btn.setText("Open Dashboard");
				discordStatusLabel.setText("Dashboard: " + error);
				discordStatusLabel.setForeground(new Color(244, 67, 54));
			})
		);
	}

	// ─── Utilities ───

	private void showConsentPrompt()
	{
		eventsContent.removeAll();

		JLabel msg = new JLabel("<html>Enable <b>Data Sharing Consent</b> in<br>plugin settings to activate.</html>");
		msg.setForeground(Color.GRAY);
		msg.setAlignmentX(Component.LEFT_ALIGNMENT);
		msg.setBorder(new EmptyBorder(8, 4, 8, 0));
		eventsContent.add(msg);

		JButton privacyBtn = new JButton("Privacy Policy");
		privacyBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		privacyBtn.setForeground(Color.WHITE);
		privacyBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		privacyBtn.addActionListener(e -> LinkBrowser.browse("https://conquest.projectonyx.net/privacy.html"));
		eventsContent.add(privacyBtn);
		eventsContent.add(Box.createVerticalStrut(4));

		JButton termsBtn = new JButton("Terms of Service");
		termsBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		termsBtn.setForeground(Color.WHITE);
		termsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		termsBtn.addActionListener(e -> LinkBrowser.browse("https://conquest.projectonyx.net/terms.html"));
		eventsContent.add(termsBtn);

		eventsContent.revalidate();
		eventsContent.repaint();
	}

	private void showPlaceholder(JPanel panel, String message)
	{
		panel.removeAll();
		JLabel label = new JLabel(message);
		label.setForeground(Color.GRAY);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(new EmptyBorder(8, 4, 0, 0));
		panel.add(label);
		panel.revalidate();
		panel.repaint();
	}

	private String formatXp(double xp)
	{
		if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000);
		if (xp >= 1_000) return String.format("%.1fK", xp / 1_000);
		return String.valueOf((long) xp);
	}

	private String stringVal(Map<String, Object> map, String key)
	{
		Object v = map.get(key);
		return v != null ? v.toString() : null;
	}

	private double doubleVal(Map<String, Object> map, String key)
	{
		Object v = map.get(key);
		if (v instanceof Number) return ((Number) v).doubleValue();
		return 0;
	}
}
