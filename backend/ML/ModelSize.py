from tensorflow.keras.models import load_model
import tensorflow as tf


model = load_model("asl_mediapipe_model.keras")
model.summary()
total_bytes = 0

print("\nWeight dtypes:")
for layer in model.layers:
    weights = layer.get_weights()
    for i, w in enumerate(weights):
        total_bytes += w.nbytes
        print(f"  {layer.name} weight[{i}]: shape={w.shape}, dtype={w.dtype}, bytes={w.nbytes}")
print(f"\nOriginal model total size: {total_bytes/1000} kB")



w = 0
def tfliteSize(tflite_path):
    global w
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()

    details = interpreter.get_tensor_details()
    total_weight_bytes = 0


    print(f"\nTensor Name | Shape | DType | Bytes")


    for tensor in details:
        try:

            t_data = interpreter.tensor(tensor['index'])()


            if t_data is not None and t_data.size > 0:

                nbytes = int(t_data.nbytes)

                total_weight_bytes += nbytes
                print(f"{tensor['name'][:45]} | {str(tensor['shape'])} | {str(tensor['dtype'].__name__)} | {nbytes}")
                if str(tensor['dtype'].__name__) == "int8":
                    w += nbytes


        except (ValueError, RuntimeError):
            continue

    kb = total_weight_bytes / 1024
    return kb


try:
    size = tfliteSize("asl_mediapipe_model_L.tflite")

    print(f"\ntfLite weight (int8 only) size: {w/1024:.2f} KB")
    print(f"Total tfLite model Size: {size:.2f} KB")

except Exception as e:
    print(f"Error: {e}")