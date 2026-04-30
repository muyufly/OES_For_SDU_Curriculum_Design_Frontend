package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class StudentExamWindowController {
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private Label lastSaveLabel;
    @FXML private TableView<Map> questionTable;
    @FXML private TableColumn<Map, String> sortColumn;
    @FXML private TableColumn<Map, String> typeColumn;
    @FXML private TableColumn<Map, String> scoreColumn;
    @FXML private TextArea questionArea;
    @FXML private VBox choiceBox;
    @FXML private RadioButton optionAButton;
    @FXML private RadioButton optionBButton;
    @FXML private RadioButton optionCButton;
    @FXML private RadioButton optionDButton;
    @FXML private TextArea answerArea;
    @FXML private Button saveButton;
    @FXML private Button submitButton;

    private final ObservableList<Map> questions = FXCollections.observableArrayList();
    private final Map<Integer, String> answers = new HashMap<>();
    private final ToggleGroup choiceGroup = new ToggleGroup();
    private Integer examId;
    private Stage stage;
    private Timeline timer;
    private long remainingSeconds;
    private int autoSaveTicks;
    private boolean ended;

    @FXML
    public void initialize() {
        sortColumn.setCellValueFactory(new MapValueFactory<>("sortOrder"));
        typeColumn.setCellValueFactory(new MapValueFactory<>("questionType"));
        scoreColumn.setCellValueFactory(new MapValueFactory<>("score"));
        optionAButton.setToggleGroup(choiceGroup);
        optionBButton.setToggleGroup(choiceGroup);
        optionCButton.setToggleGroup(choiceGroup);
        optionDButton.setToggleGroup(choiceGroup);
        optionAButton.setUserData("A");
        optionBButton.setUserData("B");
        optionCButton.setUserData("C");
        optionDButton.setUserData("D");
        choiceGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            Map question = questionTable.getSelectionModel().getSelectedItem();
            if (question != null && "CHOICE".equals(text(question, "questionType")) && newToggle != null) {
                answers.put(intValue(question, "questionId"), newToggle.getUserData().toString());
            }
        });
        questionTable.setItems(questions);
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            saveCurrentAnswer(oldRow);
            showQuestion(newRow);
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(event -> {
            if (!ended) {
                saveDraft(false);
            }
            stopTimer();
        });
    }

    public void startExam(Integer examId) {
        this.examId = examId;
        DataResponse response = HttpRequestUtil.request("/api/student/exams/" + examId + "/start", new DataRequest());
        if (response == null || response.getCode() != 0 || !(response.getData() instanceof Map)) {
            MessageDialog.showDialog(response == null ? "进入考试失败" : response.getMsg());
            if (stage != null) stage.close();
            return;
        }
        loadSession((Map) response.getData());
        startTimer();
    }

    @FXML
    private void onManualSave() {
        saveDraft(true);
    }

    @FXML
    private void onSubmit() {
        submitExam(true);
    }

    private void submitExam(boolean confirm) {
        saveCurrentAnswer(questionTable.getSelectionModel().getSelectedItem());
        if (confirm && MessageDialog.choiceDialog("确认提交试卷？提交后不能继续修改。") != MessageDialog.CHOICE_YES) {
            return;
        }
        DataResponse response = HttpRequestUtil.request("/api/student/exams/" + examId + "/submit", buildAnswerRequest());
        if (response != null && response.getCode() == 0) {
            ended = true;
            lockEditing();
            stopTimer();
            MessageDialog.showDialog("提交成功");
        } else {
            MessageDialog.showDialog(response == null ? "提交失败" : response.getMsg());
        }
    }

    private void loadSession(Map data) {
        titleLabel.setText(text(data, "title"));
        statusLabel.setText(text(data, "studentExamStatus"));
        remainingSeconds = longValue(data, "remainingSeconds");
        List<Map> loadedQuestions = (List<Map>) data.get("questions");
        questions.setAll(loadedQuestions == null ? List.of() : loadedQuestions);
        answers.clear();
        for (Map question : questions) {
            Integer questionId = intValue(question, "questionId");
            answers.put(questionId, text(question, "answer"));
        }
        if (!questions.isEmpty()) {
            questionTable.getSelectionModel().selectFirst();
        }
        updateTimerLabel();
    }

    private void showQuestion(Map question) {
        choiceGroup.selectToggle(null);
        if (question == null) {
            questionArea.clear();
            answerArea.clear();
            choiceBox.setVisible(false);
            choiceBox.setManaged(false);
            answerArea.setVisible(true);
            answerArea.setManaged(true);
            return;
        }
        questionArea.setText(text(question, "content"));
        boolean choice = "CHOICE".equals(text(question, "questionType"));
        choiceBox.setVisible(choice);
        choiceBox.setManaged(choice);
        answerArea.setVisible(!choice);
        answerArea.setManaged(!choice);
        Integer questionId = intValue(question, "questionId");
        String savedAnswer = answers.getOrDefault(questionId, "");
        if (choice) {
            optionAButton.setText("A. " + text(question, "optionA"));
            optionBButton.setText("B. " + text(question, "optionB"));
            optionCButton.setText("C. " + text(question, "optionC"));
            optionDButton.setText("D. " + text(question, "optionD"));
            selectChoice(savedAnswer);
        } else {
            answerArea.setText(savedAnswer);
        }
    }

    private void selectChoice(String answer) {
        for (Toggle toggle : choiceGroup.getToggles()) {
            if (toggle.getUserData() != null && toggle.getUserData().toString().equalsIgnoreCase(answer)) {
                choiceGroup.selectToggle(toggle);
                return;
            }
        }
        choiceGroup.selectToggle(null);
    }

    private void saveCurrentAnswer(Map question) {
        if (question == null || ended) return;
        Integer questionId = intValue(question, "questionId");
        if (questionId == null) return;
        if ("CHOICE".equals(text(question, "questionType"))) {
            Toggle selected = choiceGroup.getSelectedToggle();
            answers.put(questionId, selected == null ? "" : selected.getUserData().toString());
        } else {
            answers.put(questionId, answerArea.getText());
        }
    }

    private void saveDraft(boolean showMessage) {
        if (ended || examId == null) return;
        saveCurrentAnswer(questionTable.getSelectionModel().getSelectedItem());
        DataResponse response = HttpRequestUtil.request("/api/student/exams/" + examId + "/draft", buildAnswerRequest());
        if (response != null && response.getCode() == 0) {
            lastSaveLabel.setText("上次保存：" + java.time.LocalTime.now().withNano(0));
            if (showMessage) MessageDialog.showDialog("草稿已保存");
        } else if (showMessage) {
            MessageDialog.showDialog(response == null ? "保存失败" : response.getMsg());
        }
    }

    private DataRequest buildAnswerRequest() {
        List<Map<String, Object>> answerList = new ArrayList<>();
        for (Map question : questions) {
            Integer questionId = intValue(question, "questionId");
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", questionId);
            answer.put("answer", answers.getOrDefault(questionId, ""));
            answerList.add(answer);
        }
        DataRequest request = new DataRequest();
        request.add("answers", answerList);
        return request;
    }

    private void startTimer() {
        stopTimer();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (ended) return;
            remainingSeconds = Math.max(0, remainingSeconds - 1);
            autoSaveTicks++;
            updateTimerLabel();
            if (autoSaveTicks >= 180) {
                autoSaveTicks = 0;
                saveDraft(false);
            }
            if (remainingSeconds <= 0) {
                submitExam(false);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void updateTimerLabel() {
        long h = remainingSeconds / 3600;
        long m = (remainingSeconds % 3600) / 60;
        long s = remainingSeconds % 60;
        timerLabel.setText(String.format("剩余时间：%02d:%02d:%02d", h, m, s));
    }

    private void lockEditing() {
        answerArea.setEditable(false);
        choiceBox.setDisable(true);
        saveButton.setDisable(true);
        submitButton.setDisable(true);
        statusLabel.setText("已结束");
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

    private long longValue(Map map, String key) {
        try {
            String value = text(map, key);
            return value.isBlank() ? 0 : (long) Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
