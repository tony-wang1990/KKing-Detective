package com.tony.kingdetective.utils.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.utils
 * @className: DuckDuckGoSearchService
 * @author: Tony Wang
 * @date: 2025/9/22 22:43
 */
@Service
@Slf4j
public class DuckDuckGoSearchService {

    private Mono<List<String>> searchWikipedia(String query) {
        return Mono.fromCallable(() -> {
            String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8);

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/140.0.0.0 Safari/537.36");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                //  JSON
                return List.of(response.toString());
            } finally {
                con.disconnect();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<List<String>> searchDuckDuckGo(String query) {
        return Mono.fromCallable(() -> {
            String url = "https://api.duckduckgo.com/?q=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8) + "&format=json&no_html=1";

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/140.0.0.0 Safari/537.36");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                //  JSON
                return List.of(response.toString());
            } finally {
                con.disconnect();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<List<String>> searchHtmlDuckDuckGo(String query) {
        return Mono.fromCallable(() -> {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://duckduckgo.com/html/?q=" + encodedQuery;

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                return parseResults(response.toString());
            } finally {
                con.disconnect();
            }
        }).subscribeOn(Schedulers.boundedElastic()); //  Reactor 
    }

    private List<String> parseResults(String html) {
        List<String> results = new ArrayList<>();

        //  DuckDuckGo HTML 
        Pattern pattern = Pattern.compile("<a[^>]+class=\"result__a\"[^>]*>(.*?)</a>");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find() && results.size() < 5) {
            String text = matcher.group(1)
                    .replaceAll("<.*?>", "")   //  HTML 
                    .replaceAll("&amp;", "&")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .trim();

            if (!text.isEmpty()) {
                results.add(text);
            }
        }

        return results;
    }

    public Mono<List<String>> search(String query) {
        Mono<List<String>> wikiMono = searchWikipedia(query)
                .doOnNext(results -> log.info("????[{}]?Wikipedia ?????{}", query.trim(), results))
                .onErrorResume(e -> {
                    log.error("Wikipedia ?????{}", e.getMessage());
                    return Mono.just(List.of());
                });

        Mono<List<String>> duckDuckGoMono = searchDuckDuckGo(query)
                .doOnNext(results -> log.info("????[{}]?DuckDuckGo ?????{}", query.trim(), results))
                .onErrorResume(e -> {
                    log.error("DuckDuckGo ?????{}", e.getMessage());
                    return Mono.just(List.of());
                });

        Mono<List<String>> duckDuckGoHtmlMono = searchHtmlDuckDuckGo(query)
                .doOnNext(results -> log.info("????[{}]?DuckDuckGo HTML ?????{}", query.trim(), results))
                .onErrorResume(e -> {
                    log.error("DuckDuckGo HTML ?????{}", e.getMessage());
                    return Mono.just(List.of());
                });

        return Mono.zip(wikiMono, duckDuckGoMono, duckDuckGoHtmlMono)
                .map(tuple3 -> {
                    List<String> combined = new ArrayList<>();
                    combined.addAll(tuple3.getT1());
                    combined.addAll(tuple3.getT2());
                    combined.addAll(tuple3.getT3());
                    return combined;
                });
    }

    public Mono<List<String>> searchWithHtml(String query) {
        return searchHtmlDuckDuckGo(query).doOnNext(results -> log.info("????[{}]?HTML ?????{}", query.trim(), results));
    }

    public static void main(String[] args) {
        DuckDuckGoSearchService service = new DuckDuckGoSearchService();
        service.search("oracle").block();
    }
}