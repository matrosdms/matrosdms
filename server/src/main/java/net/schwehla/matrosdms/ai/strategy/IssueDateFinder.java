/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Finds the date a document was issued in its text.
 *
 * <p>
 * A letter mentions many dates: the period it covers ("01.01.-31.12.2014"), the reference date of
 * a balance ("Stand zum 31. Dezember 2014"), a due date, a birth date, the date of the letter it
 * answers. Taking the first date found is therefore wrong most of the time. Every date in the text
 * is collected and scored by where it stands, following the layout of a DIN 5008 business letter:
 * <ul>
 * <li>a date line ("Karlsruhe, 22.02.2016", "Berlin, den 3. März 2020", "Karlsruhe August 2015", a
 * date alone on its line), a label ("Datum:", "Rechnungsdatum") or the date column of the reference
 * line (labels "Ihr Zeichen … Datum" on one line, the values on the next) counts most,</li>
 * <li>the letterhead (the first lines) counts more than the body,</li>
 * <li>periods, "Stand … zum", "bis", "fällig", "ab", "seit" and "Ihr Schreiben vom" count little,
 * birth dates not at all,</li>
 * <li>a date after today only counts on a date line or with a label (insurers date their yearly
 * letters ahead, "Oktober 2027"); in running text it is a due date or an appointment,</li>
 * <li>today's date counts little: a document filed today was rarely written today.</li>
 * </ul>
 * A month without a day ("August 2015") stands for the first of that month.
 *
 * <p>
 * The metadata envelope of the text layer is ignored; its {@code processed_date} is the day the
 * file was read, not a date of the document.
 */
final class IssueDateFinder {

	/** A date found, how sure the finder is (0..1) and where it came from, for the summary. */
	record Found(LocalDate date, double confidence, String evidence) {
	}

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static {
		String[][] names = {
				{ "januar", "january", "jan", "jänner", "jaenner" },
				{ "februar", "february", "feb" },
				{ "märz", "maerz", "march", "mär", "mrz", "mar" },
				{ "april", "apr" },
				{ "mai", "may" },
				{ "juni", "june", "jun" },
				{ "juli", "july", "jul" },
				{ "august", "aug" },
				{ "september", "sept", "sep" },
				{ "oktober", "october", "okt", "oct" },
				{ "november", "nov" },
				{ "dezember", "december", "dez", "dec" } };
		for (int m = 0; m < names.length; m++)
			for (String n : names[m])
				MONTHS.put(n, m + 1);
	}

	/** Month names longest first, so the regex alternation prefers "januar" over "jan". */
	private static final String MONTH_ALTERNATION = MONTHS.keySet().stream()
			.sorted(Comparator.comparingInt(String::length).reversed())
			.collect(Collectors.joining("|"));

	/** Only written-out names stand for a month without a day ("August 2015"), not "Mar" or "Dez". */
	private static final String FULL_MONTH_ALTERNATION = Arrays
			.stream(new String[] { "januar", "january", "jänner", "februar", "february", "märz", "maerz", "march",
					"april", "mai", "may", "juni", "june", "juli", "july", "august", "september", "oktober",
					"october", "november", "dezember", "december" })
			.sorted(Comparator.comparingInt(String::length).reversed())
			.collect(Collectors.joining("|"));

	private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

	private static final Pattern ISO = Pattern.compile("(?<![\\d.])(\\d{4})-(\\d{1,2})-(\\d{1,2})(?![\\d])");
	private static final Pattern NUMERIC = Pattern
			.compile("(?<![\\d.])(\\d{1,2})([./])\\s?(\\d{1,2})\\2\\s?(\\d{4}|\\d{2})(?![\\d])");
	private static final Pattern NUMERIC_DASH = Pattern.compile("(?<![\\d.-])(\\d{1,2})-(\\d{1,2})-(\\d{4})(?![\\d])");
	private static final Pattern TEXT_MONTH = Pattern
			.compile("(?<![\\d.])(\\d{1,2})\\.?\\s*(" + MONTH_ALTERNATION + ")\\.?\\s+(\\d{4})(?![\\d])", FLAGS);
	private static final Pattern MONTH_YEAR = Pattern
			.compile("(?<![\\p{L}])(" + FULL_MONTH_ALTERNATION + ")\\s+(\\d{4})(?![\\d])", FLAGS);
	/** Any numeric date, to tell whether another one follows on the same line. */
	private static final Pattern ANY_NUMERIC_DATE = Pattern.compile("\\d{1,2}\\.\\s?\\d{1,2}\\.\\s?\\d{2,4}|\\d{4}-\\d{1,2}-\\d{1,2}");

