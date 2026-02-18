
import numpy as np
from openai import OpenAI
from sklearn.metrics.pairwise import cosine_similarity


client = OpenAI(
    base_url="http://localhost:1234/v1",
    api_key="lm-studio"
)

#examples for training every time the server restarts
TRAINING_EXAMPLES = {
    "weather": [
        "What's the weather like today?",
        "Will it rain tomorrow?",
        "Show me the weather forecast for Dublin",
        "What's the temperature in San Francisco?",
        "Is it going to be sunny this weekend?",
        "Weather forecast for next Tuesday",
        "What's the weather in Galway at 3pm tomorrow?",
        "Tell me the current weather conditions",
        "How cold will it be tonight?",
        "Do I need an umbrella today?",
        "What's the forecast for the next 5 days?",
        "Will it snow in Dublin this week?",
        "Is it windy outside right now?",
        "What's the temperature going to be at noon?",
        "Show me tomorrow's weather in London",
        "How's the weather looking for Friday?",
        "What should I wear today based on the weather?",
        "Is there a storm coming?",
        "Check the weather for Paris next Monday",
        "What's the humidity level today?"
    ],
    "news": [
        "What's the latest news?",
        "Show me today's headlines",
        "Get me the top news stories",
        "What's happening in Irish politics?",
        "Latest tech news please",
        "Give me business news from USA",
        "What's trending in the news today?",
        "Show me sports headlines",
        "Any breaking news right now?",
        "Get entertainment news",
        "What's happening in world news?",
        "Show me science news articles",
        "Latest health news updates",
        "Give me news from BBC",
        "What's new in technology?",
        "Show me Irish news headlines",
        "Any political updates today?",
        "Updates on the Olympics",
        "What's happening in the environment sector?",
        "Show me education news"
        "updates on the leaving certificate"
    ],
    "chat": [
        "Hello, how are you?",
        "What can you help me with?",
        "Tell me a joke",
        "Good morning!",
        "Thanks for your help",
        "That's interesting",
        "Can you explain how you work?",
        "What's your name?",
        "How's it going?",
        "Nice to meet you",
        "What are your capabilities?",
        "I appreciate your assistance",
        "You're very helpful",
        "Tell me about yourself",
        "What time is it?",
        "How do I use this service?",
        "Who created you?",
        "Have a great day!",
        "Thank you so much",
        "who's Taylor Swift?",
    ]
}



class SimpleIntentClassifier:


    def __init__(self):
        self.intent_embeddings = {}
        self.threshold = 0.5
        self.is_trained = False

    def get_embedding(self, text: str) -> np.ndarray:

        try:
            response = client.embeddings.create(
                model="local-model",
                input=text
            )
            return np.array(response.data[0].embedding)
        except Exception as e:
            print(f"Error getting embedding: {e}")
            return None

    def train(self, examples: dict = None): #train model ervytime the server retarts (takes approx 10s)

        if examples is None:
            examples = TRAINING_EXAMPLES

        print("Training classifier...")

        for intent, prompts in examples.items():
            embeddings = []

            for prompt in prompts:
                embedding = self.get_embedding(prompt)
                if embedding is not None:
                    embeddings.append(embedding)
                    embeddings.append(embedding)

            if embeddings:
                self.intent_embeddings[intent] = np.array(embeddings)

        self.is_trained = True


    def classify(self, user_prompt: str) -> str:

        if not self.is_trained:
            raise ValueError("Classifier not trained")

        # Get embedding for user prompt
        prompt_embedding = self.get_embedding(user_prompt)
        if prompt_embedding is None:
            return "chat"

        # Compare with all intent embeddings
        intent_scores = {}

        for intent, embeddings in self.intent_embeddings.items():
            similarities = cosine_similarity(
                prompt_embedding.reshape(1, -1),
                embeddings
            )[0]
            intent_scores[intent] = np.max(similarities)

        # Get the intent with highest score
        best_intent = max(intent_scores, key=intent_scores.get)
        confidence = intent_scores[best_intent]

        # If confidence too low, default to chat
        if confidence < self.threshold:
            return "chat"

        return best_intent



classifier = SimpleIntentClassifier()
trained = False



def initialize():

    global trained
    if not trained:
        classifier.train()
        trained = True
initialize()

def get_intent(user_prompt: str) -> str:

    if not trained:
        initialize()

    return classifier.classify(user_prompt)




#functiongemma +qwen3 embedding