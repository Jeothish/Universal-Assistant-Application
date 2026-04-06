import tensorflow as tf
import numpy as np

model = tf.keras.models.load_model("../FinalModels/sasl_mediapipe_model_final_R.keras")

converter = tf.lite.TFLiteConverter.from_keras_model(model)

#Quantize model
converter.optimizations = [tf.lite.Optimize.DEFAULT]

tflite_model = converter.convert()

with open("../FinalModels/asl_mediapipe_model_finalR.tflite", "wb") as f:
    f.write(tflite_model)

#convert numpy labels to txt file

labels = np.load("../Labels/asl_labels_R.npy", allow_pickle=True)
with open("../Labels/asl_labels_finalR.txt", "w") as l:
    l.write("\n".join(str(l) for l in labels))