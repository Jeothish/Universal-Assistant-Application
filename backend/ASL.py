



#######################

import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense
import pkg_resources



##########################################################

def predict(seq):
    sequence = np.array(seq)
    prediction = model.predict(sequence)
    if sequence.shape != (30, 126):
        raise ValueError("Expected shape (30, 126)")

    prediction = model.predict(sequence[np.newaxis, ...], verbose=0)[0]
    confidence = float(np.max(prediction))
    letter = actions[np.argmax(prediction)]

    return {
        "letter": letter,
        "confidence": confidence
    }



#
# def signLanguageRecognizerMethod():
#
#     sequence = []
#     sentence = []
#     predictions = []
#     threshold = 0.2
#     cap = cv2.VideoCapture(0)
#     with mpHolistic.Holistic(min_detection_confidence=0.5, min_tracking_confidence=0.5) as holistic:
#         while cap.isOpened():
#             ret, frame = cap.read()
#             image, results = mediapipeDetection(frame, holistic)
#
#             drawLandmarks(image, results)
#             keypoints = extractKeypoints(results)
#
#             sequence.append(keypoints)
#             sequence = sequence[-30:]
#             if len(sequence) == 30:
#                 res = model.predict(tf.expand_dims(sequence, axis=0))[0]
#                 predictions.append(np.argmax(res))
#                 if np.unique(predictions[-10:])[0] == np.argmax(res):
#                     if (max(res) >= threshold):
#                         if len(sentence) > 0:
#                             if actions[np.argmax(res)] != sentence[-1]:
#                                 sentence.append(actions[np.argmax(res)])
#                         else:
#                             sentence.append(actions[np.argmax(res)])
#
#                         cv2.putText(image, actions[np.argmax(res)]+' : '+str((max(res) * 100).astype(float))+' %',
#                                     (15, 25),
#                                     cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 1, cv2.LINE_AA)
#
#             cv2.imshow("window", image)
#             k = cv2.waitKey(1)
#             if k % 256 == 27:
#                 # ESC pressed
#                 print("\nEscape hit, closing...")
#                 break
#
#         cap.release()
#         cv2.destroyAllWindows()