	private static final Pattern META = Pattern.compile("(?s)<meta>.*?</meta>");

	/**
	 * A place before the date on its line, "Karlsruhe," or "Frankfurt am Main, den" or "Bad Homburg v.
	 * d. Höhe,": capitalised words, the small words of place names only between two of them, so neither
	 * a sentence ("Die Zahlung erfolgte am") nor "Gedruckt am" is taken for a date line.
	 */
	static final String PLACE = "\\p{Lu}[\\p{L}.\\-]*(?:\\s+(?:(?:am|an|der|bei|ob|im|in|v\\.|d\\.|a\\.|i\\.)\\s+)*"
			+ "\\p{Lu}[\\p{L}.\\-]*){0,4}";
	private static final Pattern DATELINE_BEFORE = Pattern.compile("^\\s*(?:" + PLACE + ")?\\s*,?\\s*(?:den\\s+)?$");
	/** Print dates ("Druckdatum", "Gedruckt am") are left out: a printout is mostly made the day it is filed. */
	private static final Pattern LABEL = Pattern.compile(
			"(?:rechnungs|beleg|ausstellungs|bescheid|brief|schreib)?datum\\s*:?\\s*$|\\bdate[d]?\\s*:?\\s*$"
					+ "|ausgestellt\\s+am\\s*:?\\s*$|erstellt\\s+am\\s*:?\\s*$",
			FLAGS);
	/** The label line of a DIN 5008 reference line ends with the date column: "Ihr Zeichen … Datum". */
	private static final Pattern REFERENCE_LINE_LABELS = Pattern.compile("\\b(?:datum|date)\\s*:?\\s*$", FLAGS);
	private static final Pattern VOM = Pattern.compile("\\bvom\\s*$", FLAGS);
	private static final Pattern REFERENCE = Pattern.compile(
			"\\b(?:ihr|ihre|ihrem|ihren|unser|unsere|unserem|unseren)\\s+"
					+ "(?:schreiben|nachricht|brief|antrag|anfrage|e-?mail|mail|zeichen|auftrag|bestellung|bescheid)"
					+ "\\s+vom\\s*$",
			FLAGS);
	/** A word within the last three before the date that makes it a period, deadline or reference date. */
	private static final Pattern OTHER_DATE = Pattern.compile(
			"(?:^|[\\s(])(?:bis|zum|stand|seit|ab|beginn\\p{L}*|ende|endet|ablauf\\p{L}*|gültig\\p{L}*|gueltig\\p{L}*"
					+ "|fällig\\p{L}*|faellig\\p{L}*|zahlbar|frist\\p{L}*|termin|laufzeit|zeitraum|spätestens|spaetestens"
					+ "|valuta|lieferdatum|leistungsdatum|leistungszeitraum|kündigung\\p{L}*|kuendigung\\p{L}*"
					+ "|vertragsbeginn|versicherungsbeginn|vor\\s+dem|nach\\s+dem|until|due|valid"
					+ "|ez|erstzulassung|zulassung|hu|au|baujahr)"
					+ "(?:\\s+\\S+){0,2}\\s*:?\\s*$",
			FLAGS);
	private static final Pattern BIRTH = Pattern.compile(
			"(?:geb\\.|geboren|geburtsdatum|geburtstag|born|birth|dob)(?:\\s+\\S+){0,2}\\s*:?\\s*$", FLAGS);
	private static final Pattern RANGE_START = Pattern.compile("^\\s*(?:-|–|bis)\\s*\\d", FLAGS);
	private static final Pattern RANGE_END = Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.?(?:\\d{2,4})?\\s*(?:-|–|bis)\\s*$",
			FLAGS);

	/** Dates in file names as scanners and phones write them: 2016_02_22, 20160222, 22-02-2016. */
	private static final Pattern FILENAME_YMD = Pattern
			.compile("(?<!\\d)(19\\d{2}|20\\d{2})[-_. ]?(\\d{2})[-_. ]?(\\d{2})(?!\\d)");
	private static final Pattern FILENAME_DMY = Pattern.compile("(?<!\\d)(\\d{2})[-_.](\\d{2})[-_.](19\\d{2}|20\\d{2})(?!\\d)");

	private static final int LETTERHEAD_LINES = 25;
	private static final double MIN_SCORE = 0.25;
	/** How far ahead a letter can be dated; anything later is a deadline, a typo or OCR noise. */
	private static final int MAX_DAYS_AHEAD = 400;

	private IssueDateFinder() {
	}

	/** The most likely issue date in the text, or empty when no date in it can be the issue date. */
	static Optional<Found> find(String text, LocalDate today) {
		if (text == null || text.isBlank())
			return Optional.empty();
		String content = META.matcher(text).replaceAll(" ");

		List<Candidate> candidates = new ArrayList<>();
		collect(content, today, candidates);

		return candidates.stream()
				.filter(c -> c.score >= MIN_SCORE)
				// best score; on a tie the earlier one, the letterhead comes first
				.max(Comparator.comparingDouble((Candidate c) -> c.score).thenComparing(c -> -c.start))
				.map(Candidate::toFound);
	}

	/** A date in the file name, the fallback when the text has none. Scanners name files by the scan day. */
	static Optional<Found> fromFilename(String filename, LocalDate today) {
		if (filename == null || filename.isBlank())
			return Optional.empty();
		Matcher ymd = FILENAME_YMD.matcher(filename);
		while (ymd.find()) {
			LocalDate d = date(ymd.group(1), ymd.group(2), ymd.group(3));
			if (d != null && !d.isAfter(today))
				return Optional.of(new Found(d, 0.40, "date in the file name"));
		}
		Matcher dmy = FILENAME_DMY.matcher(filename);
		while (dmy.find()) {
			LocalDate d = date(dmy.group(3), dmy.group(2), dmy.group(1));
			if (d != null && !d.isAfter(today))
				return Optional.of(new Found(d, 0.40, "date in the file name"));
		}
		return Optional.empty();
	}

	private static void collect(String content, LocalDate today, List<Candidate> out) {
		List<int[]> taken = new ArrayList<>();

		Matcher m = TEXT_MONTH.matcher(content);
		while (m.find()) {
			Integer month = MONTHS.get(m.group(2).toLowerCase());
			add(out, taken, content, m, month == null ? null : date(m.group(3), month, m.group(1)), false, false,
					today);
		}
		m = ISO.matcher(content);
		while (m.find())
			add(out, taken, content, m, date(m.group(1), m.group(2), m.group(3)), false, false, today);
		m = NUMERIC.matcher(content);
		while (m.find()) {
			String year = m.group(4);
			add(out, taken, content, m, date(fullYear(year, today), m.group(3), m.group(1)), false,
					year.length() == 2, today);
		}
		m = NUMERIC_DASH.matcher(content);
		while (m.find())
			add(out, taken, content, m, date(m.group(3), m.group(2), m.group(1)), false, false, today);
		m = MONTH_YEAR.matcher(content);
		while (m.find()) {
			Integer month = MONTHS.get(m.group(1).toLowerCase());
			add(out, taken, content, m, month == null ? null : date(m.group(2), month, "1"), true, false, today);
		}
	}

	private static void add(List<Candidate> out, List<int[]> taken, String content, Matcher m, LocalDate date,
			boolean monthOnly, boolean shortYear, LocalDate today) {
		int start = m.start();
		int end = m.end();
		for (int[] t : taken)
			if (start < t[1] && end > t[0])
				return; // part of a date already found ("Dezember 2014" in "31. Dezember 2014")
		taken.add(new int[] { start, end });
		if (date == null || date.getYear() < 1900 || date.isAfter(today.plusDays(MAX_DAYS_AHEAD)))
			return;

		int lineStart = content.lastIndexOf('\n', start - 1) + 1;
		int lineEnd = content.indexOf('\n', end);
		if (lineEnd < 0)
			lineEnd = content.length();
		String before = content.substring(lineStart, start);
		String after = content.substring(end, lineEnd);
		String near = before.length() > 60 ? before.substring(before.length() - 60) : before;
		String previousLine = lineStart == 0 ? ""
				: content.substring(content.lastIndexOf('\n', lineStart - 2) + 1, lineStart - 1);
		int line = countLines(content, lineStart);

		Candidate c = new Candidate(date, start, m.group());
		c.monthOnly = monthOnly;

		boolean otherKindOfDate = true;
		if (BIRTH.matcher(near).find()) {
			c.score *= 0.05;
			c.evidence = "birth date";
		} else if (REFERENCE.matcher(near).find()) {
			c.score *= 0.3;
			c.evidence = "date of a letter it refers to";
		} else if (OTHER_DATE.matcher(near).find() || RANGE_END.matcher(near).find()
				|| RANGE_START.matcher(after).find()) {
			c.score *= 0.3;
			c.evidence = "period, deadline or reference date";
		} else {
			otherKindOfDate = false;
		}

		if (!otherKindOfDate) {
			if (LABEL.matcher(near).find()) {
				c.strong("labelled '" + near.trim() + "'", 1.5);
			} else if (REFERENCE_LINE_LABELS.matcher(previousLine).find()
					&& !ANY_NUMERIC_DATE.matcher(after).find()) {
				c.strong("date column of the reference line", 1.5);
			} else if (DATELINE_BEFORE.matcher(before).matches() && after.trim().length() <= 3) {
				c.strong(before.isBlank() ? "date on a line of its own"
						: "date line '" + (before + m.group()).trim() + "'", 2.0);
			} else if (VOM.matcher(near).find()) {
				c.score += 0.3;
				c.evidence = "'vom' date";
			}
			if (line < LETTERHEAD_LINES)
				c.score += 0.5;
		}

		if (monthOnly)
			c.score *= 0.8;
		if (shortYear)
			c.score *= 0.9;
		if (date.isAfter(today)) {
			// a letter dated ahead is fine on its date line; in running text it is a due date
			if (c.isStrong)
				c.ahead = true;
			else
				c.score *= 0.1;
		} else if (date.equals(today)) {
			c.score *= 0.5;
			c.evidence = c.evidence + ", today";
		}
		out.add(c);
	}

	private static int countLines(String s, int upTo) {
		int n = 0;
		for (int i = 0; i < upTo; i++)
			if (s.charAt(i) == '\n')
				n++;
		return n;
	}

	/** Two-digit years: up to this year's two digits they are 20xx, later ones 19xx ("05.03.98"). */
	private static String fullYear(String year, LocalDate today) {
		if (year.length() != 2)
			return year;
		int yy = Integer.parseInt(year);
		int century = yy <= today.getYear() % 100 ? 2000 : 1900;
		return String.valueOf(century + yy);
	}

	private static LocalDate date(String year, Object month, String day) {
		try {
			int mo = month instanceof Integer i ? i : Integer.parseInt(month.toString());
			return LocalDate.of(Integer.parseInt(year), mo, Integer.parseInt(day));
		} catch (NumberFormatException | DateTimeException e) {
			return null;
		}
	}

	private static final class Candidate {
		final LocalDate date;
		final int start;
		final String text;
		double score = 1.0;
		boolean isStrong;
		boolean monthOnly;
		boolean ahead;
		String evidence = "date in the text";

		Candidate(LocalDate date, int start, String text) {
			this.date = date;
			this.start = start;
			this.text = text;
		}

		void strong(String why, double bonus) {
			score += bonus;
			isStrong = true;
			evidence = why;
		}

		Found toFound() {
			double confidence;
			if (isStrong && ahead)
				confidence = 0.60;
			else if (isStrong)
				confidence = monthOnly ? 0.70 : 0.85;
			else if (score >= 1.0)
				confidence = 0.55;
			else
				confidence = 0.35;
			return new Found(date, confidence, evidence + " (" + text.trim() + ")");
		}
	}
}
