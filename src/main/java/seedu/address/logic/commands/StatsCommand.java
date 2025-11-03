package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javafx.collections.ObservableList;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/** Shows counts of people per tag. */
public class StatsCommand extends Command {
    public static final String COMMAND_WORD = "stats";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Shows the number of people in each tag.\n"
            + "Example: " + COMMAND_WORD;

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);

        ObservableList<Person> people = model.getAddressBook().getPersonList();
        if (people.isEmpty()) {
            return new CommandResult("No contacts in the address book.");
        }

        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> displayNames = new HashMap<>();

        for (Person p : people) {
            for (Tag t : p.getTags()) {
                String normalized = t.tagName.toLowerCase();
                displayNames.putIfAbsent(normalized, t.tagName);
                counts.merge(normalized, 1, Integer::sum);
            }
        }

        if (counts.isEmpty()) {
            return new CommandResult("No tags found on any contact.");
        }

        // Sort by descending count, then alphabetical
        List<Map.Entry<String, Integer>> entries = counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(e -> displayNames.get(e.getKey()).toLowerCase()))
                .toList();

        StringJoiner sj = new StringJoiner("\n");
        sj.add("Tag stats:");
        for (Map.Entry<String, Integer> e : entries) {
            String display = displayNames.get(e.getKey());
            sj.add(String.format("• %s: %d", display, e.getValue()));
        }
        return new CommandResult(sj.toString());
    }
}
