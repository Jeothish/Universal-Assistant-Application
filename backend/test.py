import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout


# mediapipe landmarks dataset from
# https://github.com/JaspreetSingh-exe/Sign-Language-Recognition-System

df = pd.read_csv("asl_mediapipe_keypoints_dataset.csv")

X = df.drop(columns=["label"]).values# import dataset and organize
y= df["label"].values
# print(X.shape)

encoder = LabelEncoder() # to orgnize all alphabet labels
y_encode = encoder.fit_transform(y)
print(encoder.classes_)
np.save("asl_labels_og_retrain.npy", encoder.classes_)

#split into testing and training
X_train, X_temp, y_train, y_temp = train_test_split(X, y_encode, test_size = 0.3, random_state = 42, stratify = y_encode)

X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size = 0.5, random_state = 42, stratify = y_temp)

# train model
model = Sequential([Dense(128, activation="relu", input_shape=(X.shape[1],)), Dropout(0.3), Dense(64, activation="relu"), Dense(len(np.unique(y_encode)), activation="softmax")])

model.compile(optimizer=tf.keras.optimizers.Adam(0.001), loss="sparse_categorical_crossentropy", metrics=["accuracy"])

history = model.fit(X_train, y_train, validation_data=(X_val, y_val), epochs=50, batch_size=32)

#test model
test_loss, test_acc = model.evaluate(X_test, y_test)


print("Test accuracy:", test_acc)
#save model
model.save("asl_mediapipe_model_original_retrain.keras")
