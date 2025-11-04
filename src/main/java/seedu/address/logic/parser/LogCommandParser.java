package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DETAILS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_INTERACTION_TYPE;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.LogCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.interaction.InteractionType;

/**
 * Parses input arguments and creates a new {@link LogCommand}.
 */
public class LogCommandParser implements Parser<LogCommand> {
    @Override
    public LogCommand parse(String args) throws ParseException {
        requireNonNull(args);

        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(args, PREFIX_INTERACTION_TYPE, PREFIX_DETAILS);

        String preamble = argMultimap.getPreamble().trim();

        if (preamble.isEmpty()) {
            throw new ParseException(ParserUtil.MESSAGE_MISSING_INDEX);
        }
        String[] parts = preamble.split("\\s+", 2);
        String idxTok = parts[0];
        if (parts.length > 1 && !parts[1].isBlank()) {
            throw new ParseException("Invalid parameter: " + parts[1]
                + ". Allowed parameters are i/<type> and d/<details>.");
        }
        boolean unsignedDigits = preamble.chars().allMatch(Character::isDigit);
        boolean signedNegative = preamble.matches("-\\d+");
        if (!(unsignedDigits || signedNegative)) {
            throw new ParseException(ParserUtil.MESSAGE_INDEX_NOT_NUMBER);
        }
        if ("0".equals(preamble) || signedNegative) {
            throw new ParseException(ParserUtil.MESSAGE_INDEX_NOT_POSITIVE);
        }

        Index index = ParserUtil.parseIndex(argMultimap.getPreamble());

        if (!argMultimap.getValue(PREFIX_INTERACTION_TYPE).isPresent()
                || !argMultimap.getValue(PREFIX_DETAILS).isPresent()) {
            throw new ParseException(LogCommand.MESSAGE_USAGE);
        }

        String typeRaw = argMultimap.getValue(PREFIX_INTERACTION_TYPE).get();
        String details = argMultimap.getValue(PREFIX_DETAILS).get();

        if (details.trim().isEmpty()) {
            throw new ParseException("Details cannot be empty. Please provide a message after d/.");
        }
        if (details.length() > 500) {
            throw new ParseException("Details too long (max 500 characters).");
        }

        InteractionType type;
        try {
            type = InteractionType.parse(typeRaw);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage());
        }
        return new LogCommand(index, type, details);
    }
}
