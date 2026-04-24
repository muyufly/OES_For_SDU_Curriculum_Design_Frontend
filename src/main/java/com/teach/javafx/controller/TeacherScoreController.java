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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class TeacherScoreController extends ToolController {
    @FXML private ComboBox<Map> examComboBox;
    @FXML private TextField classField;
    @FXML private TextField keywordField;
    @FXML private TableView<Map> scoreTable;
    @FXML private TableColumn<Map, String> studentNumColumn;
    @FXML private TableColumn<Map, String> studentNameColumn;
    @FXML private TableColumn<Map, String> classNameColumn;
    @FXML private TableColumn<Map, String> courseColumn;
    @FXML private TableColumn<Map, String> examColumn;
    @FXML private TableColumn<Map, String> markColumn;
    @FXML private TableColumn<Map, String> gradedColumn;
    private final ObservableList<Map> scores = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentNumColumn.setCellValueFactory(new MapValueFactory<>("studentNum"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        classNameColumn.setCellValueFactory(new MapValueFactory<>("className"));
        courseColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        examColumn.setCellValueFactory(new MapValueFactory<>("examTitle"));
        markColumn.setCellValueFactory(new MapValueFactory<>("mark"));
        gradedColumn.setCellValueFactory(new MapValueFactory<>("allGraded"));
        scoreTable.setItems(scores);
        examComboBox.setCellFactory(list -> examCell());
        examComboBox.setButtonCell(examCell());
        loadExams();
        onQuery();
    }

    @Override
    public void doRefresh() {
        onQuery();
    }

    @FXML
    private void onQuery() {
        StringBuilder url = new StringBuilder("/api/teacher/scores?");
        Map exam = examComboBox.getSelectionModel().getSelectedItem();
        if (exam != null) {
            url.append("examId=").append(text(exam, "examId")).append("&");
        }
        url.append("className=").append(enc(classField.getText())).append("&keyword=").append(enc(keywordField.getText()));
        DataResponse response = HttpRequestUtil.get(url.toString());
        if (response != null && response.getCode() == 0) {
            scores.setAll((List<Map>) response.getData());
        } else {
            MessageDialog.showDialog(response == null ? "查询成绩失败" : response.getMsg());
        }
    }

    private void loadExams() {
        DataResponse response = HttpRequestUtil.get("/api/teacher/exams");
        if (response != null && response.getCode() == 0) {
            examComboBox.getItems().setAll((List<Map>) response.getData());
        }
    }

    private ListCell<Map> examCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Map item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "全部课程成绩" : text(item, "title"));
            }
        };
    }

    private String enc(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    private String text(Map map, String key) { Object value = map == null ? null : map.get(key); return value == null ? "" : value.toString(); }
}
