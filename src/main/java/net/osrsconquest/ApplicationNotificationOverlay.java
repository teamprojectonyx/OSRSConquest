package net.osrsconquest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Singleton
public class ApplicationNotificationOverlay extends Overlay
{
	private static final int DISPLAY_DURATION_MS = 5000;
	private static final Color BACKGROUND = new Color(0, 0, 0, 180);
	private static final Color GOLD = new Color(255, 215, 0);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final Font TITLE_FONT = new Font("RuneScape Bold", Font.BOLD, 22);
	private static final Font NAME_FONT = new Font("RuneScape", Font.PLAIN, 16);
	private static final Font FALLBACK_TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
	private static final Font FALLBACK_NAME_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

	private final Deque<Notification> notifications = new ConcurrentLinkedDeque<>();

	@Inject
	public ApplicationNotificationOverlay()
	{
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	public void showNotification(String playerName)
	{
		notifications.addLast(new Notification(playerName, System.currentTimeMillis()));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long now = System.currentTimeMillis();

		// Remove expired notifications
		while (!notifications.isEmpty() && now - notifications.peekFirst().timestamp > DISPLAY_DURATION_MS)
		{
			notifications.pollFirst();
		}

		if (notifications.isEmpty())
		{
			return null;
		}

		Notification notification = notifications.peekFirst();

		// Calculate opacity for fade out (last 1 second)
		float alpha = 1.0f;
		long elapsed = now - notification.timestamp;
		if (elapsed > DISPLAY_DURATION_MS - 1000)
		{
			alpha = Math.max(0f, (DISPLAY_DURATION_MS - elapsed) / 1000f);
		}

		// Use RuneScape font if available, otherwise fallback
		Font titleFont = isFontAvailable("RuneScape Bold") ? TITLE_FONT : FALLBACK_TITLE_FONT;
		Font nameFont = isFontAvailable("RuneScape") ? NAME_FONT : FALLBACK_NAME_FONT;

		String title = "CLAN APPLICATION";
		String body = notification.playerName + " wants to join!";

		graphics.setFont(titleFont);
		FontMetrics titleMetrics = graphics.getFontMetrics();
		int titleWidth = titleMetrics.stringWidth(title);

		graphics.setFont(nameFont);
		FontMetrics nameMetrics = graphics.getFontMetrics();
		int nameWidth = nameMetrics.stringWidth(body);

		int width = Math.max(titleWidth, nameWidth) + 40;
		int height = titleMetrics.getHeight() + nameMetrics.getHeight() + 24;

		// Background
		Composite originalComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, width, height, 10, 10);

		// Gold border
		graphics.setColor(withAlpha(GOLD, alpha));
		graphics.setStroke(new BasicStroke(2));
		graphics.drawRoundRect(0, 0, width, height, 10, 10);

		// Title
		graphics.setFont(titleFont);
		graphics.setColor(withAlpha(GOLD, alpha));
		int titleX = (width - titleWidth) / 2;
		graphics.drawString(title, titleX, titleMetrics.getAscent() + 8);

		// Player name
		graphics.setFont(nameFont);
		graphics.setColor(withAlpha(TEXT_COLOR, alpha));
		int nameX = (width - nameWidth) / 2;
		graphics.drawString(body, nameX, titleMetrics.getHeight() + nameMetrics.getAscent() + 16);

		graphics.setComposite(originalComposite);

		return new Dimension(width, height);
	}

	private static Color withAlpha(Color color, float alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(color.getAlpha() * alpha));
	}

	private static boolean isFontAvailable(String fontName)
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (String name : ge.getAvailableFontFamilyNames())
		{
			if (name.equals(fontName)) return true;
		}
		return false;
	}

	private static class Notification
	{
		final String playerName;
		final long timestamp;

		Notification(String playerName, long timestamp)
		{
			this.playerName = playerName;
			this.timestamp = timestamp;
		}
	}
}
