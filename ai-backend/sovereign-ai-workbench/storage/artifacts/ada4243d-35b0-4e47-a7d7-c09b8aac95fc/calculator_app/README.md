# FastAPI Calculator Application

A simple calculator API built with FastAPI.

## Installation

```bash
pip install -r requirements.txt
```

## Running the Application

```bash
python main.py
```

Or with uvicorn directly:

```bash
uvicorn main:app --reload
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Welcome message with endpoint list |
| POST | `/add` | Add two numbers |
| POST | `/subtract` | Subtract two numbers |
| POST | `/multiply` | Multiply two numbers |
| POST | `/divide` | Divide two numbers |
| GET | `/docs` | Interactive API documentation (Swagger UI) |
| GET | `/redoc` | Alternative API documentation (ReDoc) |

## Request Format

All calculation endpoints accept JSON with two numbers:

```json
{
  "a": 10,
  "b": 5
}
```

## Response Format

```json
{
  "operation": "addition",
  "a": 10,
  "b": 5,
  "result": 15
}
```

## Example Usage

### Using curl

```bash
# Addition
curl -X POST "http://localhost:8000/add" \
  -H "Content-Type: application/json" \
  -d '{"a": 10, "b": 5}'

# Subtraction
curl -X POST "http://localhost:8000/subtract" \
  -H "Content-Type: application/json" \
  -d '{"a": 10, "b": 5}'

# Multiplication
curl -X POST "http://localhost:8000/multiply" \
  -H "Content-Type: application/json" \
  -d '{"a": 10, "b": 5}'

# Division
curl -X POST "http://localhost:8000/divide" \
  -H "Content-Type: application/json" \
  -d '{"a": 10, "b": 5}'
```

### Using Python requests

```python
import requests

response = requests.post("http://localhost:8000/add", json={"a": 10, "b": 5})
print(response.json())
# {'operation': 'addition', 'a': 10, 'b': 5, 'result': 15}
```

## Error Handling

Division by zero returns a 400 error:

```json
{
  "detail": "Cannot divide by zero"
}
```
