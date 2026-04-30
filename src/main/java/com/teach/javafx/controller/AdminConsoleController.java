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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminConsoleController extends ToolController {
    @FXML private Label overviewLabel;
    @FXML private TextField userKeywordField;
    @FXML private ComboBox<String> roleFilterBox;
    @FXML private ComboBox<String> roleEditBox;
    @FXML private TableView<Map> userTable;
    @FXML private TableColumn<Map, String> userNameColumn;
    @FXML private TableColumn<Map, String> personNameColumn;
    @FXML private TableColumn<Map, String> roleColumn;
    @FXML private TableColumn<Map, String> lastLoginColumn;

    @FXML private TextField teacherKeywordField;
    @FXML private TableView<Map> teacherTable;
    @FXML private TableColumn<Map, String> teacherNumColumn;
    @FXML private TableColumn<Map, String> teacherNameColumn;
    @FXML private TableColumn<Map, String> teacherDeptColumn;
    @FXML private TableColumn<Map, String> teacherClassCountColumn;
    @FXML private ComboBox<Map> classComboBox;
    @FXML private TableView<Map> bindingTable;
    @FXML private TableColumn<Map, String> bindingTeacherColumn;
    @FXML private TableColumn<Map, String> bindingClassColumn;
    @FXML private TableColumn<Map, String> bindingStudentCountColumn;

    @FXML private TextField examKeywordField;
    @FXML private ComboBox<String> examStatusBox;
    @FXML private ComboBox<String> examStatusEditBox;
    @FXML private TableView<Map> examTable;
    @FXML private TableColumn<Map, String> examTitleColumn;
    @FXML private TableColumn<Map, String> examCourseColumn;
    @FXML private TableColumn<Map, String> examStatusColumn;
    @FXML private TableColumn<Map, String> examEndedColumn;
    @FXML private TableColumn<Map, String> examDraftColumn;
    @FXML private TableColumn<Map, String> examCreatorColumn;

    @FXML private TableView<Map> scoreTable;
    @FXML private TableColumn<Map, String> scoreStudentColumn;
    @FXML private TableColumn<Map, String> scoreClassColumn;
    @FXML private TableColumn<Map, String> scoreCourseColumn;
    @FXML private TableColumn<Map, String> scoreMarkColumn;

    private final ObservableList<Map> users = FXCollections.observableArrayList();
    private final ObservableList<Map> teachers = FXCollections.observableArrayList();
    private final ObservableList<Map> bindings = FXCollections.observableArrayList();
    private final ObservableList<Map> exams = FXCollections.observableArrayList();
    private final ObservableList<Map> scores = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        roleFilterBox.getItems().setAll("全部", "ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT");
        roleFilterBox.getSelectionModel().selectFirst();
        roleEditBox.getItems().setAll("ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT");
        examStatusBox.getItems().setAll("全部", "DRAFT", "OPEN", "CLOSED");
        examStatusBox.getSelectionModel().selectFirst();
        examStatusEditBox.getItems().setAll("DRAFT", "OPEN", "CLOSED");

        bindUserColumns();
        bindTeacherColumns();
        bindExamColumns();
        bindScoreColumns();

        classComboBox.setCellFactory(list -> classCell());
        classComboBox.setButtonCell(classCell());

        loadAll();
    }

    @Override
    public void doRefresh() {
        loadAll();
    }

    @FXML
    private void onRefresh() {
        loadAll();
    }

    @FXML
    private void onQueryUsers() {
        String role = roleFilterBox.getValue();
        if ("全部".equals(role)) {
            role = "";
        }
        DataResponse response = HttpRequestUtil.get("/api/admin/users?keyword=" + enc(userKeywordField.getText()) + "&roleName=" + enc(role));
        if (ok(response)) {
            users.setAll((List<Map>) response.getData());
        } else {
            showError(response, "用户列表加载失败");
        }
    }

    @FXML
    private void onUpdateUserRole() {
        Map user = userTable.getSelectionModel().getSelectedItem();
        String role = roleEditBox.getValue();
        if (user == null || role == null || role.isBlank()) {
            MessageDialog.showDialog("请选择用户和新角色");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("roleName", role);
        DataResponse response = HttpRequestUtil.put("/api/admin/users/" + text(user, "personId") + "/role", request);
        if (ok(response)) {
            MessageDialog.showDialog("角色已更新");
            onQueryUsers();
            loadOverview();
        } else {
            showError(response, "角色更新失败");
        }
    }

    @FXML
    private void onQueryTeachers() {
        DataResponse response = HttpRequestUtil.get("/api/admin/teachers?keyword=" + enc(teacherKeywordField.getText()));
        if (ok(response)) {
            teachers.setAll((List<Map>) response.getData());
        } else {
            showError(response, "教师列表加载失败");
        }
    }

    @FXML
    private void onAssignClass() {
        Map teacher = teacherTable.getSelectionModel().getSelectedItem();
        Map clazz = classComboBox.getSelectionModel().getSelectedItem();
        if (teacher == null || clazz == null) {
            MessageDialog.showDialog("请选择教师和班级");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("teacherId", text(teacher, "teacherId"));
        request.add("className", text(clazz, "className"));
        DataResponse response = HttpRequestUtil.request("/api/admin/teachers/assign", request);
        if (ok(response)) {
            MessageDialog.showDialog("班级绑定成功");
            loadBindings();
            onQueryTeachers();
        } else {
            showError(response, "班级绑定失败");
        }
    }

    @FXML
    private void onDeleteBinding() {
        Map binding = bindingTable.getSelectionModel().getSelectedItem();
        if (binding == null) {
            MessageDialog.showDialog("请选择要删除的绑定");
            return;
        }
        DataResponse response = HttpRequestUtil.delete("/api/admin/teacher-classes/" + text(binding, "id"));
        if (ok(response)) {
            MessageDialog.showDialog("绑定已删除");
            loadBindings();
            onQueryTeachers();
        } else {
            showError(response, "删除绑定失败");
        }
    }

    @FXML
    private void onQueryExams() {
        String status = examStatusBox.getValue();
        if ("全部".equals(status)) {
            status = "";
        }
        DataResponse response = HttpRequestUtil.get("/api/admin/exams?keyword=" + enc(examKeywordField.getText()) + "&status=" + enc(status));
        if (ok(response)) {
            exams.setAll((List<Map>) response.getData());
        } else {
            showError(response, "试卷列表加载失败");
        }
    }

    @FXML
    private void onUpdateExamStatus() {
        Map exam = examTable.getSelectionModel().getSelectedItem();
        String status = examStatusEditBox.getValue();
        if (exam == null || status == null || status.isBlank()) {
            MessageDialog.showDialog("请选择试卷和新状态");
            return;
        }
        DataRequest request = new DataRequest();
        request.add("status", status);
        DataResponse response = HttpRequestUtil.put("/api/admin/exams/" + text(exam, "examId") + "/status", request);
        if (ok(response)) {
            MessageDialog.showDialog("试卷状态已更新");
            onQueryExams();
            loadOverview();
        } else {
            showError(response, "试卷状态更新失败");
        }
    }

    @FXML
    private void onLoadScores() {
        DataResponse response = HttpRequestUtil.get("/api/admin/scores/all?currentPage=0&pageSize=200");
        if (ok(response) && response.getData() instanceof Map<?, ?> data) {
            scores.setAll((List<Map>) data.get("dataList"));
        } else {
            showError(response, "成绩加载失败");
        }
    }

    private void loadAll() {
        loadOverview();
        onQueryUsers();
        onQueryTeachers();
        loadClasses();
        loadBindings();
        onQueryExams();
        onLoadScores();
    }

    private void loadOverview() {
        DataResponse response = HttpRequestUtil.get("/api/admin/overview");
        if (!ok(response) || !(response.getData() instanceof Map<?, ?> data)) {
            showError(response, "系统总览加载失败");
            return;
        }
        overviewLabel.setText(String.format(
                "用户 %s | 学生 %s | 教师 %s | 课程 %s | 试卷 %s | 开放试卷 %s | 草稿考试 %s | 已结束考试 %s | 成绩 %s",
                value(data, "userCount"), value(data, "studentCount"), value(data, "teacherCount"),
                value(data, "courseCount"), value(data, "examCount"), value(data, "openExamCount"),
                value(data, "draftAttemptCount"), value(data, "endedAttemptCount"), value(data, "scoreCount")));
    }

    private void loadClasses() {
        DataResponse response = HttpRequestUtil.get("/api/admin/classes");
        if (ok(response)) {
            classComboBox.getItems().setAll((List<Map>) response.getData());
        }
    }

    private void loadBindings() {
        DataResponse response = HttpRequestUtil.get("/api/admin/teacher-classes");
        if (ok(response)) {
            bindings.setAll((List<Map>) response.getData());
        }
    }

    private void bindUserColumns() {
        userNameColumn.setCellValueFactory(new MapValueFactory<>("userName"));
        personNameColumn.setCellValueFactory(new MapValueFactory<>("personName"));
        roleColumn.setCellValueFactory(new MapValueFactory<>("roleName"));
        lastLoginColumn.setCellValueFactory(new MapValueFactory<>("lastLoginTime"));
        userTable.setItems(users);
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row != null) {
                roleEditBox.setValue(text(row, "roleName"));
            }
        });
    }

    private void bindTeacherColumns() {
        teacherNumColumn.setCellValueFactory(new MapValueFactory<>("teacherNum"));
        teacherNameColumn.setCellValueFactory(new MapValueFactory<>("teacherName"));
        teacherDeptColumn.setCellValueFactory(new MapValueFactory<>("dept"));
        teacherClassCountColumn.setCellValueFactory(new MapValueFactory<>("classCount"));
        teacherTable.setItems(teachers);
        bindingTeacherColumn.setCellValueFactory(new MapValueFactory<>("teacherName"));
        bindingClassColumn.setCellValueFactory(new MapValueFactory<>("className"));
        bindingStudentCountColumn.setCellValueFactory(new MapValueFactory<>("studentCount"));
        bindingTable.setItems(bindings);
    }

    private void bindExamColumns() {
        examTitleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        examCourseColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        examStatusColumn.setCellValueFactory(new MapValueFactory<>("status"));
        examEndedColumn.setCellValueFactory(new MapValueFactory<>("endedCount"));
        examDraftColumn.setCellValueFactory(new MapValueFactory<>("draftCount"));
        examCreatorColumn.setCellValueFactory(new MapValueFactory<>("creatorName"));
        examTable.setItems(exams);
        examTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row != null) {
                examStatusEditBox.setValue(text(row, "status"));
            }
        });
    }

    private void bindScoreColumns() {
        scoreStudentColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        scoreClassColumn.setCellValueFactory(new MapValueFactory<>("className"));
        scoreCourseColumn.setCellValueFactory(new MapValueFactory<>("courseName"));
        scoreMarkColumn.setCellValueFactory(new MapValueFactory<>("mark"));
        scoreTable.setItems(scores);
    }

    private ListCell<Map> classCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Map item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : text(item, "className") + " (" + text(item, "studentCount") + "人)");
            }
        };
    }

    private boolean ok(DataResponse response) {
        return response != null && Objects.equals(response.getCode(), 0);
    }

    private void showError(DataResponse response, String fallback) {
        MessageDialog.showDialog(response == null || response.getMsg() == null ? fallback : response.getMsg());
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String text(Map map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : value.toString();
    }

    private String value(Map<?, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "0" : value.toString();
    }
}
