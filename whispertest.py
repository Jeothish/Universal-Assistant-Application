import whisper

# Load model (tiny, base, small, medium, large)
model = whisper.load_model("base")

result = model.transcribe("recording (6).m4a")

print(result["text"])
