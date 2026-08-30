package com.localllm.sovereign_ai_workbench.Controller;

import org.springframework.web.bind.annotation.*;

import com.localllm.sovereign_ai_workbench.Service.AgentService;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionService;


@RestController
@RequestMapping("/ai")
public class ChatController {

    private final AgentService agentService;
    private final CodeExecutionService codeExecutionService;

    public ChatController(
        AgentService agentService,
        CodeExecutionService codeExecutionService
    ){
        this.agentService= agentService;
        this.codeExecutionService = codeExecutionService;
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
