package com.simplecrawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleCrawler {
    public void crawl(String url) {
        try {
            String content = downloadUrl(url);
            System.out.println("Downloaded: " + url);
            
            List<String> links = extractLinks(content);
            System.out.println("Found " + links.size() + " links:");
            for (String link : links) {
                System.out.println(link);
            }
        } catch (Exception e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        }
    }

    private String downloadUrl(String url) throws Exception {
        StringBuilder content = new StringBuilder();
        URL urlObject = new URL(url);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlObject.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    private List<String> extractLinks(String content) {
        List<String> links = new ArrayList<>();
        Pattern pattern = Pattern.compile("href=\"(http[s]?://[^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }
}