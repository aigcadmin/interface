import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Instant;
import org.json.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.json.JSONObject;


class StreamResponseHandler implements Iterable<JSONObject> {
    private HttpClient client;
    private HttpRequest request;

    public StreamResponseHandler(String url, JSONObject prompt) {
        this.client = HttpClient.newHttpClient();
        this.request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(prompt.toString()))
                .build();
    }

    @Override
    public Iterator<JSONObject> iterator() {
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
            return new Iterator<JSONObject>() {
                private String nextLine = null;

                @Override
                public boolean hasNext() {
                    try {
                        if (nextLine != null) {
                            return true;
                        }
                        nextLine = reader.readLine();
                        return nextLine != null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public JSONObject next() {
                    if (nextLine == null && !hasNext()) {
                        throw new IllegalStateException();
                    }
                    String currentLine = nextLine;
                    nextLine = null;
                    return new JSONObject(currentLine);
                }
            };
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to get response", e);
        }
    }
}


class ChatInterface {
    private static final String BASE_URL = "http://47.251.65.65:80";
    private String url;
    private int caseId;
    private Path templatePath;
    private Path localPath;

    public ChatInterface(int caseId) {
        this.url = BASE_URL + "/chat";
        this.caseId = caseId;
        Path root = Paths.get(System.getProperty("user.dir"));
        this.templatePath = root.resolve("buffer/template_dialog.json");
        this.localPath = root.resolve("buffer/dialog_" + caseId + ".json");
    }

    public JSONObject loadDataFromDisk() throws IOException {
        Path path = Files.exists(localPath) ? localPath : templatePath;
        return new JSONObject(Files.readString(path));
    }

    private void updatePrompt(JSONObject prompt, JSONObject updateInfo) {
        for (String key : updateInfo.keySet()) {
            prompt.put(key, updateInfo.get(key));  // 更新或添加每个键值对
        }
    }

    public void postProcess(JSONObject prompt, JSONObject item) throws IOException {
        JSONObject result = new JSONObject();
        result.put("text", "");

        String flag = item.getString("flag");
        switch (flag) {
            case "text":
                System.out.print(item.getString("info"));
//                @前端：这里根据算法返回的文本更新前端的流式输出
                break;
            case "select":
                System.out.println(item.getJSONObject("info"));
//                @前端：这里根据算法返回的选择题更新前端页面
                result.append(flag, item.getJSONObject("info"));
                break;
            case "commodity":
                System.out.println("\n--------commodity info---------");
                JSONArray commodities = item.getJSONArray("info");
                for (int j = 0; j < commodities.length(); j++) {
                    JSONObject commodity = commodities.getJSONObject(j);
//                    @前端：这里根据算法返回的商品显示到前端
                    System.out.println("commodity skuId: " + commodity.getLong("id"));
                }
                result.append("commodity", commodities);
                break;
            case "log":
                updatePrompt(prompt, item.getJSONObject("info"));
//                @后端：这里需要保存历史记录
                Files.writeString(localPath, prompt.toString());
                result.append("log", item.getJSONObject("info"));
                break;
            default:
                System.out.println("error: unknown flag");
        }
    }

    public JSONArray getResponse(JSONObject prompt) throws IOException, InterruptedException {
        StreamResponseHandler handler = new StreamResponseHandler(url, prompt);
        for (JSONObject json : handler) {
            postProcess(prompt, json);
        }
        return new JSONArray();
    }
}

public class Main {
    public static void main(String[] args) {
        try {
            ChatInterface app = new ChatInterface((int) Instant.now().getEpochSecond());

            JSONObject prompt1 = app.loadDataFromDisk();
//            @前端：这里根据用户输入的问题更新prompt1中的current_question，下面这行是模拟
            prompt1.put("current_question", "油性皮肤用什么化妆品好？,请推荐一款适合油性皮肤的欧莱雅粉底液，价格200块以内，适合室外使用。");
            app.getResponse(prompt1);

            JSONObject prompt2 = app.loadDataFromDisk();
//            @前端：这里根据用户输入的选项更新prompt2
            app.getResponse(prompt2);

            JSONObject prompt3 = app.loadDataFromDisk();
//            @前端：这里根据用户的回答更新prompt3
            app.getResponse(prompt3);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

