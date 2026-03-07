from tensorflow.keras.models import load_model
import numpy as np
import io

model = load_model("asl_mediapipe_model_new4L.keras")
model.summary()
total_bytes = 0

print("\nWeight dtypes:")
for layer in model.layers:
    weights = layer.get_weights()
    for i, w in enumerate(weights):
        total_bytes += w.nbytes
        print(f"  {layer.name} weight[{i}]: shape={w.shape}, dtype={w.dtype}, bytes={w.nbytes}")
print(f"\ntotal size: {total_bytes/1000} kB")