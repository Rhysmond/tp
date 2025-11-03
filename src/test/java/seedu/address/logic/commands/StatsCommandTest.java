package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Unit tests for {@link StatsCommand}.
 * Tests include:
 * - typical tag counting
 * - handling of no tags
 * - sorting by frequency then alphabetically
 * - case-insensitive tag merging
 */
public class StatsCommandTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(new AddressBook(), new UserPrefs());
    }

    @Test
    public void execute_typicalTags_success() {
        // friends (3), owesMoney (1)
        Person alice = new PersonBuilder().withName("Alice").withTags("friends").build();
        Person benson = new PersonBuilder().withName("Benson").withTags("friends", "owesMoney").build();
        Person daniel = new PersonBuilder().withName("Daniel").withTags("friends").build();

        Arrays.asList(alice, benson, daniel).forEach(model::addPerson);

        String expectedMessage = String.join("\n",
                "Tag stats:",
                "• friends: 3",
                "• owesMoney: 1");

        CommandResult result = new StatsCommand().execute(model);
        assertEquals(expectedMessage, result.getFeedbackToUser());
    }

    @Test
    public void execute_noTagsAnywhere_showsNoTagsMessage() {
        Person a = new PersonBuilder().withName("A").withTags().build();
        Person b = new PersonBuilder().withName("B").withTags().build();
        model.addPerson(a);
        model.addPerson(b);

        CommandResult result = new StatsCommand().execute(model);
        assertEquals("No tags found on any contact.", result.getFeedbackToUser());
    }

    @Test
    public void execute_countsAndOrdering_respectsSortRules() {
        // friends: 4, alpha:1, owesMoney:1
        Person p1 = new PersonBuilder().withName("P1").withTags("friends").build();
        Person p2 = new PersonBuilder().withName("P2").withTags("friends").build();
        Person p3 = new PersonBuilder().withName("P3").withTags("friends").build();
        Person p4 = new PersonBuilder().withName("P4").withTags("friends").build();
        Person p5 = new PersonBuilder().withName("P5").withTags("owesMoney").build();
        Person p6 = new PersonBuilder().withName("P6").withTags("alpha").build();

        Arrays.asList(p1, p2, p3, p4, p5, p6).forEach(model::addPerson);

        String expectedMessage = String.join("\n",
                "Tag stats:",
                "• friends: 4",
                "• alpha: 1",
                "• owesMoney: 1");

        CommandResult result = new StatsCommand().execute(model);
        assertEquals(expectedMessage, result.getFeedbackToUser());
    }

    @Test
    public void execute_caseInsensitiveTags_mergedCorrectly() {
        // "Friends", "friends", "FRIENDS" should merge → count = 3, display first variant
        Person a = new PersonBuilder().withName("Case A").withTags("Friends").build();
        Person b = new PersonBuilder().withName("Case B").withTags("friends").build();
        Person c = new PersonBuilder().withName("Case C").withTags("FRIENDS").build();

        Arrays.asList(a, b, c).forEach(model::addPerson);

        String expectedMessage = String.join("\n",
                "Tag stats:",
                "• Friends: 3");

        CommandResult result = new StatsCommand().execute(model);
        assertEquals(expectedMessage, result.getFeedbackToUser());
    }
}
