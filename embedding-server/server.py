from typing import List, Optional, Union

from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer


app = FastAPI(title="Local Embedding Server")


# 这里可以换成你想要的本地模型，比如 BGE、m3e 等
MODEL_NAME = "BAAI/bge-m3"
model = SentenceTransformer(MODEL_NAME)


class EmbeddingsRequest(BaseModel):
    model: Optional[str] = None
    input: Union[str, List[str]]


class EmbeddingData(BaseModel):
    embedding: List[float]


class EmbeddingsResponse(BaseModel):
    data: List[EmbeddingData]


@app.post("/v1/embeddings", response_model=EmbeddingsResponse)
async def embeddings(req: EmbeddingsRequest) -> EmbeddingsResponse:
    if isinstance(req.input, str):
        texts = [req.input]
    else:
        texts = req.input

    # 这里只用一个固定本地模型，忽略传入的 model 字段
    vectors = model.encode(texts, normalize_embeddings=False)
    if not isinstance(vectors, list):
        vectors = vectors.tolist()

    # 当前 Java 端一次只传一个 input，这里仍然返回 data[0].embedding
    first_vec = vectors[0]
    return EmbeddingsResponse(data=[EmbeddingData(embedding=list(first_vec))])


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8001)

