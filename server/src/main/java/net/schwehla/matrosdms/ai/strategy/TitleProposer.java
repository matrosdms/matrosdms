/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.ai.strategy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.schwehla.matrosdms.domain.search.MSearchResult;

/**
 * Title suggestions without an LLM, from what the document and the archive already say:
 * <ol>
 * <li>the subject of an e-mail,</li>
 * <li>the title of a document already filed in the same context that the new one resembles, with
 * its dates moved on ("VBL Versicherungsnachweis 2013" becomes "… 2014", "Bezügemitteilung 2023-06"
 * becomes "… 2026-11"): the naming the user
 * follows is learnt from what they file, and every correction they make is the example for the next
 * document,</li>
 * <li>the subject line of a DIN 5008 letter, the line right before the salutation,</li>
 * <li>for scanner file names ("Scan0001", "12_30_2016_02_22…") type, context and year.</li>
 * </ol>
 */
public final class TitleProposer {

	/** A suggested title, how sure (0..1) and where it came from, for the summary. */
	record Proposal(String title, double confidence, String evidence) {
	}

	static final int MAX_TITLE = 120;

	private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

	private static final Pattern REPLY_PREFIX = Pattern
			.compile("^(?:\\s*(?:re|aw|wg|fw|fwd|wtr|antw|tr)\\s*(?:\\[\\d+\\])?\\s*:)+\\s*", FLAGS);
	private static final Pattern META = Pattern.compile("(?s)<meta>.*?</meta>");
	private static final Pattern SALUTATION = Pattern.compile(
			"^\\s*(?:sehr\\s+geehrte|guten\\s+tag|hallo\\b|liebe[rs]?\\b|dear\\b|ladies\\s+and)", FLAGS);
	private static final Pattern SUBJECT_LABEL = Pattern.compile("^\\s*(?:betreff|betr\\.|subject)\\s*:?\\s*", FLAGS);
	/** Lines of the page frame and the information block that can stand right before the salutation. */
	private static final Pattern NOT_A_SUBJECT = Pattern.compile("^(?:seite|page)\\s+\\d|^\\(?anlage|^(?:ihr|unser)e?\\s+zeichen"
			+ "|^(?:kunden|versicherungs|vertrags|rechnungs|mitglieds|personal)[-\\s]?(?:nummer|nr)|^aktenzeichen|^az\\b",
			FLAGS);
	private static final Pattern ADDRESS_OR_CONTACT = Pattern
			.compile("(?<!\\d)\\d{5}(?!\\d)|\\b(?:tel|telefon|fax|e-?mail|www|http)\\b|@", FLAGS);
	private static final String MONTH = "(?:januar|jänner|februar|märz|maerz|april|mai|juni|juli|august|september"
			+ "|oktober|november|dezember|january|february|march|may|june|july|october|december"
			+ "|jan|feb|mär|mrz|apr|jun|jul|aug|sept|sep|okt|oct|nov|dez|dec)";
	private static final String DATE = "(?iu:\\d{1,2}\\.\\s?\\d{1,2}\\.\\s?\\d{2,4}|\\d{4}-\\d{1,2}-\\d{1,2}"
			+ "|(?:\\d{1,2}\\.?\\s*)?(?<!\\p{L})" + MONTH + "\\.?\\s+\\d{4})";
	private static final Pattern DATES = Pattern.compile(DATE);
	/** "Karlsruhe, 22.02.2016", "Frankfurt am Main, den 3. März 2020", "Karlsruhe August 2015". */
	private static final Pattern DATELINE = Pattern
			.compile("^\\s*" + IssueDateFinder.PLACE + "\\s*,?\\s*(?:den\\s+)?" + DATE + "\\s*$");
	private static final Pattern WORD = Pattern.compile("\\p{L}{3,}");
	private static final Pattern YEAR = Pattern.compile("(?<!\\d)(19\\d{2}|20\\d{2})(?!\\d)");
	/**
	 * A month of a year ("2023-06", "06/2023", "06.2023") that is not part of a full date, or else a
	 * year: one pattern, so that the year of a month is not moved a second time.
	 */
	private static final Pattern MONTH_OR_YEAR = Pattern.compile(
			"(?<![\\d./-])(?:(?<isoYear>(?:19|20)\\d{2})-(?<isoMonth>0?[1-9]|1[0-2])"
					+ "|(?<month>0?[1-9]|1[0-2])(?<sep>[./])(?<year>(?:19|20)\\d{2}))(?![./-]?\\d)"
					+ "|(?<!\\d)(?<bareYear>(?:19|20)\\d{2})(?!\\d)");
	private static final Pattern FILE_EXTENSION = Pattern.compile("\\.[a-z][a-z0-9]{0,4}$", FLAGS);
	private static final Pattern SCANNER_NAME = Pattern.compile(
			"^(?:scan|scanned|img|image|dsc|dscn|pxl|photo|foto|bild|dok|doc|document|dokument|file|datei"
					+ "|untitled|unbenannt|neu|new|print|druck)[\\W_]*[\\d\\W_]*$",
			FLAGS);

