package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.MyTreeNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class MainFrameController {
    class ChangePanelHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            changeContent(actionEvent);
        }
    }

    private final Map<String, Tab> tabMap = new HashMap<>();
    private final Map<String, Scene> sceneMap = new HashMap<>();
    private final Map<String, ToolController> controlMap = new HashMap<>();

    @FXML private MenuBar menuBar;
    @FXML private TreeView<MyTreeNode> menuTree;
    @FXML protected TabPane contentTabPane;
    @FXML private Label systemPrompt;
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;

    private ChangePanelHandler handler = null;

    void addMenuItems(Menu parent, List<Map> menuList) {
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            String name = (String) menuData.get("name");
            String title = (String) menuData.get("title");
            if (childList == null || childList.isEmpty()) {
                MenuItem item = new MenuItem();
                item.setId(name);
                item.setText(title);
                item.setOnAction(this::changeContent);
                parent.getItems().add(item);
            } else {
                Menu menu = new Menu();
                menu.setText(title);
                addMenuItems(menu, childList);
                parent.getItems().add(menu);
            }
        }
    }

    public void initMenuBar(List<Map> menuList) {
        menuBar.getMenus().clear();
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            Menu menu = new Menu();
            menu.setText((String) menuData.get("title"));
            if (childList != null && !childList.isEmpty()) {
                addMenuItems(menu, childList);
            }
            menuBar.getMenus().add(menu);
        }
    }

    void addMenuItems(TreeItem<MyTreeNode> parent, List<Map> menuList) {
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            TreeItem<MyTreeNode> menu = new TreeItem<>(new MyTreeNode(null, (String) menuData.get("name"), (String) menuData.get("title"), 0));
            parent.getChildren().add(menu);
            if (childList != null && !childList.isEmpty()) {
                addMenuItems(menu, childList);
            }
        }
    }

    public void initMenuTree(List<Map> menuList) {
        TreeItem<MyTreeNode> root = new TreeItem<>(new MyTreeNode(null, null, "菜单", 0));
        for (Map menuData : menuList) {
            List<Map> childList = (List<Map>) menuData.get("sList");
            TreeItem<MyTreeNode> menu = new TreeItem<>(new MyTreeNode(null, (String) menuData.get("name"), (String) menuData.get("title"), 0));
            if (childList != null && !childList.isEmpty()) {
                addMenuItems(menu, childList);
            }
            root.getChildren().add(menu);
        }
        menuTree.setRoot(root);
        menuTree.setShowRoot(false);
        menuTree.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            TreeItem<MyTreeNode> treeItem = menuTree.getSelectionModel().getSelectedItem();
            if (treeItem == null || treeItem.getValue() == null) {
                return;
            }
            MyTreeNode menu = treeItem.getValue();
            String name = menu.getValue();
            if (name == null || name.isEmpty()) {
                return;
            }
            if ("logout".equals(name)) {
                logout();
            } else if (name.endsWith("Command")) {
                try {
                    Method method = this.getClass().getMethod(name);
                    method.invoke(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    MessageDialog.showDialog("命令执行失败：" + name);
                }
            } else {
                changeContent(name, menu.getLabel());
            }
        });
    }

    @FXML
    public void initialize() {
        handler = new ChangePanelHandler();
        welcomeLabel.setText("你好，" + AppStore.getDisplayName());
        roleLabel.setText(AppStore.getJwt() == null ? "" : AppStore.getJwt().getRole());

        DataRequest req = new DataRequest();
        DataResponse dbResponse = HttpRequestUtil.request("/api/base/getDataBaseUserName", req);
        String userName = dbResponse != null && dbResponse.getData() != null ? dbResponse.getData().toString() : "未知";
        systemPrompt.setText("服务地址：" + HttpRequestUtil.serverUrl + "  数据库：" + userName);

        DataResponse menuResponse = HttpRequestUtil.request("/api/base/getMenuList", req);
        if (menuResponse == null || menuResponse.getCode() != 0 || !(menuResponse.getData() instanceof List<?>)) {
            MessageDialog.showDialog(menuResponse == null ? "菜单加载失败" : menuResponse.getMsg());
            return;
        }
        List<Map> menuList = (List<Map>) menuResponse.getData();
        addExamMenus(menuList);
        initMenuBar(menuList);
        initMenuTree(menuList);
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    private void addExamMenus(List<Map> menuList) {
        String role = AppStore.getJwt() == null ? "" : AppStore.getJwt().getRole();
        if (role.contains("TEACHER")) {
            Map root = menuRoot("考试管理");
            addChild(root, "teacher-exam-panel", "试卷管理");
            addChild(root, "teacher-grade-panel", "试卷批改");
            addChild(root, "teacher-score-panel", "学生成绩管理");
            mergeRoot(menuList, root);
        }
        if (role.contains("STUDENT")) {
            Map root = menuRoot("我的考试");
            addChild(root, "student-exam-panel", "获取试卷");
            addChild(root, "student-score-panel", "我的成绩");
            mergeRoot(menuList, root);
        }
    }

    private Map menuRoot(String title) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", "");
        root.put("title", title);
        root.put("sList", new ArrayList<Map>());
        return root;
    }

    private void addChild(Map root, String name, String title) {
        List<Map> children = (List<Map>) root.get("sList");
        Map<String, Object> child = new LinkedHashMap<>();
        child.put("name", name);
        child.put("title", title);
        child.put("sList", new ArrayList<Map>());
        children.add(child);
    }

    private void mergeRoot(List<Map> menuList, Map root) {
        String title = (String) root.get("title");
        for (Map existing : menuList) {
            if (!title.equals(existing.get("title"))) {
                continue;
            }
            List<Map> existingChildren = (List<Map>) existing.get("sList");
            if (existingChildren == null) {
                existingChildren = new ArrayList<>();
                existing.put("sList", existingChildren);
            }
            for (Map child : (List<Map>) root.get("sList")) {
                boolean exists = existingChildren.stream().anyMatch(c -> child.get("name").equals(c.get("name")));
                if (!exists) {
                    existingChildren.add(child);
                }
            }
            return;
        }
        menuList.add(root);
    }

    protected void onLogoutMenuClick(ActionEvent event) {
        logout();
    }

    @FXML
    protected void onLogoutButtonClick() {
        logout();
    }

    protected void logout() {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 960, 640);
            AppStore.setJwt(null);
            MainApplication.loginStage("OES 登录", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeContent(ActionEvent ae) {
        Object obj = ae.getSource();
        if (obj instanceof MenuItem item) {
            changeContent(item.getId(), item.getText());
        }
    }

    public void changeContent(String name, String title) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Tab tab = tabMap.get(name);
        if (tab == null) {
            Scene scene = sceneMap.get(name);
            if (scene == null) {
                FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(name + ".fxml"));
                try {
                    scene = new Scene(fxmlLoader.load(), 1024, 768);
                    sceneMap.put(name, scene);
                } catch (IOException e) {
                    e.printStackTrace();
                    MessageDialog.showDialog("页面加载失败：" + name);
                    return;
                }
                Object controller = fxmlLoader.getController();
                if (controller instanceof ToolController toolController) {
                    controlMap.put(name, toolController);
                }
            }
            tab = new Tab(title);
            tab.setId(name);
            tab.setOnSelectionChanged(this::tabSelectedChanged);
            tab.setOnClosed(this::tabOnClosed);
            tab.setContent(scene.getRoot());
            contentTabPane.getTabs().add(tab);
            tabMap.put(name, tab);
        }
        contentTabPane.getSelectionModel().select(tab);
    }

    public void tabSelectedChanged(Event e) {
        Tab tab = (Tab) e.getSource();
        ToolController controller = controlMap.get(tab.getId());
        if (controller != null) {
            controller.doRefresh();
        }
    }

    public void tabOnClosed(Event e) {
        Tab tab = (Tab) e.getSource();
        String name = tab.getId();
        contentTabPane.getTabs().remove(tab);
        tabMap.remove(name);
    }

    public ToolController getCurrentToolController() {
        for (String name : controlMap.keySet()) {
            Tab tab = tabMap.get(name);
            if (tab != null && tab.isSelected()) {
                return controlMap.get(name);
            }
        }
        return null;
    }

    protected void doNewCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) controller.doNew();
    }

    protected void doSaveCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) controller.doSave();
    }

    protected void doDeleteCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) controller.doDelete();
    }

    protected void doPrintCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) controller.doPrint();
    }

    protected void doExportCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) controller.doExport();
    }

    protected void doImportCommand() {
        ToolController controller = getCurrentToolController();
        if (controller != null) controller.doImport();
    }

    protected void doTestCommand() {
        ToolController controller = getCurrentToolController();
        if (controller == null) controller = new ToolController() {};
        controller.doTest();
    }

    public ToolController getToolController(String name) {
        return controlMap.get(name);
    }
}
