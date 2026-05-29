package com.teach.javafx;

import atlantafx.base.theme.PrimerLight;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    private static final double LOGIN_WIDTH = 1080;
    private static final double LOGIN_HEIGHT = 768;
    private static final String GLOBAL_STYLESHEET = MainApplication.class.getResource("css/app-theme.css").toExternalForm();

    private static Stage mainStage;
    private static double stageWidth = -1;
    private static double stageHeight = -1;
    private static boolean canClose = true;

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), LOGIN_WIDTH, LOGIN_HEIGHT);
        applyGlobalStyles(scene);
        stage.setTitle("OES 登录");
        stage.setScene(scene);
        stage.setMinWidth(LOGIN_WIDTH);
        stage.setMinHeight(LOGIN_HEIGHT);
        stage.show();
        stage.setMaximized(true);
        stage.setOnCloseRequest(event -> {
            if (canClose) {
                HttpRequestUtil.close();
            } else {
                event.consume();
            }
        });
        mainStage = stage;
    }

    public static void resetStage(String name, Scene scene) {
        applyGlobalStyles(scene);
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        mainStage.show();
        mainStage.setMaximized(true);
    }

    public static void loginStage(String name, Scene scene) {
        applyGlobalStyles(scene);
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        mainStage.show();
        mainStage.setMaximized(true);
    }

    private static void applyGlobalStyles(Scene scene) {
        if (scene != null && !scene.getStylesheets().contains(GLOBAL_STYLESHEET)) {
            scene.getStylesheets().add(GLOBAL_STYLESHEET);
        }
        if (scene != null && scene.getRoot() != null) {
            prepareScrollableLists(scene.getRoot());
        }
    }

    public static void prepareScrollableLists(Parent root) {
        if (root == null) {
            return;
        }
        root.applyCss();
        prepareNode(root);
    }

    private static void prepareNode(javafx.scene.Node node) {
        if (node instanceof Control control && isListLikeControl(control)) {
            control.setMinHeight(180);
            control.setMaxHeight(Double.MAX_VALUE);
            control.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(control, Priority.ALWAYS);
            HBox.setHgrow(control, Priority.ALWAYS);
            if (control instanceof TableView<?> tableView) {
                tableView.setFixedCellSize(42);
                tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                tableView.setPlaceholder(new javafx.scene.control.Label("暂无数据"));
            }
        }
        if (node instanceof ScrollPane scrollPane) {
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
        }
        if (node instanceof Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                prepareNode(child);
            }
        }
    }

    private static boolean isListLikeControl(Control control) {
        return control instanceof javafx.scene.control.TableView<?>
                || control instanceof javafx.scene.control.ListView<?>
                || control instanceof javafx.scene.control.TreeView<?>;
    }

    public static void main(String[] args) {
        launch();
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void setCanClose(boolean canClose) {
        MainApplication.canClose = canClose;
    }
}
