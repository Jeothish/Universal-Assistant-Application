import pandas as pd
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score

#load data
df = pd.read_csv("../Datasets/asl_mediapipe_keypoints_dataset.csv")
X = df.drop(columns=["label"]).values
y = df["label"].values

encoder = LabelEncoder()
y_encode = encoder.fit_transform(y)

#test split
_, X_test, _, y_test = train_test_split(
    X, y_encode, test_size=0.15, random_state=42, stratify=y_encode
)

#load models
keras_model = tf.keras.models.load_model("../FinalModels/asl_mediapipe_model_final.keras")
interpreter = tf.lite.Interpreter(model_path="../FinalModels/asl_mediapipe_model_finalL.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()


def evaluate_tflite(X_data):
    tflite_preds = []
    for i in range(len(X_data)):
        input_data = np.array([X_data[i]], dtype=np.float32)
        interpreter.set_tensor(input_details[0]['index'], input_data)
        interpreter.invoke()
        output_data = interpreter.get_tensor(output_details[0]['index'])
        tflite_preds.append(np.argmax(output_data))
    return np.array(tflite_preds)

# run test
print("Running keras predctions...")
y_pred_keras = np.argmax(keras_model.predict(X_test, verbose=0), axis=1)
keras_acc = accuracy_score(y_test, y_pred_keras)

print("Running tfLite predictions...")
y_pred_tflite = evaluate_tflite(X_test)
tflite_acc = accuracy_score(y_test, y_pred_tflite)


drop = keras_acc - tflite_acc

print(f"Keras Accuracy:    {keras_acc*100:.2f}%")
print(f"tflite Accuracy:   {tflite_acc*100:.2f}%")
print(f"Accuracy Drop:     {drop*100:.2f}%")
