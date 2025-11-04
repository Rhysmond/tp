package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.interaction.Interaction;
import seedu.address.model.interaction.InteractionType;
import seedu.address.model.person.Person;

/** Adds an interaction entry to a person's history. */
public class LogCommand extends Command {
    public static final String COMMAND_WORD = "log";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Log an interaction for a contact.\n"
            + "Parameters: INDEX (must be a positive integer) "
            + "i/TYPE(call/email/meeting/note) d/DETAILS\n"
            + "Example: " + COMMAND_WORD + " 2 i/email d/Sent the deck and pricing";

    public static final String MESSAGE_SUCCESS = "Added %s to %s";

    private final Index index;
    private final InteractionType type;
    private final String details;

    /** Constructs a {@code LogCommand} to append an interaction to the person at {@code index}. */
    public LogCommand(Index index, InteractionType type, String details) {
        this.index = index;
        this.type = type;
        this.details = details;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();

        if (index.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        Person target = lastShownList.get(index.getZeroBased());
        List<Interaction> newHistory = new ArrayList<>(target.getInteractions());
        newHistory.add(new Interaction(type, details, Instant.now()));

        Person updated = new Person(target, newHistory);
        model.setPerson(target, updated);

        return new CommandResult(String.format(MESSAGE_SUCCESS, type, target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof LogCommand)) {
            return false;
        }
        LogCommand o = (LogCommand) other;
        return index.equals(o.index) && type.equals(o.type) && details.equals(o.details);
    }
}
