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

public class StudentScoreController extends ToolController {
    @FXML private TableView<Map> courseScoreTable;
    @FXML private TableColumn<Map, String> courseNameColumn;
    @FXML private TableColumn<Map, String> markColumn;
    @FXML private TableView<Map> examScoreTable;
    @FXML private TableColumn<Map, String> examTitleColumn;
    @FXML private TableColumn<Map, String> examScoreColumn;
    @FXML private TableColumn<Map, String> examGradedColumn;
    @FXML private TableView<Map> resultTable;
    @FXML private TableColumn<Map, String> resultQuestionColumn;
    @FXML private TableColumn<Map, String> resultAnswerColumn;
    @FXML private TableColumn<Map, String> resultScoreColumn;
    private final ObservableList<Map> courseScores = FXCollections.observableArrayList();
    private final ObservableList<Map> examScores = FXCollections.observableArrayList();
    private final ObservableList<Map> results = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        courseNameColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        markColumn.setCellValueFactory(new MapValueFactory<>("mark"));
        examTitleColumn.setCellValueFactory(new MapValueFactory<>("examTitle"));
        examScoreColumn.setCellValueFactory(new MapValueFactory<>("totalScore"));
        examGradedColumn.setCellValueFactory(new MapValueFactory<>("allGraded"));
        resultQuestionColumn.setCellValueFactory(new MapValueFactory<>("content"));
        resultAnswerColumn.setCellValueFactory(new MapValueFactory<>("answer"));
        resultScoreColumn.setCellValueFactory(new MapValueFactory<>("score"));
        courseScoreTable.setItems(courseScores);
        examScoreTable.setItems(examScores);
        resultTable.setItems(results);
        examScoreTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> loadResult(row));
        doRefresh();
    }

    @Override
    public void doRefresh() {
        DataResponse response = HttpRequestUtil.get("/api/student/scores");
        if (response != null && response.getCode() == 0 && response.getData() instanceof Map) {
            Map data = (Map) response.getData();
            courseScores.setAll((List<Map>) data.get("courseScores"));
            examScores.setAll((List<Map>) data.get("examScores"));
        } else {
            MessageDialog.showDialog(response == null ? "加载成绩失败" : response.getMsg());
        }
    }

    private void loadResult(Map examScore) {
        Integer examId = intValue(examScore, "examId");
        if (examId == null) return;
        DataResponse response = HttpRequestUtil.get("/api/student/exams/" + examId + "/result");
        if (response != null && response.getCode() == 0 && response.getData() instanceof Map) {
            Map data = (Map) response.getData();
            results.setAll((List<Map>) data.get("questions"));
        }
    }

    private String text(Map map, String key) { Object value = map == null ? null : map.get(key); return value == null ? "" : value.toString(); }
    private Integer intValue(Map map, String key) { try { String v = text(map, key); return v.isBlank() ? null : (int) Double.parseDouble(v); } catch (Exception e) { return null; } }
}
