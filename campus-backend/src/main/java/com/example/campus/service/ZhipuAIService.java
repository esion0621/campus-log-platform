package com.example.campus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ZhipuAIService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AIQueryService aiQueryService;  // 注入数据查询服务

    public ZhipuAIService(
            @Value("${zhipuai.url}") String url,
            @Value("${zhipuai.api-key}") String apiKey,
            @Value("${zhipuai.model}") String model,
            AIQueryService aiQueryService) {
        this.apiKey = apiKey;
        this.model = model;
        this.aiQueryService = aiQueryService;
        this.webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String ask(String question) {
        // 1. 根据问题获取相关数据上下文
        String context = aiQueryService.fetchContext(question);

        // 2. 构建提示词，将数据上下文与问题结合
        String prompt;
        if (!context.isEmpty()) {
            prompt = "以下是校园日志平台的最新数据：\n" + context + "\n请根据以上数据回答用户的问题：" + question;
        } else {
            prompt = question;  // 没有相关数据时，直接提问
        }

        // 3. 调用智谱 AI API
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false
        );

        String responseJson = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseResponse(responseJson);
    }

    private String parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");
                return message.path("content").asText();
            }
            return "抱歉，AI 响应异常。";
        } catch (Exception e) {
            e.printStackTrace();
            return "解析响应失败：" + e.getMessage();
        }
    }
}
