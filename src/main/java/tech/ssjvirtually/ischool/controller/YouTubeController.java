package tech.ssjvirtually.ischool.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class YouTubeController {

    @GetMapping("/extract")
    public String extractJson(@RequestParam String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Element scriptElement = doc.select("script:containsData(ytInitialPlayerResponse)").first();

        if (scriptElement != null) {
            String scriptContent = scriptElement.html();
            Pattern pattern = Pattern.compile("var ytInitialPlayerResponse = (\\{.*?\\});", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(scriptContent);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "{\"error\": \"ytInitialPlayerResponse not found\"}";
    }

    @GetMapping("/chapters")
    public Map<String, Object> getChapters(@RequestParam String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Element scriptElement = doc.select("script:containsData(ytInitialPlayerResponse)").first();

        if (scriptElement != null) {
            String scriptContent = scriptElement.html();
            Pattern pattern = Pattern.compile("var ytInitialPlayerResponse = (\\{.*?\\});", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(scriptContent);
            if (matcher.find()) {
                String json = matcher.group(1);
                Map<String, Object> jsonObject = new ObjectMapper().readValue(json, HashMap.class);
                Map<String, Object> videoDetails = (Map<String, Object>) jsonObject.get("videoDetails");
                String description = (String) videoDetails.get("shortDescription");

                Map<String, Object> response = new HashMap<>();
                response.put("chapters", parseChapters(description));
                response.put("videoId", extractVideoId(url));
                return response;
            }
        }
        return Collections.singletonMap("error", "ytInitialPlayerResponse not found");
    }

    private List<Map<String, String>> parseChapters(String description) {
        List<Map<String, String>> chapters = new ArrayList<>();

        Pattern pattern = Pattern.compile("(\\d{2}:\\d{2}:\\d{2})\\s-\\s(.+)");
        Matcher matcher = pattern.matcher(description);

        while (matcher.find()) {
            Map<String, String> chapter = new HashMap<>();
            chapter.put("time", matcher.group(1));
            chapter.put("title", matcher.group(2).trim());
            chapters.add(chapter);
        }

        return chapters;
    }

    private String extractVideoId(String url) {
        Pattern pattern = Pattern.compile("v=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
