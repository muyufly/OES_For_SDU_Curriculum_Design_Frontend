package com.teach.javafx.request;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Frontend HTTP helper for talking to the Spring Boot backend.
 */
public class HttpRequestUtil {
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();
    public static String serverUrl = "http://localhost:22222";
//    public static String serverUrl = "http://202.194.7.29:22222";

    public static void close() {
    }

    private static String getAuthorizationHeader() {
        if (AppStore.getJwt() == null || AppStore.getJwt().getToken() == null || AppStore.getJwt().getToken().isBlank()) {
            return null;
        }
        return "Bearer " + AppStore.getJwt().getToken();
    }

    private static void attachUsername(DataRequest request) {
        if (request == null) {
            return;
        }
        if (AppStore.getJwt() != null && AppStore.getJwt().getUsername() != null) {
            request.add("username", AppStore.getJwt().getUsername());
        }
    }

    private static HttpRequest buildJsonRequest(String url, DataRequest request) {
        DataRequest actualRequest = request == null ? new DataRequest() : request;
        attachUsername(actualRequest);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(actualRequest)))
                .header("Content-Type", "application/json");

        String authorization = getAuthorizationHeader();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return builder.build();
    }

    private static DataResponse buildErrorResponse(String msg) {
        return new DataResponse(1, null, msg);
    }

    private static HttpRequest.Builder authorizedBuilder(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + url));
        String authorization = getAuthorizationHeader();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return builder;
    }

    public static String login(LoginRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/auth/login"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                .header("Content-Type", "application/json")
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode====" + response.statusCode());
            if (response.statusCode() == 200) {
                JwtResponse jwt = gson.fromJson(response.body(), JwtResponse.class);
                AppStore.setJwt(jwt);
                return null;
            }
            if (response.statusCode() == 401) {
                return "用户名或密码错误";
            }
            return "登录失败，状态码：" + response.statusCode();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "无法连接后端服务：" + serverUrl;
        }
    }

    public static DataResponse request(String url, DataRequest request) {
        HttpRequest httpRequest = buildJsonRequest(url, request);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("url=" + url + "    response.statusCode=" + response.statusCode());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
            if (response.statusCode() == 401) {
                return buildErrorResponse("登录已失效，请重新登录");
            }
            return buildErrorResponse("请求失败，状态码：" + response.statusCode());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return buildErrorResponse("请求失败，无法连接后端服务：" + serverUrl);
        }
    }

    public static DataResponse get(String url) {
        HttpRequest httpRequest = authorizedBuilder(url).GET().build();
        return sendForDataResponse(httpRequest, "GET " + url);
    }

    public static DataResponse put(String url, DataRequest request) {
        DataRequest actualRequest = request == null ? new DataRequest() : request;
        attachUsername(actualRequest);
        HttpRequest httpRequest = authorizedBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(actualRequest)))
                .header("Content-Type", "application/json")
                .build();
        return sendForDataResponse(httpRequest, "PUT " + url);
    }

    public static DataResponse delete(String url) {
        HttpRequest httpRequest = authorizedBuilder(url).DELETE().build();
        return sendForDataResponse(httpRequest, "DELETE " + url);
    }

    private static DataResponse sendForDataResponse(HttpRequest httpRequest, String label) {
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println(label + " response.statusCode=" + response.statusCode());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return buildErrorResponse("没有权限或登录已失效，请重新登录");
            }
            return buildErrorResponse("请求失败，状态码：" + response.statusCode());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return buildErrorResponse("请求失败，无法连接后端服务：" + serverUrl);
        }
    }

    public static DataResponse uploadMultipart(String uri, String fileName, Map<String, String> params) {
        try {
            String boundary = "----OES" + UUID.randomUUID();
            byte[] fileBytes = java.nio.file.Files.readAllBytes(Path.of(fileName));
            StringBuilder prefix = new StringBuilder();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isBlank()) {
                        continue;
                    }
                    prefix.append("--").append(boundary).append("\r\n")
                            .append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n")
                            .append(entry.getValue()).append("\r\n");
                }
            }
            String safeName = URLEncoder.encode(Path.of(fileName).getFileName().toString(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            prefix.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(safeName).append("\"\r\n")
                    .append("Content-Type: application/octet-stream\r\n\r\n");
            byte[] prefixBytes = prefix.toString().getBytes(StandardCharsets.UTF_8);
            byte[] suffixBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
            System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
            System.arraycopy(fileBytes, 0, body, prefixBytes.length, fileBytes.length);
            System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileBytes.length, suffixBytes.length);
            HttpRequest httpRequest = authorizedBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .build();
            return sendForDataResponse(httpRequest, "UPLOAD " + uri);
        } catch (IOException e) {
            e.printStackTrace();
            return buildErrorResponse("上传失败，无法读取文件：" + fileName);
        }
    }

    public static MyTreeNode requestTreeNode(String url, DataRequest request) {
        HttpRequest httpRequest = buildJsonRequest(url, request);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), MyTreeNode.class);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<MyTreeNode> requestTreeNodeList(String url, DataRequest request) {
        HttpRequest httpRequest = buildJsonRequest(url, request);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Map<String, Object>> list = gson.fromJson(response.body(), List.class);
                List<MyTreeNode> result = new ArrayList<>();
                for (Map<String, Object> item : list) {
                    result.add(new MyTreeNode(item));
                }
                return result;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<OptionItem> requestOptionItemList(String url, DataRequest request) {
        HttpRequest httpRequest = buildJsonRequest(url, request);
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                OptionItemList optionItemList = gson.fromJson(response.body(), OptionItemList.class);
                return optionItemList.getItemList();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<OptionItem> getDictionaryOptionItemList(String code) {
        DataRequest request = new DataRequest();
        request.add("code", code);
        return requestOptionItemList("/api/base/getDictionaryOptionItemList", request);
    }

    public static byte[] requestByteData(String url, DataRequest request) {
        HttpRequest httpRequest = buildJsonRequest(url, request);
        try {
            HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DataResponse uploadFile(String uri, String fileName, String remoteFile) {
        try {
            Path file = Path.of(fileName);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + uri + "?uploader=HttpTestApp&remoteFile=" + remoteFile + "&fileName="
                            + file.getFileName()))
                    .POST(HttpRequest.BodyPublishers.ofFile(file));
            String authorization = getAuthorizationHeader();
            if (authorization != null) {
                builder.header("Authorization", authorization);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
            return buildErrorResponse("上传失败，状态码：" + response.statusCode());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return buildErrorResponse("上传失败，无法连接后端服务：" + serverUrl);
        }
    }

    public static DataResponse importData(String url, String fileName, String paras) {
        try {
            Path file = Path.of(fileName);
            String urlStr = serverUrl + url + "?uploader=HttpTestApp&fileName=" + file.getFileName();
            if (paras != null && !paras.isEmpty()) {
                urlStr += "&" + paras;
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .POST(HttpRequest.BodyPublishers.ofFile(file));
            String authorization = getAuthorizationHeader();
            if (authorization != null) {
                builder.header("Authorization", authorization);
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), DataResponse.class);
            }
            return buildErrorResponse("导入失败，状态码：" + response.statusCode());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return buildErrorResponse("导入失败，无法连接后端服务：" + serverUrl);
        }
    }
}
