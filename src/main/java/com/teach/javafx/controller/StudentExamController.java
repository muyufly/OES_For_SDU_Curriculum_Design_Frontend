package com.teach.javafx.controller;

import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class StudentExamController extends ToolController {
    @FXML private TableView<Map> examTable;
    @FXML private TableColumn<Map, String> examTitleColumn;
    @FXML private TableColumn<Map, String> courseColumn;
    @FXML private TableColumn<Map, String> endTimeColumn;
    @FXML private TableColumn<Map, String> statusColumn;
    @FXML private TableColumn<Map, String> remainingColumn;

    private final ObservableList<Map> exams = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        examTitleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        courseColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        endTimeColumn.setCellValueFactory(new MapValueFactory<>("endTime"));
        statusColumn.setCellValueFactory(new MapValueFactory<>("studentExamStatus"));
        remainingColumn.setCellValueFactory(new MapValueFactory<>("remainingSeconds"));
        examTable.setItems(exams);
        doRefresh();
    }

    @Override
    public void doRefresh() {
        DataResponse response = HttpRequestUtil.get("/api/student/exams");
        if (response != null && response.getCode() == 0) {
            exams.setAll((List<Map>) response.getData());
        } else {
            MessageDialog.showDialog(response == null ? "加载试卷失败" : response.getMsg());
        }
    }

    @FXML
    private void onEnterExam() {
        Map exam = examTable.getSelectionModel().getSelectedItem();
        Integer examId = intValue(exam, "examId");
        if (examId == null) {
            MessageDialog.showDialog("请先选择试卷");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("student-exam-window.fxml"));
            Scene scene = new Scene(loader.load(), 1180, 760);
            Stage stage = new Stage();
            stage.initOwner(MainApplication.getMainStage());
            stage.initModality(Modality.NONE);
            stage.setTitle("OES 考试界面");
            stage.setScene(scene);
            StudentExamWindowController controller = loader.getController();
            controller.setStage(stage);
            controller.startExam(examId);
            stage.setOnHidden(event -> doRefresh());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("打开考试界面失败");
        }
    }

    private String text(Map map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : value.toString();
    }

    private Integer intValue(Map map, String key) {
        try {
            String value = text(map, key);
            return value.isBlank() ? null : (int) Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }
}
