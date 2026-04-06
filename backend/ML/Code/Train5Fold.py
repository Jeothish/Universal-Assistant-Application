import pandas as pd
import numpy as np
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split, StratifiedKFold
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization
from tensorflow.keras.callbacks import EarlyStopping, Callback
from sklearn.metrics import confusion_matrix, classification_report
import matplotlib.pyplot as plt
import seaborn as sns
import pickle
from sklearn.utils.class_weight import compute_class_weight


# mediapipe landmarks dataset from
# https://github.com/JaspreetSingh-exe/Sign-Language-Recognition-System

df = pd.read_csv("../Datasets/asl_landmarks_extracted_2.csv")#load data

X = df.drop(columns=["label"]).values#prep data
y = df["label"].values

encoder = LabelEncoder()#encode data (letters to ints)
y_encode = encoder.fit_transform(y)
print("Classes:", encoder.classes_)
np.save("../Labels/asl_labels_R.npy", encoder.classes_)

#test data
X_trainval, X_test, y_trainval, y_test = train_test_split(
    X, y_encode, test_size=0.15, random_state=42, stratify=y_encode#15% as test data
)#strat = ensure equal % of each letter


def build_model(input_dim, num_classes):
    model = Sequential([#seq = layer by layer build
        Dense(128, activation="relu", input_shape=(input_dim,)),#rectified linear unit, input shape =63
        Dropout(0.1),
        Dense(64, activation="relu"),
        Dense(num_classes, activation="softmax")#num classes = 28, softmax = o.p. prob.
    ])
    model.compile(
        optimizer=tf.keras.optimizers.Adam(0.0005),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )
    return model

#5fold cross val #########################################################################################################################
kfold = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

fold_train_accs = []
fold_val_accs   = []
fold_histories  = []

print("\n5fold val")

for fold, (train_idx, val_idx) in enumerate(kfold.split(X_trainval, y_trainval)):

    print(f"  FOLD {fold + 1} / 5")


    X_fold_train, X_fold_val = X_trainval[train_idx], X_trainval[val_idx]
    y_fold_train, y_fold_val = y_trainval[train_idx], y_trainval[val_idx]

    class_weights = compute_class_weight(
        'balanced', classes=np.unique(y_fold_train), y=y_fold_train
    )
    class_weight_dict = dict(enumerate(class_weights))

    fold_model = build_model(X.shape[1], len(encoder.classes_))

    early_stop = EarlyStopping(
        monitor="val_loss", patience=10, restore_best_weights=True
    )
    lr_schedule = tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.5, patience=5, min_lr=1e-5
    )

    history = fold_model.fit(
        X_fold_train, y_fold_train,
        validation_data=(X_fold_val, y_fold_val),
        epochs=100,
        batch_size=32,
        callbacks=[early_stop, lr_schedule],
        class_weight=class_weight_dict,
        verbose=0
    )

    _, train_acc = fold_model.evaluate(X_fold_train, y_fold_train, verbose=0)
    _, val_acc   = fold_model.evaluate(X_fold_val,   y_fold_val,   verbose=0)

    fold_train_accs.append(train_acc)
    fold_val_accs.append(val_acc)
    fold_histories.append(history.history)

    print(f"\n  Fold {fold + 1} Results:")
    print(f"  Train Acc : {train_acc*100:.2f}%")
    print(f"  Val   Acc : {val_acc*100:.2f}%")

print("\nCROSS VALIDATION")
print(f"Mean Train Accuracy : {np.mean(fold_train_accs)*100:.2f}% +- {np.std(fold_train_accs)*100:.2f}%")
print(f"Mean Val   Accuracy : {np.mean(fold_val_accs)*100:.2f}%   +- {np.std(fold_val_accs)*100:.2f}%")

#graph shwoing all folds acc / epoch
plt.figure(figsize=(10, 5))
for i, h in enumerate(fold_histories):
    plt.plot(h['val_accuracy'], alpha=0.7, linewidth=2, label=f'Fold {i+1}')
plt.xlabel('Epoch')
plt.ylabel('Validation Accuracy')
plt.title('Validation Accuracy per Fold')
plt.legend()
plt.tight_layout()
plt.savefig("cv_val_accuracy_per_fold.png")
plt.show()
###############################################################################################################################################

print("\nFinal Model")

X_train, X_val, y_train, y_val = train_test_split(
    X_trainval, y_trainval, test_size=0.15, random_state=42, stratify=y_trainval
)

class_weights = compute_class_weight(
    'balanced', classes=np.unique(y_train), y=y_train
)
class_weight_dict = dict(enumerate(class_weights))

final_model = build_model(X.shape[1], len(encoder.classes_))

early_stop = EarlyStopping(
    monitor="val_loss", patience=10, restore_best_weights=True
)
lr_schedule = tf.keras.callbacks.ReduceLROnPlateau(
    monitor='val_loss', factor=0.5, patience=5, min_lr=1e-5
)

history = final_model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=100,
    batch_size=32,
    callbacks=[early_stop, lr_schedule],
    class_weight=class_weight_dict,
    verbose=0
)

final_model.save("../FinalModels/asl_mediapipe_model_final_R.keras")

with open("../Models/training_history_final.pkl", "wb") as f:
    pickle.dump(history.history, f)


plt.figure()
plt.plot(history.history['accuracy'],     label='Train',      color='steelblue', linewidth=2)
plt.plot(history.history['val_accuracy'], label='Validation', color='orange',    linewidth=2)
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend()
plt.title('Accuracy vs Epoch (Final Model)')
plt.tight_layout()
plt.savefig("accuracy_vs_epoch_final.png")
plt.show()

plt.figure()
plt.plot(history.history['loss'],     label='Train',      color='steelblue', linewidth=2)
plt.plot(history.history['val_loss'], label='Validation', color='orange',    linewidth=2)
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend()
plt.title('Loss vs Epoch (Final Model)')
plt.tight_layout()
plt.savefig("loss_vs_epoch_final.png")
plt.show()

#summary table for train/test/val
train_loss, train_acc = final_model.evaluate(X_train, y_train, verbose=0)
val_loss,   val_acc   = final_model.evaluate(X_val,   y_val,   verbose=0)
test_loss,  test_acc  = final_model.evaluate(X_test,  y_test,  verbose=0)

print("\nACCURACY SUMMARY TABLE")
results_df = pd.DataFrame({
    "Split":    ["Training", "Validation", "Testing"],
    "Accuracy": [f"{train_acc*100:.2f}%", f"{val_acc*100:.2f}%", f"{test_acc*100:.2f}%"],
    "Loss":     [f"{train_loss:.4f}",     f"{val_loss:.4f}",     f"{test_loss:.4f}"]
})
print(results_df.to_string(index=False))
print(f"\nCross-Val Mean Val Accuracy : {np.mean(fold_val_accs)*100:.2f}% ± {np.std(fold_val_accs)*100:.2f}%")


#conf matrixs
y_pred_probs = final_model.predict(X_test)
y_pred = np.argmax(y_pred_probs, axis=1)

cm = confusion_matrix(y_test, y_pred)

plt.figure(figsize=(10, 8))
sns.heatmap(cm,
            annot=True,
            fmt="d",
            xticklabels=encoder.classes_,
            yticklabels=encoder.classes_)
plt.xlabel("Predicted")
plt.ylabel("True")
plt.title("Confusion Matrix (Test Set)")
plt.tight_layout()
plt.savefig("confusion_matrix.png")
plt.show()


print("\nClassification Report:\n")
print(classification_report(y_test, y_pred, target_names=encoder.classes_))