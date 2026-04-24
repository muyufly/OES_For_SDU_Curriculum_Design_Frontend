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

import java.util.*;

public class StudentExamController extends ToolController {
    @FXML private TableView<Map> examTable;
    @FXML private TableColumn<Map, String> examTitleColumn;
    @FXML private TableColumn<Map, String> courseColumn;
    @FXML private TableColumn<Map, String> endTimeColumn;
    @FXML private TableColumn<Map, String> submittedColumn;
    @FXML private TableView<Map> questionTable;
    @FXML private TableColumn<Map, String> questionTypeColumn;
    @FXML private TableColumn<Map, String> questionContentColumn;
    @FXML private TableColumn<Map, String> questionScoreColumn;
    @FXML private TextArea questionArea;
    @FXML private TextArea answerArea;
    private final ObservableList<Map> exams = FXCollections.observableArrayList();
    private final ObservableList<Map> questions = FXCollections.observableArrayList();
    private final Map<Integer, String> answers = new HashMap<>();

    @FXML
    public void initialize() {
        examTitleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        courseColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        endTimeColumn.setCellValueFactory(new MapValueFactory<>("endTime"));
        submittedColumn.setCellValueFactory(new MapValueFactory<>("submitted"));
        questionTypeColumn.setCellValueFactory(new MapValueFactory<>("questionType"));
        questionContentColumn.setCellValueFactory(new MapValueFactory<>("content"));
        questionScoreColumn.setCellValueFactory(new MapValueFactory<>("score"));
        examTable.setItems(exams);
        questionTable.setItems(questions);
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, row) -> showQuestion(row));
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
    private void onLoadQuestions() {
        Map exam = examTable.getSelectionModel().getSelectedItem();
        Integer examId = intValue(exam, "examId");
        if (examId == null) {
            MessageDialog.showDialog("请先选择试卷");
            return;
        }
        DataResponse response = HttpRequestUtil.get("/api/student/exams/" + examId + "/questions");
        if (response != null && response.getCode() == 0 && response.getData() instanceof Map) {
            Map data = (Map) response.getData();
            questions.setAll((List<Map>) data.get("questions"));
            answers.clear();
            if (!questions.isEmpty()) {
                questionTable.getSelectionModel().selectFirst();
            }
        } else {
            MessageDialog.showDialog(response == null ? "获取题目失败" : response.getMsg());
        }
    }

    @FXML
    private void onSaveAnswer() {
        Map question = questionTable.getSelectionModel().getSelectedItem();
        Integer questionId = intValue(question, "questionId");
        if (questionId != null) {
            answers.put(questionId, answerArea.getText());
            MessageDialog.showDialog("当前题答案已暂存");
        }
    }

    @FXML
    private void onSubmitExam() {
        onSaveAnswer();
        Map exam = examTable.getSelectionModel().getSelectedItem();
        Integer examId = intValue(exam, "examId");
        if (examId == null || questions.isEmpty()) {
            MessageDialog.showDialog("请先获取试卷题目");
            return;
        }
        List<Map<String, Object>> answerList = new ArrayList<>();
        for (Map question : questions) {
            Integer questionId = intValue(question, "questionId");
            Map<String, Object> item = new HashMap<>();
            item.put("questionId", questionId);
            item.put("answer", answers.getOrDefault(questionId, ""));
            answerList.add(item);
        }
        DataRequest request = new DataRequest();
        request.add("answers", answerList);
        DataResponse response = HttpRequestUtil.request("/api/student/exams/" + examId + "/submit", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("提交成功");
            doRefresh();
        } else {
            MessageDialog.showDialog(response == null ? "提交失败" : response.getMsg());
        }
    }

    private void showQuestion(Map question) {
        if (question == null) return;
        Integer previousId = intValue(questionTable.getSelectionModel().getSelectedItem(), "questionId");
        questionArea.setText(buildQuestionText(question));
        Integer questionId = intValue(question, "questionId");
        answerArea.setText(answers.getOrDefault(questionId, ""));
    }

    private String buildQuestionText(Map question) {
        StringBuilder builder = new StringBuilder(text(question, "content"));
        if ("CHOICE".equals(text(question, "questionType"))) {
            builder.append("\nA. ").append(text(question, "optionA"))
                    .append("\nB. ").append(text(question, "optionB"))
                    .append("\nC. ").append(text(question, "optionC"))
                    .append("\nD. ").append(text(question, "optionD"));
        }
        return builder.toString();
    }

    private String text(Map map, String key) { Object value = map == null ? null : map.get(key); return value == null ? "" : value.toString(); }
    private Integer intValue(Map map, String key) { try { String v = text(map, key); return v.isBlank() ? null : (int) Double.parseDouble(v); } catch (Exception e) { return null; } }
}
