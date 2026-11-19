"""
ML microservice for LexiQuery.

  POST /link      semantic schema linking   (sentence-transformers embeddings + cosine similarity)
  POST /classify  query complexity          (TF-IDF + Logistic Regression)  -> SIMPLE | MEDIUM | COMPLEX
  POST /risk      injection / jailbreak     (TF-IDF char n-grams + Logistic Regression) -> probability
  GET  /health

Run:  uvicorn app:app --port 8000
The seed datasets in data/ are deliberately small; grow them (and report cross-validated metrics) for your paper.
"""
import re
from functools import lru_cache
from typing import List

import numpy as np
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import make_pipeline

app = FastAPI(title="LexiQuery ML service")

embedder = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")


def _train(csv, text_col, label_col, analyzer="word", ngram=(1, 2)):
    df = pd.read_csv(csv)
    model = make_pipeline(
        TfidfVectorizer(analyzer=analyzer, ngram_range=ngram, lowercase=True),
        LogisticRegression(max_iter=2000, class_weight="balanced"),
    )
    model.fit(df[text_col], df[label_col])
    return model


complexity_model = _train("data/complexity.csv", "question", "label")
injection_model = _train("data/injection.csv", "text", "label", analyzer="char_wb", ngram=(3, 5))


@lru_cache(maxsize=4096)
def _embed(text: str) -> np.ndarray:
    return embedder.encode(text, normalize_embeddings=True)


def _humanize(name: str) -> str:
    return re.sub(r"[_\s]+", " ", name).strip().lower()


class TableIn(BaseModel):
    name: str
    columns: List[str]


class LinkReq(BaseModel):
    question: str
    tables: List[TableIn]
    top_k: int = 3


class TextReq(BaseModel):
    text: str


@app.post("/link")
def link(req: LinkReq):
    q = _embed(req.question)
    ranked = []
    for t in req.tables:
        # table score = best match among the table name, "table + all columns", and individual columns
        candidates = [_humanize(t.name), _humanize(t.name) + " " + " ".join(_humanize(c) for c in t.columns)]
        candidates += [_humanize(t.name) + " " + _humanize(c) for c in t.columns]
        score = max(float(np.dot(q, _embed(c))) for c in candidates)
        ranked.append({"name": t.name, "score": score})
    ranked.sort(key=lambda r: r["score"], reverse=True)
    return {"tables": ranked[: req.top_k]}


@app.post("/classify")
def classify(req: TextReq):
    probs = complexity_model.predict_proba([req.text])[0]
    idx = int(np.argmax(probs))
    return {"label": str(complexity_model.classes_[idx]), "confidence": float(probs[idx])}


@app.post("/risk")
def risk(req: TextReq):
    classes = list(injection_model.classes_)
    p = injection_model.predict_proba([req.text])[0][classes.index(1)]
    return {"score": float(p)}


@app.get("/health")
def health():
    return {"status": "ok"}
