from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Union

app = FastAPI(title="Calculator API", version="1.0.0")


class CalculationRequest(BaseModel):
    a: float
    b: float


class CalculationResponse(BaseModel):
    operation: str
    a: float
    b: float
    result: float


@app.get("/")
def root():
    return {
        "message": "Welcome to the Calculator API",
        "endpoints": {
            "add": "POST /add",
            "subtract": "POST /subtract",
            "multiply": "POST /multiply",
            "divide": "POST /divide",
            "docs": "/docs"
        }
    }


@app.post("/add", response_model=CalculationResponse)
def add(request: CalculationRequest):
    return CalculationResponse(
        operation="addition",
        a=request.a,
        b=request.b,
        result=request.a + request.b
    )


@app.post("/subtract", response_model=CalculationResponse)
def subtract(request: CalculationRequest):
    return CalculationResponse(
        operation="subtraction",
        a=request.a,
        b=request.b,
        result=request.a - request.b
    )


@app.post("/multiply", response_model=CalculationResponse)
def multiply(request: CalculationRequest):
    return CalculationResponse(
        operation="multiplication",
        a=request.a,
        b=request.b,
        result=request.a * request.b
    )


@app.post("/divide", response_model=CalculationResponse)
def divide(request: CalculationRequest):
    if request.b == 0:
        raise HTTPException(status_code=400, detail="Cannot divide by zero")
    return CalculationResponse(
        operation="division",
        a=request.a,
        b=request.b,
        result=request.a / request.b
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
