package com.project.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.dao.ProjektDAO;
import com.project.model.Projekt;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ProjectController {

    private static final Logger Logger = LoggerFactory.getLogger(ProjectController.class);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Zmienne do obsługi stronicowania i wyszukiwania
    private String search4;
    private Integer pageNo;
    private Integer pageSize;

    private ExecutorService wykonawca;
    private ProjektDAO projektDAO;
    private ObservableList<Projekt> projekty;

    // Komponenty GUI wstrzykiwane przez FXML
    @FXML private ChoiceBox<Integer> cbPageSizes;
    @FXML private TableView<Projekt> tblProjekt;
    @FXML private TableColumn<Projekt, Integer> colId;
    @FXML private TableColumn<Projekt, String> colNazwa;
    @FXML private TableColumn<Projekt, String> colopis;
    @FXML private TableColumn<Projekt, LocalDateTime> colDataCzasUtworzenia;
    @FXML private TableColumn<Projekt, LocalDate> colDataOddania;
    @FXML private TextField txtSzukaj;
    @FXML private Button btnDalej;
    @FXML private Button btnWstecz;
    @FXML private Button btnPierwsza;
    @FXML private Button btnOstatnia;

    // Konstruktor obligatoryjny (używany przez ControllerFactory)
    public ProjectController(ProjektDAO projektDAO) {
        this.projektDAO = projektDAO;
        this.wykonawca = Executors.newFixedThreadPool(1);
    }

    @FXML
    public void initialize() {
        // Inicjalizacja zmiennych sterujących
        search4 = "";
        pageNo = 0;
        pageSize = 10;

        // Konfiguracja ChoiceBox rozmiaru strony
        cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
        cbPageSizes.setValue(pageSize);
        cbPageSizes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                pageSize = newVal;
                pageNo = 0;
                wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
            }
        });

        // Mapowanie kolumn tabeli na pola modelu Projekt [cite: 1213-1215]
        colId.setCellValueFactory(new PropertyValueFactory<>("projektId"));
        colNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa"));
        colopis.setCellValueFactory(new PropertyValueFactory<>("opis"));
        colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<>("dataCzasUtworzenia"));
        colDataOddania.setCellValueFactory(new PropertyValueFactory<>("dataOddania"));

        // Formatowanie wyświetlania daty i czasu w tabeli [cite: 1334-1342]
        colDataCzasUtworzenia.setCellFactory(column -> new TableCell<Projekt, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(dateTimeFormater.format(item));
                }
            }
        });

        // Dodanie kolumny z przyciskami akcji (Edycja, Usuń)
        TableColumn<Projekt, Void> colEdit = new TableColumn<>("Edycja");
        colEdit.setCellFactory(column -> new TableCell<Projekt, Void>() {
            private final GridPane pane;
            {
                Button btnEdit = new Button("Edycja");
                Button btnRemove = new Button("Usuń");
                Button btnTask = new Button("Zadania");

                btnEdit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnRemove.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnTask.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                btnEdit.setOnAction(event -> edytujProjekt(getCurrentProjekt()));
                btnRemove.setOnAction(event -> usunProjekt(getCurrentProjekt()));

                pane = new GridPane();
                pane.setAlignment(Pos.CENTER);
                pane.setHgap(10);
                pane.setVgap(10);
                pane.setPadding(new Insets(5, 5, 5, 5));
                pane.add(btnTask, 0, 0);
                pane.add(btnEdit, 0, 1);
                pane.add(btnRemove, 0, 2);
            }

            private Projekt getCurrentProjekt() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        // Ustawienie szerokości kolumn i dodanie kolumny edycji
        colId.setMaxWidth(5000);
        colNazwa.setMaxWidth(10000);
        colopis.setMaxWidth(10000);
        colDataCzasUtworzenia.setMaxWidth(9000);
        colDataOddania.setMaxWidth(7000);
        colEdit.setMaxWidth(7000);
        tblProjekt.getColumns().add(colEdit);

        // Powiązanie tabeli z listą obserwowaną
        projekty = FXCollections.observableArrayList();
        tblProjekt.setItems(projekty);

        // Pierwsze ładowanie danych [cite: 1239]
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    private void loadPage(String search4, Integer pageNo, Integer pageSize) {
        try {
            final List<Projekt> projektList = new ArrayList<>();
            // Logika wyszukiwania oparta na wyrażeniach regularnych
            if (search4 != null && !search4.isEmpty()) {
                if (search4.matches("[0-9]+")) {
                    Projekt p = projektDAO.getProjekt(Integer.parseInt(search4));
                    if (p != null) projektList.add(p);
                } else if (search4.matches("^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) {
                    projektList.addAll(projektDAO.getProjektyWhereDataOddaniaIs(LocalDate.parse(search4), pageNo * pageSize, pageSize));
                } else {
                    projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, pageNo * pageSize, pageSize));
                }
            } else {
                projektList.addAll(projektDAO.getProjekty(pageNo * pageSize, pageSize));
            }

            Platform.runLater(() -> {
                projekty.clear();
                projekty.addAll(projektList);
            });
        } catch (RuntimeException e) {
            String errMsg = "Błąd podczas pobierania listy projektów.";
            Logger.error(errMsg, e);
            Platform.runLater(() -> showError(errMsg, e.getMessage()));
        }
    }

    private void edytujProjekt(Projekt projekt) {
        Dialog<Projekt> dialog = new Dialog<>();
        dialog.setTitle("Edycja");
        dialog.setHeaderText(projekt.getProjektId() != null ? "Edycja danych projektu" : "Dodawanie projektu");
        dialog.setResizable(true);

        // Budowa formularza w GridPane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));

        TextField txtNazwa = new TextField(projekt.getNazwa() != null ? projekt.getNazwa() : "");
        TextArea txtOpis = new TextArea(projekt.getOpis() != null ? projekt.getOpis() : "");
        txtOpis.setPrefRowCount(6);
        txtOpis.setWrapText(true);

        DatePicker dtDataOddania = new DatePicker(projekt.getDataOddania());
        dtDataOddania.setPromptText("RRRR-MM-DD");

        grid.add(getRightLabel("Id: "), 0, 0);
        grid.add(new Label(projekt.getProjektId() != null ? projekt.getProjektId().toString() : ""), 1, 0);
        grid.add(getRightLabel("Nazwa: "), 0, 2);
        grid.add(txtNazwa, 1, 2);
        grid.add(getRightLabel("Opis: "), 0, 3);
        grid.add(txtOpis, 1, 3);
        grid.add(getRightLabel("Data oddania: "), 0, 4);
        grid.add(dtDataOddania, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, ButtonType.CANCEL);

        dialog.setResultConverter(b -> {
            if (b == buttonTypeOk) {
                projekt.setNazwa(txtNazwa.getText().trim());
                projekt.setOpis(txtOpis.getText().trim());
                projekt.setDataOddania(dtDataOddania.getValue());
                return projekt;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            wykonawca.execute(() -> {
                try {
                    projektDAO.setProjekt(result);
                    Platform.runLater(() -> {
                        if (!projekty.contains(result)) projekty.add(0, result);
                        tblProjekt.refresh();
                    });
                } catch (RuntimeException e) {
                    Logger.error("Błąd podczas zapisywania danych projektu!", e);
                    Platform.runLater(() -> showError("Błąd zapisu", e.getMessage()));
                }
            });
        });
    }

    private void usunProjekt(Projekt projekt) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Potwierdzenie");
        alert.setHeaderText("Czy na pewno chcesz usunąć projekt: " + projekt.getNazwa() + "?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                wykonawca.execute(() -> {
                    try {
                        projektDAO.deleteProjekt(projekt.getProjektId());
                        Platform.runLater(() -> projekty.remove(projekt));
                    } catch (RuntimeException e) {
                        Logger.error("Błąd podczas usuwania projektu!", e);
                        Platform.runLater(() -> showError("Błąd usuwania", e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    private void onActionBtnSzukaj(ActionEvent event) {
        search4 = txtSzukaj.getText().trim();
        pageNo = 0;
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnDalej(ActionEvent event) {
        pageNo++;
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
    }

    @FXML
    private void onActionBtnWstecz(ActionEvent event) {
        if (pageNo > 0) {
            pageNo--;
            wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
        }
    }

    @FXML private void onActionBtnDodaj(ActionEvent event) { edytujProjekt(new Projekt()); }
    @FXML private void onActionBtnPierwsza(ActionEvent event) { pageNo = 0; wykonawca.execute(() -> loadPage(search4, pageNo, pageSize)); }

    private Label getRightLabel(String text) {
        Label lbl = new Label(text);
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        return lbl;
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (wykonawca != null) {
            wykonawca.shutdown();
            try {
                if (!wykonawca.awaitTermination(5, TimeUnit.SECONDS)) wykonawca.shutdownNow();
            } catch (InterruptedException e) {
                wykonawca.shutdownNow();
            }
        }
    }
}