	/** Words that say nothing about which document it is. */
	private static final Set<String> STOPWORDS = Set.of("und", "oder", "für", "fuer", "von", "vom", "mit", "zum",
			"zur", "bei", "der", "die", "das", "den", "dem", "des", "ihr", "ihre", "ihren", "ihrem", "auf", "aus",
			"nach", "über", "ueber", "the", "and", "for", "neu", "new", "januar", "februar", "märz", "maerz", "april",
			"mai", "juni", "juli", "august", "september", "oktober", "november", "dezember");

	private TitleProposer() {
	}

	/** The subject of an e-mail without "AW:", "WG:", "Re:" and the like; null when there is none. */
	public static String fromEmailSubject(String subject) {
		if (subject == null)
			return null;
		String s = REPLY_PREFIX.matcher(subject.replaceAll("\\s+", " ").trim()).replaceFirst("").trim();
		if (s.isEmpty() || "no subject".equalsIgnoreCase(s))
			return null;
		return cut(s);
	}

	/**
	 * The subject line of a business letter. DIN 5008 sets it without the word "Betreff" between the
	 * address and information block and the salutation, so it is the line right before "Sehr geehrte…".
	 */
	static Optional<Proposal> fromSubjectLine(String text) {
		if (text == null || text.isBlank())
			return Optional.empty();
		String[] lines = META.matcher(text).replaceAll(" ").split("\\R");
		for (int i = 0; i < lines.length; i++) {
			if (!SALUTATION.matcher(lines[i]).find())
				continue;
			for (int j = i - 1; j >= Math.max(0, i - 3); j--) {
				String line = lines[j].trim();
				if (line.isEmpty())
					continue;
				String subject = SUBJECT_LABEL.matcher(line).replaceFirst("").replaceAll("[\\s.:,;]+$", "").trim();
				return isSubject(subject)
						? Optional.of(new Proposal(cut(subject), 0.65, "subject line of the letter"))
						: Optional.empty();
			}
			return Optional.empty(); // only the first salutation, the one of the letter itself
		}
		return Optional.empty();
	}

	/**
	 * The title of the filed document the new one resembles most, with its dates moved on by as much
	 * as the new document is younger: a month ("2023-06") by the months, a year by the years.
	 * Resemblance: how many of the title's words appear in the new document's text or file name, and
	 * how alike the file names are.
	 *
	 * @param neighbours documents already filed in the context the new one goes to
	 * @param newDate the new document's issue date, null when unknown
	 */
	static Optional<Proposal> fromSimilarDocument(List<MSearchResult> neighbours, String text, String filename,
			LocalDate newDate) {
		if (neighbours == null || neighbours.isEmpty())
			return Optional.empty();
		Set<String> fileTokens = tokens(withoutExtension(filename));
		Set<String> documentTokens = new HashSet<>(tokens(text));
		documentTokens.addAll(fileTokens);

		record Scored(MSearchResult doc, double score) {
		}
		Optional<Scored> best = neighbours.stream()
				.filter(n -> n.getName() != null && !n.getName().isBlank())
				.map(n -> {
					Set<String> titleWords = significant(tokens(n.getName()));
					if (titleWords.isEmpty())
						return new Scored(n, 0);
					long found = titleWords.stream().filter(documentTokens::contains).count();
					double coverage = (double) found / titleWords.size();
					double fileSimilarity = jaccard(tokens(withoutExtension(n.getFilename())), fileTokens);
					return new Scored(n, 0.75 * coverage + 0.25 * fileSimilarity);
				})
				.filter(s -> s.score >= 0.5)
				// a title whose date fits its document first (a slip must not become the pattern),
				// then the best match; on a tie the most recent, it shows the current naming
				.max(Comparator
						.comparing((Scored s) -> fitsItsIssueDate(s.doc().getName(), s.doc().getIssueDate()))
						.thenComparingDouble(Scored::score)
						.thenComparing(s -> s.doc().getIssueDate(),
								Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())));
		if (best.isEmpty())
			return Optional.empty();

		MSearchResult doc = best.get().doc;
		String title = doc.getName().trim();
		boolean hasYear = YEAR.matcher(title).find();
		if (hasYear && (newDate == null || doc.getIssueDate() == null))
			return Optional.of(new Proposal(cut(title), 0.35, "title of a similar document"));

