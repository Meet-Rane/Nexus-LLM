package com.localllm.sovereign_ai_workbench.Controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import com.localllm.sovereign_ai_workbench.Dto.ChatRequestDto;
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

    /**
     * Standard non-streaming chat endpoint using request body DTO.
     */
    @PostMapping("/chat")
    public String chat(@Valid @RequestBody ChatRequestDto request) {
        return agentService.chat(request.conversationId(), request.message());
    }

    /**
     * Overload: Non-streaming chat using query parameters.
     */
    @GetMapping("/chat")
    public String chatGet(
        @RequestParam String conversationId,
        @RequestParam String message
    ){
        return agentService.chat(conversationId, message);
    }

    /**
     * Real-time Server-Sent Events (SSE) streaming chat endpoint using request body DTO.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentStreamEvent> streamChatPost(@Valid @RequestBody ChatRequestDto request) {
        return agentService.streamChat(request.conversationId(), request.message());
    }

    /**
     * Real-time Server-Sent Events (SSE) streaming chat endpoint using query parameters.
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentStreamEvent> streamChatGet(
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
