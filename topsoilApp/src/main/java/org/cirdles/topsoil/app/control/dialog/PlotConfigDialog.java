package org.cirdles.topsoil.app.control.dialog;

import javafx.beans.binding.Bindings;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.cirdles.topsoil.Variable;
import org.cirdles.topsoil.app.ProjectManager;
import org.cirdles.topsoil.app.Topsoil;
import org.cirdles.topsoil.app.control.FXMLUtils;
import org.cirdles.topsoil.app.data.FXDataTable;
import org.cirdles.topsoil.app.data.TopsoilProject;
import org.cirdles.topsoil.data.DataColumn;
import org.cirdles.topsoil.plot.Plot;
import org.cirdles.topsoil.plot.PlotOption;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlotConfigDialog extends Dialog<Map<Variable<?>, DataColumn<?>>> {

    public PlotConfigDialog(FXDataTable table, Map<Variable<?>, DataColumn<?>> variableMap) {
        PlotConfigDialogPane dialogPane = new PlotConfigDialogPane(table, variableMap);
        this.setDialogPane(dialogPane);
        this.setTitle("Variable Selection");

        Stage stage = (Stage) this.getDialogPane().getScene().getWindow();
        stage.getIcons().addAll(Topsoil.getLogo());
        stage.setOnShown(event -> stage.requestFocus());
        stage.setResizable(true);

        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.APPLY) {
                return dialogPane.getSelections();
            }
            return null;
        });
    }

    private class PlotConfigDialogPane extends DialogPane {

        private static final String CONTROLLER_FXML = "plot-config-dialog-pane.fxml";

        private FXDataTable table;
        private MapProperty<Variable<?>, DataColumn<?>> selections = new SimpleMapProperty<>(FXCollections.observableHashMap());
        private Map<Variable<?>, SelectionEntry> selectionEntries = new HashMap<>();

        @FXML private TreeView<DataColumn<?>> columnTreeView;
        @FXML private ListView<SelectionEntry> variableListView;
        @FXML private Button removeButton, useExistingButton;

        public PlotConfigDialogPane(FXDataTable table, Map<Variable<?>, DataColumn<?>> variableMap) {
            this.table = table;
            try {
                FXMLUtils.loadController(CONTROLLER_FXML, PlotConfigDialogPane.class, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @FXML
        public void initialize() {
            TreeItem<DataColumn<?>> rootItem = new TreeItem<>();
            columnTreeView.setRoot(rootItem);
            columnTreeView.setShowRoot(false);
            columnTreeView.setCellFactory(param -> new ColumnTreeViewCell());
            for (DataColumn<?> column : table.getColumns()) {
                rootItem.getChildren().add(createColumnItem(column));
            }

            variableListView.setCellFactory(param -> new ListCell<SelectionEntry>() {
                    @Override
                    protected void updateItem(SelectionEntry item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(String.format(
                                    "%s => %s",
                                    item.variable.getAbbreviation(),
                                    item.column.getTitle()
                            ));
                        } else {
                            setText("");
                        }
                    }
            });

            selections.addListener((MapChangeListener<Variable<?>, DataColumn<?>>) c -> {
                SelectionEntry selectionEntry = selectionEntries.get(c.getKey());
                Variable<?> variable = c.getKey();
                DataColumn<?> column;
                if (c.wasAdded()) {
                    column = c.getValueAdded();
                    if (selectionEntry == null) {
                        selectionEntry = new SelectionEntry(variable, column);
                        selectionEntries.put(variable, selectionEntry);
                        variableListView.getItems().add(selectionEntry);
                    }
                    if (column == null) {
//                        c.getMap().remove(variable);
                        selectionEntries.remove(variable);
                        variableListView.getItems().remove(selectionEntry);
                    } else {
                        selectionEntry.column = column;
                    }
                }
                if (c.wasRemoved()) {
                    if (c.getMap().get(variable) == null) {
                        selectionEntries.remove(variable);
                        variableListView.getItems().remove(selectionEntry);
                    }
                }
                variableListView.refresh();
            });

            removeButton.disableProperty().bind(
                    Bindings.createBooleanBinding(
                            () -> variableListView.getSelectionModel().getSelectedItems().isEmpty(),
                            variableListView.getSelectionModel().selectedItemProperty()
                    )
            );

            useExistingButton.disableProperty().bind(ProjectManager.getProject().getPlotMap().get(table).emptyProperty());
        }

        Map<Variable<?>, DataColumn<?>> getSelections() {
            return selections;
        }

        void setSelections(Map<Variable<?>, DataColumn<?>> variableMap) {
            selections.clear();
            selections.putAll(variableMap);
        }

        //**********************************************//
        //                PRIVATE METHODS               //
        //**********************************************//

        private void select(Variable<?> variable, DataColumn<?> column) {
            selections.put(variable, column);
        }

        private void deselect(Variable<?> variable) {
            selections.remove(variable);
        }

        private TreeItem<DataColumn<?>> createColumnItem(DataColumn<?> column) {
            TreeItem<DataColumn<?>> treeItem = new TreeItem<>(column);
            if (column.countChildren() > 0) {
                for (DataColumn<?> child : column.getChildren()) {
                    treeItem.getChildren().add(createColumnItem(child));
                }
            }
            return treeItem;
        }

        @FXML
        private void removeButtonAction() {
            SelectionEntry entry = variableListView.getSelectionModel().getSelectedItem();
            deselect(entry.variable);
        }

        @FXML
        private void useExistingButtonAction() {
            Map<Variable<?>, DataColumn<?>> newSelections =
                    new ExistingPlotDialog(ProjectManager.getProject(), table).showAndWait().orElse(null);
            if (newSelections != null) {
                setSelections(newSelections);
            }
        }

        private class SelectionEntry {
            Variable<?> variable;
            DataColumn<?> column;

            SelectionEntry(Variable<?> variable, DataColumn<?> column) {
                this.variable = variable;
                this.column = column;
            }
        }

        private class ColumnTreeViewCell extends TreeCell<DataColumn<?>> {

            ColumnTreeViewCell() {
                super();
                this.setContentDisplay(ContentDisplay.RIGHT);
            }

            @Override
            protected void updateItem(DataColumn<?> item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    if (item.countChildren() == 0) {
                        setText("");
                        setGraphic(makeLeafColumnGraphic(item));
                    } else {
                        setText(item.getTitle());
                        setGraphic(null);
                    }
                    setStyle((getIndex() % 2 == 1) ? "-fx-background-color: #eee" : "-fx-background-color: #fff");
                } else {
                    setText("");
                    setGraphic(null);
                }
            }

            private Node makeLeafColumnGraphic(DataColumn<?> column) {
                double labelWidth = 120.0;
                Label label = new Label(column.getTitle());
                label.setMinWidth(labelWidth);
                label.setMaxWidth(labelWidth);
                label.setWrapText(true);

                ComboBox<Variable<?>> variableComboBox = new ComboBox<>(FXCollections.observableList(Variable.NUMBER_TYPE));
                variableComboBox.setCellFactory(param ->  new ListCell<Variable<?>>() {
                        @Override
                        protected void updateItem(Variable<?> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null) {
                                setText(item.getAbbreviation());
                            } else {
                                setText("");
                            }
                        }
                });
                variableComboBox.setConverter(new StringConverter<Variable<?>>() {
                    @Override
                    public String toString(Variable<?> object) {
                        return object.getAbbreviation();
                    }

                    @Override
                    public Variable<?> fromString(String string) {
                        return Variable.variableForAbbreviation(string);
                    }
                });

                Button setButton = new Button("Set");
                setButton.disableProperty().bind(variableComboBox.valueProperty().isNull());
                setButton.setOnAction(event -> {
                    Variable<?> variable = variableComboBox.getValue();
                    select(variable, column);
                });

                HBox hBox = new HBox(label, variableComboBox, setButton);
                hBox.setSpacing(5.0);
                return hBox;
            }

        }

        private class ExistingPlotDialog extends Dialog<Map<Variable<?>, DataColumn<?>>> {

            public ExistingPlotDialog(TopsoilProject project, FXDataTable table) {

                ListView<Plot> plotListView = new ListView<>();
                plotListView.setCellFactory(param -> new ListCell<Plot>() {
                    @Override
                    protected void updateItem(Plot item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((item != null) ? String.valueOf(item.getOptions().get(PlotOption.TITLE)) : "");
                    }
                });
                plotListView.getItems().addAll(project.getPlotMap().get(table));
                plotListView.setPrefSize(300.0, 200.0);

                DialogPane dialogPane = this.getDialogPane();
                dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.APPLY);
                dialogPane.lookupButton(ButtonType.APPLY).disableProperty().bind(
                        Bindings.createBooleanBinding(
                                () -> plotListView.getSelectionModel().getSelectedItems().isEmpty(),
                                plotListView.getSelectionModel().selectedItemProperty()
                        )
                );
                dialogPane.setContent(plotListView);

                Stage stage = (Stage) dialogPane.getScene().getWindow();
                stage.getIcons().add(Topsoil.getLogo());
                stage.setTitle("Use Existing Plot Config");
                this.initOwner(PlotConfigDialog.this.getDialogPane().getScene().getWindow());

                this.setResultConverter(buttonType -> {
                    if (buttonType == ButtonType.APPLY) {
                        return plotListView.getSelectionModel().getSelectedItem().getVariableMap();
                    }
                    return null;
                });
            }

        }

    }
}