		String shifted = hasYear ? moveOn(title, doc.getIssueDate(), newDate) : title;
		double confidence = best.get().score >= 0.75 ? 0.70 : 0.55;
		return Optional.of(new Proposal(cut(shifted), confidence, "follows '" + title + "'"));
	}

	/** True for file names a scanner, phone or program made up, which make no title. */
	static boolean looksGenerated(String filename) {
		String base = withoutExtension(filename).trim();
		if (base.isEmpty())
			return true;
		if (SCANNER_NAME.matcher(base).matches())
			return true;
		long letters = base.chars().filter(Character::isLetter).count();
		return letters * 2 < base.length();
	}

	/** "Schreiben Rente 2015": type, context and year, the last resort for scanner file names. */
	static Optional<Proposal> composed(String kindName, String contextName, LocalDate date) {
		if ((kindName == null || kindName.isBlank()) && (contextName == null || contextName.isBlank()))
			return Optional.empty(); // a bare year is no title
		String title = Stream.of(kindName, contextName, date == null ? null : String.valueOf(date.getYear()))
				.filter(s -> s != null && !s.isBlank())
				.map(String::trim)
				.collect(Collectors.joining(" "));
		return title.isEmpty() ? Optional.empty()
				: Optional.of(new Proposal(cut(title), 0.30, "type, context and year"));
	}

	/**
	 * Moves the dates of a title on from the issue date of its document to that of the new one. A
	 * month moves by the months between the two ("Bezügemitteilung 2023-06" of 16 June 2023 becomes
	 * "… 2026-11" for a document of 17 November 2026), a year alone by the years ("Steuer 2013/2014").
	 */
	static String moveOn(String title, LocalDate from, LocalDate to) {
		long months = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to));
		int years = to.getYear() - from.getYear();
		if (months == 0 && years == 0)
			return title;
		Matcher m = MONTH_OR_YEAR.matcher(title);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			String replacement;
			if (m.group("bareYear") != null) {
				replacement = String.valueOf(Integer.parseInt(m.group("bareYear")) + years);
			} else {
				boolean iso = m.group("isoYear") != null;
				String monthText = iso ? m.group("isoMonth") : m.group("month");
				YearMonth moved = YearMonth
						.of(Integer.parseInt(iso ? m.group("isoYear") : m.group("year")), Integer.parseInt(monthText))
						.plusMonths(months);
				// as written: "2023-06" stays two-digit, "6/2023" one-digit where it can
				String month = monthText.length() == 2 ? String.format("%02d", moved.getMonthValue())
						: String.valueOf(moved.getMonthValue());
				replacement = iso ? moved.getYear() + "-" + month : month + m.group("sep") + moved.getYear();
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/** True when the title carries a year: the titles of a series do, "Freistellungsauftrag" does not. */
	static boolean hasDate(String title) {
		return title != null && YEAR.matcher(title).find();
	}

	/** "Bezügemitteilung 2023-06" without its date: "Bezügemitteilung". */
	static String withoutDates(String title) {
		return MONTH_OR_YEAR.matcher(title).replaceAll(" ").replaceAll("\\s+", " ").trim();
	}

	/**
	 * False for a title whose month or year lies far from its own issue date, like "Bezügemitteilung
	 * 2025-04" issued on 14 November 2025: a slip that must not become the pattern for the next title.
	 * A month may be two months off, a year two years ("VBL Versicherungsnachweis 2013" comes in 2014).
	 */
	static boolean fitsItsIssueDate(String title, LocalDate issueDate) {
		if (title == null || issueDate == null)
			return true;
		Matcher m = MONTH_OR_YEAR.matcher(title);
		boolean dated = false;
		while (m.find()) {
			dated = true;
			if (m.group("bareYear") != null) {
				if (Math.abs(Integer.parseInt(m.group("bareYear")) - issueDate.getYear()) <= 2)
					return true;
			} else {
				boolean iso = m.group("isoYear") != null;
				YearMonth month = YearMonth.of(Integer.parseInt(iso ? m.group("isoYear") : m.group("year")),
						Integer.parseInt(iso ? m.group("isoMonth") : m.group("month")));
				if (Math.abs(ChronoUnit.MONTHS.between(month, YearMonth.from(issueDate))) <= 2)
					return true;
			}
		}
		return !dated;
	}

	private static boolean isSubject(String line) {
		if (line.length() < 4 || line.length() > 160)
			return false;
		if (Character.isLowerCase(line.charAt(0)))
			return false; // the end of a sentence, not a subject line
		if (ADDRESS_OR_CONTACT.matcher(line).find() || NOT_A_SUBJECT.matcher(line).find())
			return false;
		// a date line ("Karlsruhe, 22.02.2016", "Karlsruhe August 2015") is no subject
		if (DATELINE.matcher(line).matches())
			return false;
		Matcher words = WORD.matcher(DATES.matcher(line).replaceAll(" "));
		int count = 0;
		while (words.find())
			count++;
		return count >= 2;
	}

	static Set<String> tokens(String s) {
		Set<String> out = new HashSet<>();
		if (s == null)
			return out;
		Matcher m = WORD.matcher(s.toLowerCase());
		while (m.find())
			out.add(m.group());
		return out;
	}

	static Set<String> significant(Set<String> words) {
		return words.stream().filter(w -> !STOPWORDS.contains(w)).collect(Collectors.toSet());
	}

	private static double jaccard(Set<String> a, Set<String> b) {
		if (a.isEmpty() || b.isEmpty())
			return 0;
		long shared = a.stream().filter(b::contains).count();
		Set<String> union = new HashSet<>(a);
		union.addAll(b);
		return (double) shared / union.size();
	}

	static String withoutExtension(String filename) {
		return filename == null ? "" : FILE_EXTENSION.matcher(filename.trim()).replaceFirst("");
	}

	private static String cut(String s) {
		String t = s.replaceAll("\\s+", " ").trim();
		return t.length() > MAX_TITLE ? t.substring(0, MAX_TITLE).trim() : t;
	}
}
