package com.localllm.sovereign_ai_workbench.Tools;

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
            Execute Python code inside a secure isolated Docker sandbox.

            Use this tool when you need to:
            - run Python code
            - perform calculations
            - test or verify generated code
            - analyze data using Python
            - generate files programmatically

            The sandbox has no internet access and is resource limited.

            The tool accepts multiple files and an entry file.
            Generated files should be written inside /sandbox/output/.
            """
    )
    public CodeExecutionResult executePythonCode(
            CodeExecutionRequest request) {

        System.out.println("========== TOOL CALLED ==========");
        System.out.println("Entry file: " + request.getEntryFile());
        System.out.println("Files: " + request.getFiles().keySet());

        CodeExecutionResult result =
            codeExecutionService.execute(request);

        System.out.println("========== TOOL FINISHED ==========");
        System.out.println("Exit code: " + result.getExitCode());
        System.out.println("Artifacts: " + result.getArtifacts());

        return result;
    }
}