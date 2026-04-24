package com.teach.javafx.controller;

import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherExamController extends ToolController {
    @FXML private TableView<Map> examTable;
    @FXML private TableColumn<Map, String> examIdColumn;
    @FXML private TableColumn<Map, String> examTitleColumn;
    @FXML private TableColumn<Map, String> examCourseColumn;
    @FXML private TableColumn<Map, String> examStatusColumn;
    @FXML private TableView<Map> questionTable;
    @FXML private TableColumn<Map, String> questionIdColumn;
    @FXML private TableColumn<Map, String> questionTypeColumn;
    @FXML private TableColumn<Map, String> questionContentColumn;
    @FXML private TableColumn<Map, String> questionScoreColumn;
    @FXML private TextField titleField;
    @FXML private ComboBox<OptionItem> courseComboBox;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField uploadTitleField;
    @FXML private ComboBox<OptionItem> uploadCourseComboBox;
    @FXML private TextField uploadStartField;
    @FXML private TextField uploadEndField;
    @FXML private ComboBox<String> uploadStatusComboBox;
    @FXML private Label uploadFileLabel;
    @FXML private ComboBox<String> questionTypeComboBox;
    @FXML private TextArea questionContentArea;
    @FXML private TextField optionAField;
    @FXML private TextField optionBField;
    @FXML private TextField optionCField;
    @FXML private TextField optionDField;
    @FXML private TextField answerField;
    @FXML private TextField scoreField;

    private final ObservableList<Map> exams = FXCollections.observableArrayList();
    private final ObservableList<Map> questions = FXCollections.observableArrayList();
    private File selectedUploadFile;

    @FXML
    public void initialize() {
        examIdColumn.setCellValueFactory(new MapValueFactory<>("examId"));
        examTitleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        examCourseColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        examStatusColumn.setCellValueFactory(new MapValueFactory<>("status"));
        questionIdColumn.setCellValueFactory(new MapValueFactory<>("questionId"));
        questionTypeColumn.setCellValueFactory(new MapValueFactory<>("questionType"));
        questionContentColumn.setCellValueFactory(new MapValueFactory<>("content"));
        questionScoreColumn.setCellValueFactory(new MapValueFactory<>("score"));
        examTable.setItems(exams);
        questionTable.setItems(questions);
        statusComboBox.getItems().addAll("DRAFT", "OPEN", "CLOSED");
        uploadStatusComboBox.getItems().addAll("DRAFT", "OPEN", "CLOSED");
        uploadStatusComboBox.getSelectionModel().select("DRAFT");
        questionTypeComboBox.getItems().addAll("CHOICE", "READ");
        loadCourseOptions();
        examTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadExamDetail(newValue));
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showQuestion(newValue));
        doRefresh();
    }

    @Override
    public void doRefresh() {
        DataResponse response = HttpRequestUtil.get("/api/teacher/exams");
        if (response == null || response.getCode() != 0) {
            MessageDialog.showDialog(response == null ? "加载试卷失败" : response.getMsg());
            return;
        }
        exams.setAll((List<Map>) response.getData());
        if (!exams.isEmpty()) {
            examTable.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择试卷文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Exam files", "*.json", "*.csv", "*.md", "*.markdown"));
        selectedUploadFile = chooser.showOpenDialog(MainApplication.getMainStage());
        uploadFileLabel.setText(selectedUploadFile == null ? "未选择文件" : selectedUploadFile.getName());
    }

    @FXML
    private void onUploadExam() {
        if (selectedUploadFile == null) {
            MessageDialog.showDialog("请先选择试卷文件");
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put("title", uploadTitleField.getText());
        params.put("courseId", selectedCourseValue(uploadCourseComboBox));
        params.put("startTime", uploadStartField.getText());
        params.put("endTime", uploadEndField.getText());
        params.put("status", uploadStatusComboBox.getValue());
        DataResponse response = HttpRequestUtil.uploadMultipart("/api/teacher/exams/upload", selectedUploadFile.getAbsolutePath(), params);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("试卷上传成功");
            doRefresh();
        } else {
            MessageDialog.showDialog(response == null ? "上传失败" : response.getMsg());
        }
    }

    @FXML
    private void onSaveExam() {
        Map selected = examTable.getSelectionModel().getSelectedItem();
        Integer examId = getInt(selected, "examId");
        if (examId == null) {
            MessageDialog.showDialog("请先选择试卷");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("title", titleField.getText());
        request.add("courseId", parseInt(selectedCourseValue(courseComboBox)));
        request.add("startTime", startTimeField.getText());
        request.add("endTime", endTimeField.getText());
        request.add("status", statusComboBox.getValue());
        DataResponse response = HttpRequestUtil.put("/api/teacher/exams/" + examId, request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("试卷已保存");
            doRefresh();
        } else {
            MessageDialog.showDialog(response == null ? "保存失败" : response.getMsg());
        }
    }

    @FXML
    private void onSaveQuestion() {
        Map selected = questionTable.getSelectionModel().getSelectedItem();
        Integer questionId = getInt(selected, "questionId");
        if (questionId == null) {
            MessageDialog.showDialog("请先选择题目");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("questionType", questionTypeComboBox.getValue());
        request.add("content", questionContentArea.getText());
        request.add("optionA", optionAField.getText());
        request.add("optionB", optionBField.getText());
        request.add("optionC", optionCField.getText());
        request.add("optionD", optionDField.getText());
        request.add("answer", answerField.getText());
        request.add("score", parseInt(scoreField.getText()));
        DataResponse response = HttpRequestUtil.put("/api/teacher/questions/" + questionId, request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("题目已保存");
            loadExamDetail(examTable.getSelectionModel().getSelectedItem());
        } else {
            MessageDialog.showDialog(response == null ? "保存失败" : response.getMsg());
        }
    }

    @FXML
    private void onDeleteQuestion() {
        Map selected = questionTable.getSelectionModel().getSelectedItem();
        Integer questionId = getInt(selected, "questionId");
        if (questionId == null) {
            MessageDialog.showDialog("请先选择题目");
            return;
        }
        if (MessageDialog.choiceDialog("确认删除该题目？已有提交记录的题目会被后端拒绝。") != MessageDialog.CHOICE_YES) {
            return;
        }
        DataResponse response = HttpRequestUtil.delete("/api/teacher/questions/" + questionId);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("题目已删除");
            loadExamDetail(examTable.getSelectionModel().getSelectedItem());
        } else {
            MessageDialog.showDialog(response == null ? "删除失败" : response.getMsg());
        }
    }

    private void loadExamDetail(Map exam) {
        Integer examId = getInt(exam, "examId");
        if (examId == null) {
            return;
        }
        DataResponse response = HttpRequestUtil.get("/api/teacher/exams/" + examId);
        if (response == null || response.getCode() != 0 || !(response.getData() instanceof Map)) {
            MessageDialog.showDialog(response == null ? "加载试卷详情失败" : response.getMsg());
            return;
        }
        Map data = (Map) response.getData();
        titleField.setText(text(data, "title"));
        selectCourse(courseComboBox, text(data, "courseId"));
        startTimeField.setText(text(data, "startTime"));
        endTimeField.setText(text(data, "endTime"));
        statusComboBox.getSelectionModel().select(text(data, "status"));
        questions.setAll((List<Map>) data.get("questions"));
        if (!questions.isEmpty()) {
            questionTable.getSelectionModel().selectFirst();
        }
    }

    private void showQuestion(Map question) {
        if (question == null) {
            return;
        }
        questionTypeComboBox.getSelectionModel().select(text(question, "questionType"));
        questionContentArea.setText(text(question, "content"));
        optionAField.setText(text(question, "optionA"));
        optionBField.setText(text(question, "optionB"));
        optionCField.setText(text(question, "optionC"));
        optionDField.setText(text(question, "optionD"));
        answerField.setText(text(question, "answer"));
        scoreField.setText(text(question, "score"));
    }

    private void loadCourseOptions() {
        List<OptionItem> courseList = HttpRequestUtil.requestOptionItemList("/api/score/getCourseItemOptionList", new DataRequest());
        courseComboBox.getItems().setAll(courseList);
        uploadCourseComboBox.getItems().setAll(courseList);
    }

    private String selectedCourseValue(ComboBox<OptionItem> comboBox) {
        OptionItem item = comboBox.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getValue();
    }

    private void selectCourse(ComboBox<OptionItem> comboBox, String value) {
        for (OptionItem item : comboBox.getItems()) {
            if (value != null && value.equals(item.getValue())) {
                comboBox.getSelectionModel().select(item);
                return;
            }
        }
    }

    private String text(Map map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : value.toString();
    }

    private Integer getInt(Map map, String key) {
        return parseInt(text(map, key));
    }

    private Integer parseInt(String value) {
        try {
            return value == null || value.isBlank() ? null : (int) Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }
}
