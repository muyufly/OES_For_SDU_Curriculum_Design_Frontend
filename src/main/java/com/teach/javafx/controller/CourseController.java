package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseController extends ToolController {
    @FXML
    private TableView<Map<String, Object>> dataTableView;
    @FXML
    private TableColumn<Map, String> numColumn;
    @FXML
    private TableColumn<Map, String> nameColumn;
    @FXML
    private TableColumn<Map, String> creditColumn;
    @FXML
    private TableColumn<Map, String> preCourseColumn;
    @FXML
    private TableColumn<Map, FlowPane> operateColumn;
    @FXML
    private TextField numNameTextField;

    private List<Map<String, Object>> courseList = new ArrayList<>();
    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();
    private Map<String, Object> currentCourse;

    @FXML
    private void onQueryButtonClick() {
        DataRequest req = new DataRequest();
        req.add("numName", numNameTextField.getText());
        DataResponse res = HttpRequestUtil.request("/api/course/getCourseList", req);
        if (res != null && res.getCode() == 0) {
            courseList = (List<Map<String, Object>>) res.getData();
            setTableViewData();
            return;
        }
        MessageDialog.showDialog(res == null ? "课程查询失败" : res.getMsg());
    }

    @FXML
    private void onAddButtonClick() {
        Map<String, Object> row = new HashMap<>();
        row.put("courseId", null);
        row.put("num", "");
        row.put("name", "");
        row.put("credit", "0");
        row.put("coursePath", "");
        row.put("preCourse", "");
        row.put("preCourseId", null);
        courseList.add(0, row);
        setTableViewData();
        dataTableView.getSelectionModel().selectFirst();
    }

    private void setTableViewData() {
        observableList.clear();
        for (int i = 0; i < courseList.size(); i++) {
            Map<String, Object> map = courseList.get(i);
            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(10);
            flowPane.setAlignment(Pos.CENTER);

            Button saveButton = new Button("保存");
            saveButton.setId("save" + i);
            saveButton.setOnAction(e -> saveItem(((Button) e.getSource()).getId()));

            Button deleteButton = new Button("删除");
            deleteButton.setId("delete" + i);
            deleteButton.setOnAction(e -> deleteItem(((Button) e.getSource()).getId()));

            flowPane.getChildren().addAll(saveButton, deleteButton);
            map.put("operate", flowPane);
            observableList.add(map);
        }
        dataTableView.setItems(observableList);
    }

    public void saveItem(String name) {
        if (name == null) {
            return;
        }
        int index = Integer.parseInt(name.substring(4));
        saveCourse(courseList.get(index));
    }

    public void deleteItem(String name) {
        if (name == null) {
            return;
        }
        int index = Integer.parseInt(name.substring(6));
        deleteCourse(courseList.get(index));
    }

    private boolean saveCourse(Map<String, Object> data) {
        String num = CommonMethod.getString(data, "num");
        String courseName = CommonMethod.getString(data, "name");
        if (num == null || num.isBlank() || courseName == null || courseName.isBlank()) {
            MessageDialog.showDialog("课程号和课程名称不能为空");
            return false;
        }
        Integer credit = CommonMethod.getInteger(data, "credit");
        if (credit == null) {
            MessageDialog.showDialog("学分必须是整数");
            return false;
        }

        DataRequest req = new DataRequest();
        req.add("courseId", CommonMethod.getInteger(data, "courseId"));
        req.add("num", num);
        req.add("name", courseName);
        req.add("credit", credit);
        req.add("coursePath", CommonMethod.getString(data, "coursePath"));
        req.add("preCourseId", CommonMethod.getInteger(data, "preCourseId"));
        DataResponse res = HttpRequestUtil.request("/api/course/courseSave", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("课程保存成功");
            onQueryButtonClick();
            return true;
        }
        MessageDialog.showDialog(res == null ? "课程保存失败" : res.getMsg());
        return false;
    }

    private void deleteCourse(Map<String, Object> data) {
        if (data == null) {
            MessageDialog.showDialog("请先选择课程");
            return;
        }
        Integer courseId = CommonMethod.getInteger(data, "courseId");
        if (courseId == null) {
            courseList.remove(data);
            setTableViewData();
            return;
        }
        int ret = MessageDialog.choiceDialog("确认删除当前课程吗？");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }

        DataRequest req = new DataRequest();
        req.add("courseId", courseId);
        DataResponse res = HttpRequestUtil.request("/api/course/courseDelete", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("课程删除成功");
            onQueryButtonClick();
            return;
        }
        MessageDialog.showDialog(res == null ? "课程删除失败" : res.getMsg());
    }

    @FXML
    public void initialize() {
        numColumn.setCellValueFactory(new MapValueFactory<>("num"));
        numColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        numColumn.setOnEditCommit(event -> event.getRowValue().put("num", event.getNewValue()));

        nameColumn.setCellValueFactory(new MapValueFactory<>("name"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> event.getRowValue().put("name", event.getNewValue()));

        creditColumn.setCellValueFactory(new MapValueFactory<>("credit"));
        creditColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        creditColumn.setOnEditCommit(event -> event.getRowValue().put("credit", event.getNewValue()));

        preCourseColumn.setCellValueFactory(new MapValueFactory<>("preCourse"));
        preCourseColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        preCourseColumn.setOnEditCommit(event -> event.getRowValue().put("preCourse", event.getNewValue()));

        operateColumn.setCellValueFactory(new MapValueFactory<>("operate"));
        dataTableView.setEditable(true);
        dataTableView.getSelectionModel().getSelectedIndices().addListener((ListChangeListener<Integer>) change ->
                currentCourse = dataTableView.getSelectionModel().getSelectedItem());
        onQueryButtonClick();
    }

    @Override
    public void doNew() {
        onAddButtonClick();
    }

    @Override
    public void doSave() {
        if (currentCourse == null) {
            MessageDialog.showDialog("请先选择课程");
            return;
        }
        saveCourse(currentCourse);
    }

    @Override
    public void doDelete() {
        deleteCourse(currentCourse);
    }

    @Override
    public void doRefresh() {
        onQueryButtonClick();
    }
}
