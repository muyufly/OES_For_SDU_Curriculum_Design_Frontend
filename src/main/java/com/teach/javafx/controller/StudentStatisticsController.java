package com.teach.javafx.controller;

import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;

import java.util.ArrayList;
import java.util.Map;

public class StudentStatisticsController {
    @FXML
    private TableView<Map> dataTableView;  //学生信息表
    @FXML
    private TableColumn<Map, String> studentNumColumn;   //学生信息表 编号列
    @FXML
    private TableColumn<Map, String> studentNameColumn; //学生信息表 名称列
    @FXML
    private TableColumn<Map, String> courseCountColumn; //学生信息表 名称列
    @FXML
    private TableColumn<Map, String> avgScoreColumn;  //学生信息表 院系列
    @FXML
    private TableColumn<Map, String> gpaColumn; //学生信息表 专业列
    @FXML
    private TableColumn<Map, String> noColumn; //学生信息表 专业列
    @FXML
    private TableColumn<Map, String> leaveCountColumn; //学生信息表 性别列



    private ArrayList<Map> dataList = new ArrayList();  // 学生信息列表数据
    private ObservableList<Map> observableList = FXCollections.observableArrayList();  // TableView渲染列表

    /**
     * 将学生数据集合设置到面板上显示
     */
    private void setTableViewData() {
        observableList.clear();
        for (Map map : dataList) {
            observableList.addAll(FXCollections.observableArrayList(map));
        }
        dataTableView.setItems(observableList);
    }


    @FXML
    public void initialize() {
        DataResponse res;
        studentNumColumn.setCellValueFactory(new MapValueFactory<>("studentNum"));  //设置列值工程属性
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        courseCountColumn.setCellValueFactory(new MapValueFactory<>("courseCount"));
        avgScoreColumn.setCellValueFactory(new MapValueFactory<>("avgScore"));
        gpaColumn.setCellValueFactory(new MapValueFactory<>("gpa"));
        noColumn.setCellValueFactory(new MapValueFactory<>("no"));
        leaveCountColumn.setCellValueFactory(new MapValueFactory<>("leaveCount"));
        onQueryButtonClick();
    }

    @FXML
    protected void onQueryButtonClick() {
        DataRequest req = new DataRequest();
        DataResponse res = HttpRequestUtil.request("/api/studentStatistics/getStudentStatisticsList", req);
        if (res != null && res.getCode() == 0) {
            dataList = (ArrayList<Map>) res.getData();
            setTableViewData();
        }
    }


    @FXML
    protected void onStatisticsButtonClick() {
        DataRequest req = new DataRequest();
        DataResponse res = HttpRequestUtil.request("/api/studentStatistics/doStudentStatistics", req);
        if (res != null && res.getCode() == 0) {
            onQueryButtonClick();
        }
    }
}
