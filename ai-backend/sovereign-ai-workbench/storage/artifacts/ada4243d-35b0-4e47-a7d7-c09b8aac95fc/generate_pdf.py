from fpdf import FPDF

pdf = FPDF()
pdf.add_page()
pdf.set_font('Helvetica', size=12)
steps = [
    '1. Install Java JDK 17 or later and set JAVA_HOME.',
    '2. Create a Spring Initializr project with Spring Web and Spring AI dependencies.',
    '3. Add Ollama starter dependency (Maven: spring-ai-ollama-starter).',
    '4. Configure application.yml to point to local Ollama (base-url: http://localhost:11434, model: llama2).',
    '5. Write a service class using OllamaChatModel to generate text.',
    '6. Create a REST controller with a /generate endpoint that calls the service.',
    '7. Run the app (mvn spring:run or gradlew bootRun).',
    '8. Test with curl -X POST http://localhost:8080/generate -H "Content-Type: application/json" -d {"prompt": "Hello"}'.
]
for i, step in enumerate(steps, 1):
    pdf.cell(0, 10, f'{i}. {step}', ln=True)
pdf.output('output/spring_ai_local_llm.pdf')
print('PDF generated')