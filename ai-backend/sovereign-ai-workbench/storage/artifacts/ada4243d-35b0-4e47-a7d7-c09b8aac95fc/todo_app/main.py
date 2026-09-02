from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel, Field
from typing import List, Optional
from uuid import uuid4

app = FastAPI(title="Todo API", version="1.0.0")

# In-memory storage (replace with database in production)
todos_db = {}

# Pydantic Models
class TodoBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=100, description="Todo title")
    description: Optional[str] = Field(None, max_length=500, description="Todo description")
    completed: bool = Field(False, description="Completion status")

class TodoCreate(TodoBase):
    pass

class TodoUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=100)
    description: Optional[str] = Field(None, max_length=500)
    completed: Optional[bool] = None

class Todo(TodoBase):
    id: str

    class Config:
        from_attributes = True

# Routes
@app.get("/", tags=["Root"])
async def root():
    return {"message": "Welcome to Todo API", "docs": "/docs"}

@app.post("/todos", response_model=Todo, status_code=status.HTTP_201_CREATED, tags=["Todos"])
async def create_todo(todo: TodoCreate):
    """Create a new todo item"""
    todo_id = str(uuid4())
    new_todo = Todo(id=todo_id, **todo.model_dump())
    todos_db[todo_id] = new_todo
    return new_todo

@app.get("/todos", response_model=List[Todo], tags=["Todos"])
async def get_todos(completed: Optional[bool] = None):
    """Get all todos, optionally filter by completion status"""
    todos = list(todos_db.values())
    if completed is not None:
        todos = [t for t in todos if t.completed == completed]
    return todos

@app.get("/todos/{todo_id}", response_model=Todo, tags=["Todos"])
async def get_todo(todo_id: str):
    """Get a specific todo by ID"""
    if todo_id not in todos_db:
        raise HTTPException(status_code=404, detail="Todo not found")
    return todos_db[todo_id]

@app.patch("/todos/{todo_id}", response_model=Todo, tags=["Todos"])
async def update_todo(todo_id: str, todo_update: TodoUpdate):
    """Update a todo (partial update)"""
    if todo_id not in todos_db:
        raise HTTPException(status_code=404, detail="Todo not found")
    
    stored_todo = todos_db[todo_id]
    update_data = todo_update.model_dump(exclude_unset=True)
    updated_todo = stored_todo.model_copy(update=update_data)
    todos_db[todo_id] = updated_todo
    return updated_todo

@app.put("/todos/{todo_id}", response_model=Todo, tags=["Todos"])
async def replace_todo(todo_id: str, todo: TodoCreate):
    """Replace a todo completely"""
    if todo_id not in todos_db:
        raise HTTPException(status_code=404, detail="Todo not found")
    
    updated_todo = Todo(id=todo_id, **todo.model_dump())
    todos_db[todo_id] = updated_todo
    return updated_todo

@app.delete("/todos/{todo_id}", status_code=status.HTTP_204_NO_CONTENT, tags=["Todos"])
async def delete_todo(todo_id: str):
    """Delete a todo"""
    if todo_id not in todos_db:
        raise HTTPException(status_code=404, detail="Todo not found")
    del todos_db[todo_id]
    return None

@app.delete("/todos", status_code=status.HTTP_204_NO_CONTENT, tags=["Todos"])
async def delete_completed_todos():
    """Delete all completed todos"""
    global todos_db
    completed_ids = [tid for tid, todo in todos_db.items() if todo.completed]
    for tid in completed_ids:
        del todos_db[tid]
    return {"deleted_count": len(completed_ids)}