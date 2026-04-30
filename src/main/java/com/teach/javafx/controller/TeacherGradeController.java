package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;

import java.util.List;
import java.util.Map;

public class TeacherGradeController extends ToolController {
    @FXML private ComboBox<Map> examComboBox;
    @FXML private TableView<Map> attemptTable;
    @FXML private TableColumn<Map, String> studentColumn;
    @FXML private TableColumn<Map, String> classColumn;
    @FXML private TableColumn<Map, String> totalColumn;
    @FXML private TableColumn<Map, String> allGradedColumn;
    @FXML private TableView<Map> recordTable;
    @FXML private TableColumn<Map, String> questionColumn;
    @FXML private TableColumn<Map, String> typeColumn;
    @FXML private TableColumn<Map, String> scoreColumn;
    @FXML private TextArea questionArea;
    @FXML private TextArea answerArea;
    @FXML private TextField scoreField;

    private final ObservableList<Map> attempts = FXCollections.observableArrayList();
    private final ObservableList<Map> records = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        classColumn.setCellValueFactory(new MapValueFactory<>("className"));
        totalColumn.setCellValueFactory(new MapValueFactory<>("totalScore"));
        allGradedColumn.setCellValueFactory(new MapValueFactory<>("allGraded"));
        questionColumn.setCellValueFactory(new MapValueFactory<>("questionContent"));
        typeColumn.setCellValueFactory(new MapValueFactory<>("questionType"));
        scoreColumn.setCellValueFactory(new MapValueFactory<>("score"));
        attemptTable.setItems(attempts);
        recordTable.setItems(records);
        examComboBox.setCellFactory(list -> examCell());
        examComboBox.setButtonCell(examCell());
        examComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadAttempts());
        attemptTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> loadRecords(row));
        recordTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> showRecord(row));
        doRefresh();
    }

    @Override
    public void doRefresh() {
        DataResponse response = HttpRequestUtil.get("/api/teacher/exams");
        if (response != null && response.getCode() == 0) {
            examComboBox.getItems().setAll((List<Map>) response.getData());
            if (!examComboBox.getItems().isEmpty()) {
                examComboBox.getSelectionModel().selectFirst();
            }
        }
    }

    @FXML
    private void loadAttempts() {
        Map exam = examComboBox.getSelectionModel().getSelectedItem();
        Integer examId = intValue(exam, "examId");
        if (examId == null) return;
        DataResponse response = HttpRequestUtil.get("/api/teacher/exams/" + examId + "/ended-attempts");
        if (response != null && response.getCode() == 0) {
            attempts.setAll((List<Map>) response.getData());
            records.clear();
        } else {
            MessageDialog.showDialog(response == null ? "加载已结束试卷失败" : response.getMsg());
        }
    }

    private void loadRecords(Map attempt) {
        Integer attemptId = intValue(attempt, "attemptId");
        if (attemptId == null) return;
        DataResponse response = HttpRequestUtil.get("/api/teacher/attempts/" + attemptId + "/records");
        if (response != null && response.getCode() == 0) {
            records.setAll((List<Map>) response.getData());
        } else {
            MessageDialog.showDialog(response == null ? "加载答题记录失败" : response.getMsg());
        }
    }

    @FXML
    private void onGrade() {
        Map row = recordTable.getSelectionModel().getSelectedItem();
        Integer recordId = intValue(row, "recordId");
        if (recordId == null) {
            MessageDialog.showDialog("请先选择答题记录");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("score", parseInt(scoreField.getText()));
        DataResponse response = HttpRequestUtil.request("/api/teacher/records/" + recordId + "/grade", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("批改完成");
            loadRecords(attemptTable.getSelectionModel().getSelectedItem());
            loadAttempts();
        } else {
            MessageDialog.showDialog(response == null ? "批改失败" : response.getMsg());
        }
    }

    private void showRecord(Map row) {
        questionArea.setText(text(row, "questionContent"));
        answerArea.setText(text(row, "answer"));
        scoreField.setText(text(row, "score"));
        boolean read = "READ".equals(text(row, "questionType"));
        scoreField.setEditable(read);
    }

    private ListCell<Map> examCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Map item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : text(item, "title"));
            }
        };
    }

    private String text(Map map, String key) { Object value = map == null ? null : map.get(key); return value == null ? "" : value.toString(); }
    private Integer intValue(Map map, String key) { return parseInt(text(map, key)); }
    private Integer parseInt(String value) { try { return value == null || value.isBlank() ? null : (int) Double.parseDouble(value); } catch (Exception e) { return null; } }
}
