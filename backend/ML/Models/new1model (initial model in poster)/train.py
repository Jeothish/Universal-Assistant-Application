import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import EarlyStopping
from sklearn.metrics import confusion_matrix, classification_report
import matplotlib.pyplot as plt
import seaborn as sns
import pickle


# used l2 regulizer, early stopping, 100epochs, 2 dropout layers at 20%


# mediapipe landmarks dataset from
# https://github.com/JaspreetSingh-exe/Sign-Language-Recognition-System

df = pd.read_csv("asl_mediapipe_keypoints_dataset.csv")

X = df.drop(columns=["label"]).values# import dataset and organize
y= df["label"].values
# print(X.shape)

encoder = LabelEncoder() # to orgnize all alphabet labels
y_encode = encoder.fit_transform(y)
print(encoder.classes_)
np.save("asl_labels_new1L.npy", encoder.classes_)

#split into testing and training
X_train, X_temp, y_train, y_temp = train_test_split(X, y_encode, test_size = 0.3, random_state = 42, stratify = y_encode)

X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size = 0.5, random_state = 42, stratify = y_temp)


#train model
model = Sequential([Dense(128, activation="relu", input_shape=(X.shape[1],),kernel_regularizer=l2(0.001)), Dropout(0.2), Dense(64, activation="relu",kernel_regularizer=l2(0.001)),Dropout(0.2), Dense(len(np.unique(y_encode)), activation="softmax")])

model = Sequential([
    Dense(128, activation="relu", input_shape=(X.shape[1],),
          kernel_regularizer=l2(0.001)),
    Dropout(0.2),
    Dense(64, activation="relu",
          kernel_regularizer=l2(0.001)),
    Dropout(0.2),
    Dense(len(np.unique(y_encode)), activation="softmax")
])

early_stop = EarlyStopping(monitor="val_loss", patience=10, restore_best_weights=True)

history = model.fit(X_train, y_train, validation_data=(X_val, y_val), epochs=100, batch_size=32, callbacks=[early_stop], class_weight=class_weight_dict)


#save model
model.save("asl_mediapipe_model_new1L.keras")

with open("training_history_new1L.pkl", "wb") as f:
    pickle.dump(history.history, f)

#acc vs epoch
plt.figure()
plt.plot(history.history['accuracy'])
plt.plot(history.history['val_accuracy'])
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend(['Train', 'Validation'])
plt.title('Accuracy vs Epoch')
plt.savefig("accuracy_vs_epoch.png")
plt.show()

#loss vs epoich
plt.figure()
plt.plot(history.history['loss'])
plt.plot(history.history['val_loss'])
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend(['Train', 'Validation'])
plt.title('Loss vs Epoch')
plt.savefig("loss_vs_epoch.png")
plt.show()

#test model
test_loss, test_acc = model.evaluate(X_test, y_test)


print("Test accuracy:", test_acc)

#
# model = load_model("asl_mediapipe_model_graph_trainL.keras")
#confusion matrix
y_pred_probs = model.predict(X_test)
y_pred = np.argmax(y_pred_probs, axis=1)

cm = confusion_matrix(y_test, y_pred)

plt.figure(figsize=(10,8))
sns.heatmap(cm,
            annot=True,
            fmt="d",
            xticklabels=encoder.classes_,
            yticklabels=encoder.classes_)

plt.xlabel("Predicted")
plt.ylabel("True")
plt.title("Confusion Matrix")
plt.savefig("confusion_matrix.png")
plt.show()

#class. report
print("\nClassification Report:\n")
print(classification_report(y_test, y_pred, target_names=encoder.classes_))