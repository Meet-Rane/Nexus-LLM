package com.localllm.sovereign_ai_workbench.Controller;

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
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
        this.agentService = agentService;
        this.codeExecutionService = codeExecutionService;
    }

    @GetMapping("/chat")
    public String chat(
        @RequestParam String conversationId,
        @RequestParam String message
    ){
        return agentService.chat(conversationId, message);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentStreamEvent> streamChat(
        @RequestParam String conversationId,
        @RequestParam String message
    ){
        return agentService.streamChat(conversationId, message);
    }

    @GetMapping("/health")
    public String getHealth(){
        return "Healthy";
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<Message>> getChatHistory(
            @PathVariable String conversationId) {

        return ResponseEntity.ok(
                agentService.getChatHistory(conversationId)
        );
    }

}
