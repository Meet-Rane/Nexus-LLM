package com.localllm.sovereign_ai_workbench.Tools;

import com.localllm.sovereign_ai_workbench.Config.ConversationContextHolder;
import com.localllm.sovereign_ai_workbench.Dto.AgentStreamEvent;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CodeExecutionTool {

    private final CodeExecutionService codeExecutionService;

    public CodeExecutionTool(
            CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @Tool(
        name = "execute_python_code",
        description = """
            Execute Python code, algorithms, simulations, data analysis, or numerical calculations inside a secure isolated Docker sandbox.

            Pre-installed libraries in sandbox:
            - pandas, openpyxl, pillow

            CRITICAL RULES FOR CALLING THIS TOOL:
            1. NEVER use interactive 'input()' statements. Always define test inputs, arguments, and parameters directly in the code.
            2. Always use 'print(...)' to output calculation results, logs, and algorithmic steps.
            3. Use this tool for computational algorithms, simulations, data transformations, and running Python code.
            4. DO NOT use this tool to generate PDF or Word documents (use 'create_formatted_document' instead).
            5. All generated files MUST be saved into the 'output/' directory (e.g. 'output/results.csv').
            6. ALWAYS provide the Python code inside the 'files' map (e.g. key: 'main.py', value: '...python code...') AND set 'entryFile': 'main.py'.
            """
    )
    public CodeExecutionResult executePythonCode(
            CodeExecutionRequest request) {

        System.out.println("========== TOOL CALLED ==========");
        System.out.println("Entry file: " + request.getEntryFile());
        if (request.getFiles() != null) {
            System.out.println("Files: " + request.getFiles().keySet());
        }

        ConversationContextHolder.emitEvent(AgentStreamEvent.toolStart(
            "execute_python_code", 
            "Executing Python script '" + request.getEntryFile() + "' in Docker sandbox"
        ));

        try {
            CodeExecutionResult result = codeExecutionService.execute(request);

            System.out.println("========== TOOL FINISHED ==========");
            System.out.println("Exit code: " + result.getExitCode());
            System.out.println("Artifacts: " + result.getArtifacts());

            String completionMsg = "Execution finished with exit code " + result.getExitCode() + 
                                   (result.getArtifacts() != null && !result.getArtifacts().isEmpty() ? 
                                    ". Created artifacts: " + result.getArtifacts() : "");

            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("execute_python_code", completionMsg));

            return result;
        } catch (Exception e) {
            ConversationContextHolder.emitEvent(AgentStreamEvent.toolComplete("execute_python_code", "Execution failed: " + e.getMessage()));
            throw e;
        }
    }
}