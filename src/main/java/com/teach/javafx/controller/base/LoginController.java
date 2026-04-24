package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.LoginRequest;
import com.teach.javafx.request.OptionItem;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox vbox;
    @FXML
    private TextField registerUsernameField;
    @FXML
    private TextField registerPerNameField;
    @FXML
    private TextField registerEmailField;
    @FXML
    private ComboBox<OptionItem> registerRoleComboBox;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private PasswordField registerConfirmPasswordField;

    @FXML
    public void initialize() {
        vbox.getStyleClass().add("login-page");
        registerRoleComboBox.getItems().clear();
        registerRoleComboBox.getItems().add(new OptionItem(null, "STUDENT", "学生"));
        registerRoleComboBox.getItems().add(new OptionItem(null, "TEACHER", "教师"));
        registerRoleComboBox.getItems().add(new OptionItem(null, "ADMIN", "管理员"));
        registerRoleComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    protected void onLoginButtonClick() {
        onLoginButtonClick(usernameField.getText(), passwordField.getText());
    }

    @FXML
    protected void onAdminLoginButtonClick() {
        onLoginButtonClick("admin_test", "123456");
    }

    @FXML
    protected void onStudentLoginButtonClick() {
        onLoginButtonClick("student_test", "123456");
    }

    @FXML
    protected void onTeacherLoginButtonClick() {
        onLoginButtonClick("teacher_test", "123456");
    }

    @FXML
    protected void onRegisterButtonClick() {
        String username = registerUsernameField.getText();
        String perName = registerPerNameField.getText();
        String email = registerEmailField.getText();
        OptionItem roleItem = registerRoleComboBox.getSelectionModel().getSelectedItem();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();

        if (username == null || username.isBlank() ||
                perName == null || perName.isBlank() ||
                password == null || password.isBlank() ||
                confirmPassword == null || confirmPassword.isBlank()) {
            MessageDialog.showDialog("请完整填写注册信息");
            return;
        }
        username = username.trim();
        perName = perName.trim();
        email = email == null ? "" : email.trim();
        if (username.length() > 20) {
            MessageDialog.showDialog("用户名不能超过20个字符");
            return;
        }
        if (perName.length() > 50) {
            MessageDialog.showDialog("姓名不能超过50个字符");
            return;
        }
        if (email.length() > 60) {
            MessageDialog.showDialog("邮箱不能超过60个字符");
            return;
        }
        if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            MessageDialog.showDialog("邮箱格式不正确");
            return;
        }
        if (!password.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的密码不一致");
            return;
        }
        if (roleItem == null) {
            MessageDialog.showDialog("请选择注册角色");
            return;
        }

        DataRequest request = new DataRequest();
        request.add("username", username);
        request.add("password", password);
        request.add("perName", perName);
        request.add("email", email);
        request.add("role", roleItem.getValue());

        DataResponse response = HttpRequestUtil.request("/auth/registerUser", request);
        if (response != null && response.getCode() == 0) {
            MessageDialog.showDialog("注册成功，现在可以直接登录");
            usernameField.setText(username);
            passwordField.setText(password);
            clearRegisterForm();
            return;
        }
        MessageDialog.showDialog(response == null ? "注册失败" : response.getMsg());
    }

    private void onLoginButtonClick(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            MessageDialog.showDialog("请输入用户名和密码");
            return;
        }
        LoginRequest loginRequest = new LoginRequest(username.trim(), password);
        String msg = HttpRequestUtil.login(loginRequest);
        if (msg != null) {
            MessageDialog.showDialog(msg);
            return;
        }
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/main-frame.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 1280, 820);
            AppStore.setMainFrameController((MainFrameController) fxmlLoader.getController());
            MainApplication.resetStage("OES 在线考试系统", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearRegisterForm() {
        registerUsernameField.clear();
        registerPerNameField.clear();
        registerEmailField.clear();
        registerPasswordField.clear();
        registerConfirmPasswordField.clear();
        registerRoleComboBox.getSelectionModel().selectFirst();
    }
}
