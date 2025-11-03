package seedu.address.model.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.core.LogsCenter;
import seedu.address.model.interaction.Interaction;
import seedu.address.model.person.Address;
import seedu.address.model.person.Cadence;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Role;
import seedu.address.model.tag.Tag;

/**
 * CSV utilities for the address book domain.
 *
 * <p><b>Export:</b> Provides {@link #writeHeader(List, java.io.Writer)},
 * {@link #writeRow(List, java.io.Writer)}, and {@link #escape(String)} to write
 * CSV safely. Fields containing commas, quotes, or newlines are escaped by
 * doubling quotes and wrapping the cell in quotes.</p>
 *
 * <p><b>Import (HEADER-BASED ONLY):</b> Requires a header row with at least the following columns
 * (case-insensitive): {@code Name}, {@code Role}, {@code Address}, {@code Phone} and {@code Email}.
 * Optional columns: {@code Tags}, {@code Cadence}, {@code Interactions}.
 * Unknown columns are ignored for forward-compatibility.</p>
 */
public class CsvUtil {

    private static final Logger logger = LogsCenter.getLogger(CsvUtil.class);
    private static final Pattern INT_FIRST = Pattern.compile("(-?\\d+)");
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "Investor", "Partner", "Customer", "Lead"
    );

    /** CSV delimiter used for import/export. */
    private static final char DELIM = ',';
    /** Line separator appended after each written row. */
    private static final String NEWLINE = "\n";
    private static final String H_NAME = "name";
    private static final String H_PHONE = "phone";
    private static final String H_EMAIL = "email";
    private static final String H_ADDRESS = "address";
    private static final String H_TAGS = "tags";
    private static final String H_ROLE = "role";
    private static final String H_CADENCE = "cadence";
    private static final String H_INTERACTIONS = "interactions";

    private static final int IDX_MISSING = -1;

    /** Holds mapped column indices for known headers. Unknown columns are ignored. */
    private static final class HeaderIndex {
        private int name = IDX_MISSING;
        private int phone = IDX_MISSING;
        private int email = IDX_MISSING;
        private int address = IDX_MISSING;
        private int tags = IDX_MISSING;
        private int role = IDX_MISSING;
        private int cadence = IDX_MISSING;
        private int interactions = IDX_MISSING;
    }

    /**
     * Reads and parses all valid {@link Person} entries from a CSV file.
     * <p>
     * This method ensures the file exists, detects the delimiter automatically,
     * validates the header row (must contain at least Name, Role, Address, Phone, and Email),
     * and skips malformed rows with warnings. Each valid row is converted into a
     * {@link Person} object using {@link CsvUtil#readRows(java.io.BufferedReader, char, HeaderIndex)}.
     * <p>
     * Logs progress and statistics about parsed entries for debugging and traceability.
     *
     * @param filePath path to the CSV file to be read (must not be null and must exist)
     * @return a list of valid {@link Person} objects parsed from the file; never null
     * @throws IOException              if an I/O error occurs during reading
     * @throws IllegalArgumentException if the CSV is missing a valid header row or is malformed
     */
    public static List<Person> readPersonsFromCsv(Path filePath) throws IOException {
        requireNonNull(filePath);
        assert Files.exists(filePath) : "CSV file path must exist before reading.";

        try (var br = Files.newBufferedReader(filePath)) {
            HeaderInfo hdr = findHeader(br);
            if (hdr == null) {
                throw new IllegalArgumentException(
                        "CSV must contain a header row with at least: Name, Role, Address, "
                                + "Phone, and Email (case-insensitive).");
            }

            HeaderIndex hi = mapHeader(hdr.headerCells);
            requireMandatory(hi);

            List<Person> persons = readRows(br, hdr.detectedDelim, hi);
            logger.info("Parsed " + persons.size() + " valid contacts from CSV: " + filePath);
            return persons;
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Failed to read CSV file: " + filePath, ioe);
            throw ioe;
        }
    }

    /** Holds delimiter + header cells returned by findHeader. */
    private static final class HeaderInfo {
        final char detectedDelim;
        final List<String> headerCells;
        HeaderInfo(char d, List<String> h) {
            this.detectedDelim = d;
            this.headerCells = h;
        }
    }

    /** Scans until a header-like line is found; returns null if none. */
    private static HeaderInfo findHeader(java.io.BufferedReader br) throws IOException {
        String line;
        int lineNo = 0;
        while ((line = br.readLine()) != null) {
            lineNo++;
            if (line.trim().isEmpty()) {
                continue;
            }
            String candidate = line.replace("\uFEFF", "");
            char delim = detectDelimiter(candidate);
            List<String> cells = splitCsvLine(candidate, delim);
            if (looksLikeHeader(cells)) {
                logger.info("Detected CSV header at line " + lineNo + " using delimiter '" + delim + "'");
                return new HeaderInfo(delim, cells);
            }
        }
        return null;
    }

    /** Throws if any mandatory column is missing. */
    private static void requireMandatory(HeaderIndex hi) {
        List<String> missing = new ArrayList<>();
        if (hi.name == IDX_MISSING) {
            missing.add("Name");
        }
        if (hi.role == IDX_MISSING) {
            missing.add("Role");
        }
        if (hi.address == IDX_MISSING) {
            missing.add("Address");
        }
        if (hi.phone == IDX_MISSING) {
            missing.add("Phone");
        }
        if (hi.email == IDX_MISSING) {
            missing.add("Email");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV header missing mandatory column(s): " + String.join(", ", missing)
                            + " (case-insensitive)."
            );
        }
    }

    private static Optional<Tag> tryParseTag(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Tag(trimmed));
        } catch (IllegalArgumentException ex) {
            String slug = trimmed.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
            if (slug.isEmpty()) {
                return Optional.empty();
            }
            try {
                return Optional.of(new Tag(slug));
            } catch (IllegalArgumentException ex2) {
                logger.log(Level.WARNING, "Skipping invalid tag after sanitize: " + trimmed);
                return Optional.empty();
            }
        }
    }

    /** Reads remaining lines as rows and converts each to Person, skipping blanks/bad rows with warnings. */
    private static List<Person> readRows(java.io.BufferedReader br, char delim, HeaderIndex hi) throws IOException {
        List<Person> persons = new ArrayList<>();
        String raw;
        int lineNo = 1;
        while ((raw = br.readLine()) != null) {
            lineNo++;
            if (raw.trim().isEmpty()) {
                continue;
            }
            List<String> cells = splitCsvLine(raw, delim);
            if (isBlankRow(cells)) {
                continue;
            }
            try {
                persons.add(parsePersonByHeader(cells, hi));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING,
                        String.format("Skipping malformed row %d: %s (%s)", lineNo, raw, e.getMessage()));
            }
        }
        return persons;
    }


    private static Person parsePersonByHeader(List<String> cells, HeaderIndex hi) {
        String nameStr = norm(getCell(cells, hi.name));
        String phoneStr = norm(getCell(cells, hi.phone));
        String emailStr = norm(getCell(cells, hi.email));
        String addrStr = norm(getCell(cells, hi.address));

        if (hi.role == IDX_MISSING) {
            throw new IllegalArgumentException("CSV header must include Role.");
        }
        String roleStr = norm(getCell(cells, hi.role));
        if (nameStr.isEmpty() || roleStr.isEmpty() || addrStr.isEmpty() || phoneStr.isEmpty()
                || emailStr.isEmpty()) {
            throw new IllegalArgumentException("Row needs non-empty Name, Role, Address, "
                    + "Phone, and Email.");
        }
        if (!ALLOWED_ROLES.contains(roleStr)) {
            throw new IllegalArgumentException(
                    "Role must be exactly one of: Investor, Partner, Customer, Lead (case-sensitive). "
                            + "Found: \"" + roleStr + "\""
            );
        }

        Name name = new Name(nameStr);
        Phone phone = new Phone(phoneStr);
        Email email = new Email(emailStr);
        Address address = new Address(addrStr);
        Role role = new Role(roleStr);

        String tagsStr = norm(getCell(cells, hi.tags));
        Set<Tag> tags = new HashSet<>();
        if (!tagsStr.isEmpty()) {
            for (String tagStr : tagsStr.split("[;,]")) {
                tryParseTag(tagStr).ifPresent(tags::add);
            }
        }
        String cadenceStr = norm(getCell(cells, hi.cadence));
        String interactionsStr = norm(getCell(cells, hi.interactions));
        Cadence cadence = null;
        if (!cadenceStr.isEmpty()) {
            try {
                Matcher m = INT_FIRST.matcher(cadenceStr);
                if (m.find()) {
                    int days = Integer.parseInt(m.group(1));
                    cadence = new Cadence(days);
                } else {
                    logger.log(Level.WARNING,
                            "Ignoring invalid cadence (no integer found): " + cadenceStr);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Ignoring invalid cadence (must be integer days): "
                        + cadenceStr);
            }
        }

        List<Interaction> interactions = Collections.emptyList();
        if (!interactionsStr.isEmpty()) {
            try {
                Integer.parseInt(interactionsStr.trim());
            } catch (NumberFormatException nfe) {
                logger.log(Level.WARNING, "Ignoring invalid interactions count (must be integer): "
                        + interactionsStr);
            }
        }
        return new Person(name, phone, email, address, tags, role, cadence, interactions);
    }

    private static String norm(String s) {
        return s == null ? "" : s.replace("\uFEFF", "").trim();
    }

    private static boolean looksLikeHeader(List<String> cells) {
        boolean seenName = false;
        boolean seenPhone = false;
        boolean seenEmail = false;
        boolean seenRole = false;
        boolean seenAddress = false;
        for (String c : cells) {
            if (c == null) {
                continue;
            }
            String lc = c.trim().replace("\uFEFF", "").toLowerCase();
            if (H_NAME.equals(lc)) {
                seenName = true;
            }
            if (H_PHONE.equals(lc)) {
                seenPhone = true;
            }
            if (H_EMAIL.equals(lc)) {
                seenEmail = true;
            }
            if (H_ROLE.equals(lc)) {
                seenRole = true;
            }
            if (H_ADDRESS.equals(lc)) {
                seenAddress = true;
            }
        }
        return seenName && seenRole && seenAddress && seenPhone && seenEmail;
    }

    private static HeaderIndex mapHeader(List<String> headerCells) {
        HeaderIndex hi = new HeaderIndex();
        for (int i = 0; i < headerCells.size(); i++) {
            String h = headerCells.get(i) == null ? ""
                    : headerCells.get(i).trim().replace("\uFEFF", "").toLowerCase();
            switch (h) {
            case H_NAME:
                hi.name = i;
                break;
            case H_PHONE:
                hi.phone = i;
                break;
            case H_EMAIL:
                hi.email = i;
                break;
            case H_ADDRESS:
                hi.address = i;
                break;
            case H_TAGS:
                hi.tags = i;
                break;
            case H_ROLE:
                hi.role = i;
                break;
            case H_CADENCE:
                hi.cadence = i;
                break;
            case H_INTERACTIONS:
                hi.interactions = i;
                break;
            default:
                break;
            }
        }
        return hi;
    }

    /** Safe trimmed cell by index; empty if missing/out of range. */
    private static String getCell(List<String> cells, int idx) {
        if (idx == IDX_MISSING || idx < 0 || idx >= cells.size()) {
            return "";
        }
        String s = cells.get(idx);
        return s == null ? "" : s;
    }

    /** True if every cell is empty/whitespace. */
    private static boolean isBlankRow(List<String> cells) {
        for (String c : cells) {
            if (c != null && !c.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }


    /** Detects delimiter from a line (comma/tab/semicolon). */
    private static char detectDelimiter(String line) {
        String h = line == null ? "" : line.replace("\uFEFF", "");
        int commas = h.split(",", -1).length - 1;
        int tabs = h.split("\t", -1).length - 1;
        int semis = h.split(";", -1).length - 1;
        if (tabs > commas && tabs > semis) {
            return '\t';
        }
        if (semis > commas && semis > tabs) {
            return ';';
        }
        return ',';
    }

    /** Quote-aware split using a specific delimiter. Doubled quotes => literal quote. */
    private static List<String> splitCsvLine(String line, char delim) {
        List<String> out = new ArrayList<>();
        if (line == null) {
            out.add("");
            return out;
        }
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delim && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        int last = out.size() - 1;
        if (last >= 0) {
            String lastCell = out.get(last);
            if (!lastCell.isEmpty() && lastCell.charAt(lastCell.length() - 1) == '\r') {
                out.set(last, lastCell.substring(0, lastCell.length() - 1));
            }
        }
        return out;
    }


    /** Legacy wrapper (default to comma). */
    private static List<String> splitCsvLine(String line) {
        return splitCsvLine(line, DELIM);
    }

    /**
     * Escapes a string for CSV output.
     * <ul>
     *   <li>Doubles any double quotes ({@code " -> ""}).</li>
     *   <li>If the cell contains the delimiter, a quote, or a newline, wraps the
     *       entire cell in double quotes.</li>
     *   <li>Null values are rendered as empty strings.</li>
     * </ul>
     *
     * @param s the cell value, possibly {@code null}
     * @return an export-safe CSV cell
     */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        boolean needsQuotes = s.indexOf(DELIM) >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        String v = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }

    /**
     * Writes a header row using the current delimiter.
     *
     * @param headers ordered list of column names (nulls rendered empty)
     * @param out     the writer to append to
     * @throws IOException if writing fails
     */
    public static void writeHeader(List<String> headers, java.io.Writer out) throws IOException {
        writeRow(headers, out);
    }

    /**
     * Writes a single CSV row using the current delimiter.
     * <p>Each cell is escaped via {@link #escape(String)}. Nulls render as empty
     * strings. A newline is appended at the end.</p>
     *
     * @param cells ordered list of cell values (may contain nulls)
     * @param out   the writer to append to
     * @throws IOException if writing fails
     */
    public static void writeRow(List<String> cells, java.io.Writer out) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            sb.append(escape(cell == null ? "" : cell));
            if (i + 1 < cells.size()) {
                sb.append(DELIM);
            }
        }
        sb.append(NEWLINE);
        out.write(sb.toString());
    }
}
