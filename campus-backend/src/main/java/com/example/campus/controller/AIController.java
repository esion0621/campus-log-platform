package com.example.campus.controller;

import com.example.campus.dto.ChatRequest;
import com.example.campus.dto.ChatResponse;
import com.example.campus.service.ZhipuAIService;  
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final ZhipuAIService aiService; 

    public AIController(ZhipuAIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = aiService.ask(request.getQuestion());
        return new ChatResponse(answer);
    }
}
