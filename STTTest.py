import whisper

#list of the actual sentences spoken in the audio test
correct_prompts = ["She currently works as a co-host of the danish football magazine Onside.",
"A trailing arm design can also be used in an independent suspension arrangement.",
"This is because they believe that their aggressive actions will result in positive outcomes.",
"Safe passage for boats is indicated by red and green marker posts.",
"She completed both her undergraduate and postgraduate studies at the University of Cologne."]

#load whisper base model
model = whisper.load_model("base")
words=0
total_errors = 0

for i in range(5):
    #transcribe each sentence
    result = model.transcribe(f"test{i}.mp4")
    #convert transcription to string
    text = str(result["text"])

    #code for counting errors
    errors = 0
    text = text.lower().split()

    correct=correct_prompts[i].lower().split()
    print(f"correct sentence: {correct}")
    print(f"detected sentence: {text}")
    #go through each detected word and check if it matches the correct spoken word
    for j in range(len(correct)):
        words += 1
        if correct[j] != text[j]:
            errors += 1
            total_errors += 1
    print(f"errors detected: {errors}\n")

#print results
print(f"total words: {words}")
print(f"total errors: {total_errors}")
print(str(100-(total_errors/words * 100)) + "% accuracy")

