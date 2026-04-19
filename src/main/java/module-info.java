module com.teach.javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires javafx.media;
    requires java.logging;
    requires java.net.http;
    requires java.sql;
    requires com.google.gson;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.apache.poi.ooxml;
    requires static lombok;

    // 允许 FXML 加载资源和反射控制器
    opens com.teach.javafx to javafx.fxml, javafx.graphics;
    opens com.teach.javafx.controller to javafx.fxml, com.google.gson;
    opens com.teach.javafx.controller.base to javafx.fxml, com.google.gson;
    opens com.teach.javafx.request to com.google.gson, javafx.fxml;
    opens com.teach.javafx.util to com.google.gson, javafx.fxml;
    opens com.teach.javafx.models to javafx.base, com.google.gson, javafx.fxml;
    
    // 允许加载资源文件包
    opens com.teach.javafx.base to javafx.fxml, javafx.graphics;

    // 导出包以供外部（或运行时环境）访问
    exports com.teach.javafx;
    exports com.teach.javafx.controller;
    exports com.teach.javafx.controller.base;
    exports com.teach.javafx.request;
    exports com.teach.javafx.util;
    exports com.teach.javafx.models;
}