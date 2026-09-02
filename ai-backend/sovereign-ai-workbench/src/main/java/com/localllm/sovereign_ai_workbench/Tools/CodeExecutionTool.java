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
            Execute Python code inside a secure isolated Docker sandbox.

            Pre-installed libraries in sandbox:
            - fpdf2 (PDF generation: use pdf.cell(0, 10, text, new_x='LMARGIN', new_y='NEXT') or pdf.write(8, text), output to 'output/filename.pdf')
            - reportlab (PDF generation: SimpleDocTemplate('output/filename.pdf'))
            - python-docx (Word documents)
            - openpyxl (Excel spreadsheets)
            - pandas, pillow

            CRITICAL RULES FOR CALLING THIS TOOL:
            1. All generated files MUST be saved into the 'output/' directory (e.g. 'output/spring_ai_guide.pdf').
            2. ALWAYS provide the Python code inside the 'files' map (e.g. key: 'generate_pdf.py', value: '...python code...') AND set 'entryFile': 'generate_pdf.py'.
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