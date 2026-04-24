package com.teach.javafx;

import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    private static final double LOGIN_WIDTH = 960;
    private static final double LOGIN_HEIGHT = 640;
    private static final String GLOBAL_STYLESHEET = MainApplication.class.getResource("css/app-theme.css").toExternalForm();

    private static Stage mainStage;
    private static double stageWidth = -1;
    private static double stageHeight = -1;
    private static boolean canClose = true;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), LOGIN_WIDTH, LOGIN_HEIGHT);
        applyGlobalStyles(scene);
        stage.setTitle("OES 登录");
        stage.setScene(scene);
        stage.setMinWidth(LOGIN_WIDTH);
        stage.setMinHeight(LOGIN_HEIGHT);
        stage.show();
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
        if (stageWidth > 0) {
            mainStage.setWidth(stageWidth);
            mainStage.setHeight(stageHeight);
            mainStage.setX(0);
            mainStage.setY(0);
        }
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        mainStage.setMaximized(true);
        mainStage.show();
    }

    public static void loginStage(String name, Scene scene) {
        stageWidth = mainStage.getWidth();
        stageHeight = mainStage.getHeight();
        applyGlobalStyles(scene);
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        double x = (stageWidth - LOGIN_WIDTH) / 2;
        double y = (stageHeight - LOGIN_HEIGHT) / 2;
        mainStage.setX(x);
        mainStage.setY(y);
        mainStage.setWidth(LOGIN_WIDTH);
        mainStage.setHeight(LOGIN_HEIGHT);
        mainStage.show();
    }

    private static void applyGlobalStyles(Scene scene) {
        if (scene != null && !scene.getStylesheets().contains(GLOBAL_STYLESHEET)) {
            scene.getStylesheets().add(GLOBAL_STYLESHEET);
        }
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
