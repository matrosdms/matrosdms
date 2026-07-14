/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A tail-follow view of the rolling log file, reachable from the tray.
 *
 * <p>The desktop build launches without a console, so this is the only way a user can see what the
 * server is doing. It does not capture stdout: it tails the file logback already writes
 * ({@code <data>/workspace/log/matrosdms.log}), which means it also shows what happened before the
 * window was opened.
 */
final class LogWindow {

	/** Trim the view at this many characters so a long-running server cannot exhaust the heap. */
	private static final int MAX_CHARS = 600_000;

	/** On open, seed the view with the tail of the file rather than all of it. */
	private static final int INITIAL_TAIL_BYTES = 128 * 1024;

	private static Frame frame;
	private static Timer timer;

	private LogWindow() {
	}

	/** Opens the window, or brings it to the front if it is already open. */
	static void showOrFocus(Path logFile) {
		EventQueue.invokeLater(() -> {
			if (frame != null) {
				frame.setVisible(true);
				frame.setState(Frame.NORMAL);
				frame.toFront();
				return;
			}
			build(logFile);
		});
	}

	private static void build(Path logFile) {
		Frame f = new Frame("MatrosDMS — Logs");
		f.setIconImage(MatrosBadge.image(32));
		f.setLayout(new BorderLayout());

		TextArea area = new TextArea("", 30, 120, TextArea.SCROLLBARS_BOTH);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		f.add(area, BorderLayout.CENTER);

		Panel bar = new Panel(new FlowLayout(FlowLayout.LEFT));
		bar.add(new Label(logFile.toString()));

		Button clear = new Button("Clear view");
		clear.addActionListener(e -> area.setText(""));
		bar.add(clear);

		Button openFolder = new Button("Open folder");
		openFolder.addActionListener(e -> openFolder(logFile));
		bar.add(openFolder);

		f.add(bar, BorderLayout.SOUTH);

		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		f.setSize(900, 540);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
		frame = f;

		long offset = seedInitial(logFile, area);
		startTail(logFile, area, offset);
	}

	private static void openFolder(Path logFile) {
		try {
			Path dir = logFile.getParent();
			if (dir != null && Desktop.isDesktopSupported()
					&& Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				Desktop.getDesktop().open(dir.toFile());
			}
		} catch (Exception ignored) {
			// Nothing useful to tell the user here — the path is on screen either way.
		}
	}

	/** Fills the view with the tail of the file and returns the offset to follow from. */
	private static long seedInitial(Path logFile, TextArea area) {
		try {
			long length = Files.size(logFile);
			long from = Math.max(0, length - INITIAL_TAIL_BYTES);
			try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
				raf.seek(from);
				byte[] buf = new byte[(int) (length - from)];
				raf.readFully(buf);
				String text = new String(buf, StandardCharsets.UTF_8);
				if (from > 0) {
					// We seeked into the middle of a line; drop the partial one.
					int newline = text.indexOf('\n');
					if (newline >= 0) {
						text = text.substring(newline + 1);
					}
				}
				area.setText(text);
				area.setCaretPosition(area.getText().length());
			}
			return length;
		} catch (IOException e) {
			area.setText("(log file not readable yet: " + logFile + ")\n");
			return 0;
		}
	}

	private static void startTail(Path logFile, TextArea area, long startOffset) {
		Timer t = new Timer("matrosdms-logtail", true);
		t.schedule(new TimerTask() {
			private long offset = startOffset;

			@Override
			public void run() {
				try {
					long length = Files.size(logFile);
					boolean rolled = length < offset;
					if (rolled) {
						offset = 0; // the file rotated — start over from the new one
					}
					if (length == offset) {
						return;
					}
					String chunk;
					try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
						raf.seek(offset);
						byte[] buf = new byte[(int) Math.min(length - offset, MAX_CHARS)];
						raf.readFully(buf);
						chunk = new String(buf, StandardCharsets.UTF_8);
					}
					offset = length;
					EventQueue.invokeLater(() -> append(area, chunk, rolled));
				} catch (IOException ignored) {
					// Most likely caught mid-rotation; the next tick picks it up.
				}
			}
		}, 1000, 1000);
		timer = t;
	}

	private static void append(TextArea area, String chunk, boolean rolled) {
		if (rolled) {
			area.setText("");
		}
		area.append(chunk);
		String text = area.getText();
		if (text.length() > MAX_CHARS) {
			area.replaceRange("", 0, text.length() - MAX_CHARS);
		}
		area.setCaretPosition(area.getText().length());
	}

	private static void dispose() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (frame != null) {
			frame.dispose();
			frame = null;
		}
	}
}
