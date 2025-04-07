from fastapi import FastAPI, APIRouter
from fastapi.middleware.cors import CORSMiddleware
from transformers import RobertaForSequenceClassification, AutoTokenizer
import torch

app = FastAPI(
    title="Phobert sentiment analysis", docs_url="/docs", openapi_url="/openapi.json"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow any origin, for more security use specific origins
    allow_credentials=True,
    allow_methods=["*"],  # Allow all methods (GET, POST, etc.)
    allow_headers=["*"],  # Allow all headers
)

chat_router = APIRouter()

# Load the model and tokenizer
model = RobertaForSequenceClassification.from_pretrained("wonrax/phobert-base-vietnamese-sentiment")
tokenizer = AutoTokenizer.from_pretrained("wonrax/phobert-base-vietnamese-sentiment", use_fast=False)

model.save_pretrained("./phobert_vietnamese_sentiment_model")
tokenizer.save_pretrained("./phobert_vietnamese_sentiment_tokenizer")

@chat_router.post("/predict/")
async def predict(text: str):
    # inputs = tokenizer(text, return_tensors="pt", padding=True, truncation=True)
    input_ids = torch.tensor([tokenizer.encode(text)])

    with torch.no_grad():
        outs = model(input_ids).logits.softmax(dim=-1)
    prediction = torch.argmax(outs, dim=-1).item()
    labels = ['NEG', 'POS', 'NEU']
    return {"sentiment": labels[prediction]}

app.include_router(chat_router, tags=["chat"])

