package seedu.address.ui;

import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Person;

/**
 * Panel containing the list of persons.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(PersonListPanel.class);

    @FXML
    private ListView<Person> personListView;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> personList) {
        super(FXML);
        personListView.setItems(personList);
        personListView.setCellFactory(listView -> new PersonListViewCell());
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);

            if (empty || person == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new PersonCard(person, getIndex() + 1).getRoot());
            }
        }
    }

    /**
     * Expose the underlying ListView for selection binding.
     */
    public ListView<Person> getListView() {
        return personListView;
    }

    /**
     * Convenience accessor for selection model.
     */
    public javafx.scene.control.MultipleSelectionModel<Person> getSelectionModel() {
        return personListView.getSelectionModel();
    }

    /**
     * Moves the contact selection by {@code delta} (clamped to list bounds) and scrolls the new row into view.
     */
    public void moveSelection(int delta) {
        SelectionModel<Person> sm = personListView.getSelectionModel();
        int i = Math.max(0, sm.getSelectedIndex());
        int n = personListView.getItems().size();
        int j = Math.max(0, Math.min(n - 1, i + delta));
        if (j != i) {
            sm.select(j);
            personListView.scrollTo(j);
        }
    }

}
