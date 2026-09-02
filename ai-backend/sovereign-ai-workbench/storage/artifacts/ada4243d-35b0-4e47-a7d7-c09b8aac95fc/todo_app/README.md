# FastAPI Todo Application

A simple RESTful API for managing todos built with FastAPI.

## Features

- ✅ Create, Read, Update, Delete (CRUD) operations
- 🔍 Filter todos by completion status
- 📝 Partial updates (PATCH) and full replacements (PUT)
- 🗑️ Bulk delete completed todos
- 📚 Auto-generated API documentation (Swagger UI & ReDoc)
- ✨ Input validation with Pydantic

## Quick Start

### 1. Install dependencies
```bash
pip install -r requirements.txt
```

### 2. Run the server
```bash
uvicorn main:app --reload
```

### 3. Access the API
- **API Base URL**: http://localhost:8000
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Welcome message |
| POST | `/todos` | Create a new todo |
| GET | `/todos` | List all todos (filter: `?completed=true/false`) |
| GET | `/todos/{id}` | Get a specific todo |
| PATCH | `/todos/{id}` | Partially update a todo |
| PUT | `/todos/{id}` | Fully replace a todo |
| DELETE | `/todos/{id}` | Delete a todo |
| DELETE | `/todos` | Delete all completed todos |

## Example Usage

### Create a todo
```bash
curl -X POST "http://localhost:8000/todos" \
  -H "Content-Type: application/json" \
  -d '{"title": "Learn FastAPI", "description": "Build a todo app"}'
```

### Get all todos
```bash
curl "http://localhost:8000/todos"
```

### Get incomplete todos only
```bash
curl "http://localhost:8000/todos?completed=false"
```

### Update a todo (mark complete)
```bash
curl -X PATCH "http://localhost:8000/todos/{todo_id}" \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'
```

### Delete a todo
```bash
curl -X DELETE "http://localhost:8000/todos/{todo_id}"
```

## Data Model

```json
{
  "id": "uuid-string",
  "title": "string (1-100 chars)",
  "description": "string (optional, max 500 chars)",
  "completed": boolean
}
```

## Project Structure
```
todo_app/
├── main.py          # FastAPI application
├── requirements.txt # Python dependencies
└── README.md        # This file
```

## Next Steps (Production Ready)

- Add a real database (PostgreSQL, SQLite with SQLAlchemy)
- Add authentication & authorization (JWT, OAuth2)
- Add pagination for list endpoints
- Add request logging & monitoring
- Add unit & integration tests
- Containerize with Docker
- Add environment-based configuration