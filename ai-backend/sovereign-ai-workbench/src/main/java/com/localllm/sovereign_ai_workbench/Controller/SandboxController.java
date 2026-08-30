package com.localllm.sovereign_ai_workbench.Controller;

import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionRequest;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionResult;
import com.localllm.sovereign_ai_workbench.Tools.CodeExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sandbox")
public class SandboxController {

    private final CodeExecutionService codeExecutionService;

    public SandboxController(
            CodeExecutionService codeExecutionService) {

        this.codeExecutionService = codeExecutionService;
    }

    @PostMapping("/execute")
    public CodeExecutionResult execute(
            @RequestBody CodeExecutionRequest request) {

        return codeExecutionService.execute(request);
    }
}