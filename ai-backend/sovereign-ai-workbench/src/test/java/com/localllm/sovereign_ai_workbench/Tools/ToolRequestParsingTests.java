package com.localllm.sovereign_ai_workbench.Tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolRequestParsingTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesDocumentRequestAliases() throws Exception {
        CreateDocumentRequest request = mapper.readValue(
                "{\"filename\":\"approval.docx\",\"documentTitle\":\"Approval\",\"body\":\"Approved\"}",
                CreateDocumentRequest.class
        );

        assertEquals("approval.docx", request.getPath());
        assertEquals("Approval", request.getTitle());
        assertEquals("Approved", request.getContent());
    }

    @Test
    void parsesPlainTextFileRequestFromSmallerModels() throws Exception {
        CreateFileRequest request = mapper.readValue("\"print('ready')\"", CreateFileRequest.class);

        assertEquals("file.txt", request.getPath());
        assertEquals("print('ready')", request.getContent());
    }

    @Test
    void parsesStructuredCodeExecutionRequest() throws Exception {
        CodeExecutionRequest request = mapper.readValue(
                "{\"language\":\"python\",\"entryFile\":\"main.py\",\"files\":{\"main.py\":\"print(42)\"}}",
                CodeExecutionRequest.class
        );

        assertEquals("python", request.getLanguage());
        assertEquals("main.py", request.getEntryFile());
        assertEquals("print(42)", request.getFiles().get("main.py"));
    }
}
