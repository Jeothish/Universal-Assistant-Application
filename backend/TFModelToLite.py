import tensorflow as tf
import numpy as np

model = tf.keras.models.load_model("asl_mediapipe_model_custom_og.keras")

converter = tf.lite.TFLiteConverter.from_keras_model(model)

#Quantize model
converter.optimizations = [tf.lite.Optimize.DEFAULT]

tflite_model = converter.convert()

with open("asl_mediapipe_model_R.tflite", "wb") as f:
    f.write(tflite_model)

#convert numpy labels to txt file

labels = np.load("asl_labels_og_retrain.npy", allow_pickle=True)
with open("asl_labels_R.txt", "w") as l:
    l.write("\n".join(str(l) for l in labels))