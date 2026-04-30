package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;

import java.util.List;
import java.util.Map;

public class TeacherStatsController extends ToolController {
    @FXML private ComboBox<Map> examComboBox;
    @FXML private Label summaryLabel;
    @FXML private TableView<Map> statsTable;
    @FXML private TableColumn<Map, String> studentColumn;
    @FXML private TableColumn<Map, String> classColumn;
    @FXML private TableColumn<Map, String> statusColumn;
    @FXML private TableColumn<Map, String> scoreColumn;
    @FXML private TableColumn<Map, String> gradedColumn;
    private final ObservableList<Map> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        classColumn.setCellValueFactory(new MapValueFactory<>("className"));
        statusColumn.setCellValueFactory(new MapValueFactory<>("studentExamStatus"));
        scoreColumn.setCellValueFactory(new MapValueFactory<>("totalScore"));
        gradedColumn.setCellValueFactory(new MapValueFactory<>("allGraded"));
        statsTable.setItems(rows);
        examComboBox.setCellFactory(list -> examCell());
        examComboBox.setButtonCell(examCell());
        examComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadStats());
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
    private void loadStats() {
        Map exam = examComboBox.getSelectionModel().getSelectedItem();
        Integer examId = intValue(exam, "examId");
        if (examId == null) return;
        DataResponse response = HttpRequestUtil.get("/api/teacher/exams/" + examId + "/stats");
        if (response != null && response.getCode() == 0 && response.getData() instanceof Map) {
            Map data = (Map) response.getData();
            summaryLabel.setText("平均分 " + text(data, "averageScore")
                    + " | 最高 " + text(data, "maxScore")
                    + " | 最低 " + text(data, "minScore")
                    + " | 已交 " + text(data, "submittedCount")
                    + " | 未交 " + text(data, "notSubmittedCount")
                    + " | 草稿 " + text(data, "draftCount")
                    + " | 待批改 " + text(data, "pendingGradeCount"));
            rows.setAll((List<Map>) data.get("rows"));
        } else {
            MessageDialog.showDialog(response == null ? "加载统计失败" : response.getMsg());
        }
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
    private Integer intValue(Map map, String key) { try { String v = text(map, key); return v.isBlank() ? null : (int) Double.parseDouble(v); } catch (Exception e) { return null; } }
}
