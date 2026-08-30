package com.localllm.sovereign_ai_workbench.Controller;

import org.springframework.web.bind.annotation.*;

import com.localllm.sovereign_ai_workbench.Service.AgentService;


@RestController
@RequestMapping("/ai")
public class ChatController {

    private final AgentService agentService;

    public ChatController(
        AgentService agentService
    ){
        this.agentService= agentService;
    }

    @GetMapping("/chat")
    public String chat(
        @RequestParam String conversationId,
        @RequestParam String message
    ){
        return agentService.chat(conversationId, message);
    }

    @GetMapping("/health")
    public String getHealth(){
        return "Healthy";
    }

}
