import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'

/**
 * ELIZA - A tribute to Joseph Weizenbaum's 1966 chatbot
 * "ELIZA is a computer program that parodies a Rogerian psychotherapist"
 * This serves as a delightful easter egg when no real AI backend is configured.
 */
class Eliza {
  private reflections: Record<string, string> = {
    'i am': 'you are', 'i was': 'you were', 'i': 'you',
    'i\'m': 'you\'re', 'i\'d': 'you\'d', 'i\'ve': 'you\'ve',
    'i\'ll': 'you\'ll', 'my': 'your', 'me': 'you',
    'are you': 'am I', 'you\'ve': 'I\'ve', 'you\'ll': 'I\'ll',
    'your': 'my', 'yours': 'mine', 'you': 'I', 'myself': 'yourself'
  }

  private patterns: [RegExp, string[]][] = [
    [/\b(hello|hi|hey|greetings)\b/i, [
      "Hello! How are you feeling today?",
      "Hi there. What's on your mind?",
      "Greetings! Tell me about your documents."
    ]],
    [/\bsorry\b/i, [
      "Please don't apologize.",
      "Apologies are not necessary.",
      "What feelings do you have when you apologize?"
    ]],
    [/\bi remember (.*)/i, [
      "Do you often think of %1?",
      "What else does thinking of %1 bring to mind?",
      "Why do you remember %1 just now?"
    ]],
    [/\bdo you remember (.*)/i, [
      "Did you think I would forget %1?",
      "What about %1 should I remember?",
      "Why do you mention %1?"
    ]],
    [/\bi want (.*)/i, [
      "What would it mean if you got %1?",
      "Why do you want %1?",
      "Suppose you got %1 soon - what then?"
    ]],
    [/\bif (.*)/i, [
      "Do you really think it's likely that %1?",
      "Do you wish that %1?",
      "What do you know about %1?"
    ]],
    [/\bi dreamed (.*)/i, [
      "Really, %1?",
      "Have you ever fantasized about %1 while awake?",
      "Have you dreamed about %1 before?"
    ]],
    [/\b(dream|dreams)\b/i, [
      "What does that dream suggest to you?",
      "Do you dream often?",
      "What persons appear in your dreams?"
    ]],
    [/\bmy mother\b/i, [
      "Tell me more about your mother.",
      "What was your relationship with your mother like?",
      "How does that relate to your feelings today?"
    ]],
    [/\bmy father\b/i, [
      "Tell me more about your father.",
      "How did your father make you feel?",
      "Does your relationship with your father relate to your feelings today?"
    ]],
    [/\b(mother|father|family|parents)\b/i, [
      "Tell me more about your family.",
      "How does your family make you feel?",
      "What comes to mind when you think of your family?"
    ]],
    [/\bi am (.*)/i, [
      "How long have you been %1?",
      "Do you believe it is normal to be %1?",
      "Do you enjoy being %1?"
    ]],
    [/\bi\'?m (.*)/i, [
      "How does being %1 make you feel?",
      "Do you enjoy being %1?",
      "Why do you say you're %1?"
    ]],
    [/\bare you (.*)/i, [
      "Why are you interested in whether I am %1?",
      "Would you prefer if I were not %1?",
      "Perhaps I am %1 in your fantasies."
    ]],
    [/\bwhat (.*)/i, [
      "Why do you ask?",
      "Does that question interest you?",
      "What answer would please you most?"
    ]],
    [/\bhow (.*)/i, [
      "How do you suppose?",
      "What do you think?",
      "What is it you really want to know?"
    ]],
    [/\bbecause (.*)/i, [
      "Is that the real reason?",
      "What other reasons might there be?",
      "Does that reason explain anything else?"
    ]],
    [/\b(sad|unhappy|depressed|angry|mad|frustrated)\b/i, [
      "I'm sorry to hear you're feeling that way.",
      "What do you think is causing these feelings?",
      "Can you tell me more about what's troubling you?"
    ]],
    [/\b(happy|glad|joyful|excited)\b/i, [
      "That's wonderful! What's making you feel this way?",
      "I'm glad to hear that. Tell me more.",
      "What else brings you joy?"
    ]],
    [/\byes\b/i, [
      "You seem quite sure.",
      "I see. Please go on.",
      "I understand."
    ]],
    [/\bno\b/i, [
      "Why not?",
      "Are you sure?",
      "Why do you say no?"
    ]],
    [/\b(document|file|pdf|invoice|tax|receipt)\b/i, [
      "Tell me more about this document.",
      "How does this document make you feel?",
      "What would you like to do with this document?",
      "Documents can hold many memories. What does this one mean to you?"
    ]],
    [/\b(search|find|looking for)\b/i, [
      "What are you hoping to find?",
      "How would finding it make you feel?",
      "Tell me more about what you're searching for."
    ]],
    [/\bcomputer\b/i, [
      "Do computers worry you?",
      "Are you talking about me?",
      "Why do you mention computers?"
    ]],
    [/\byou are (.*)/i, [
      "What makes you think I am %1?",
      "Does it please you to believe I am %1?",
      "Perhaps you would like to be %1?"
    ]],
    [/\bi feel (.*)/i, [
      "Tell me more about these feelings.",
      "Do you often feel %1?",
      "When do you usually feel %1?"
    ]],
    [/\bwhy don\'?t you (.*)/i, [
      "Do you really think I should %1?",
      "Perhaps eventually I will %1.",
      "Do you want me to %1?"
    ]],
    [/\bwhy can\'?t I (.*)/i, [
      "Do you think you should be able to %1?",
      "What would it mean if you could %1?",
      "Have you tried?"
    ]],
    [/\bI can\'?t (.*)/i, [
      "How do you know you can't %1?",
      "Have you tried?",
      "Perhaps you could %1 now."
    ]],
    [/\balways\b/i, [
      "Can you think of a specific example?",
      "Really, always?",
      "When?"
    ]],
    [/\bthink\b/i, [
      "What do you think?",
      "Do you really think so?",
      "But you're not sure?"
    ]]
  ]

  private defaults = [
    "Please tell me more.",
    "Let's change focus a bit... Tell me about your documents.",
    "Can you elaborate on that?",
    "Very interesting. Please continue.",
    "I see. And what does that tell you?",
    "How does that make you feel?",
    "I'm not sure I understand. Can you explain?",
    "Go on, I'm listening.",
    "What else comes to mind?",
    "That's quite thought-provoking."
  ]

  private reflect(text: string): string {
    const words = text.toLowerCase().split(' ')
    return words.map(word => this.reflections[word] || word).join(' ')
  }

  public respond(input: string): string {
    const text = input.trim()
    
    for (const [pattern, responses] of this.patterns) {
      const match = text.match(pattern)
      if (match) {
        let response = responses[Math.floor(Math.random() * responses.length)]
        if (match[1]) {
          const reflected = this.reflect(match[1])
          response = response.replace('%1', reflected)
        }
        return response
      }
    }
    
    return this.defaults[Math.floor(Math.random() * this.defaults.length)]
  }
}

const eliza = new Eliza()

export const AiService = {
  async ask(question: string) {
    try {
      // Try real AI endpoint first
      const { data, error } = await client.POST("/api/ai/ask" as any, { 
          body: { question } 
      })
      
      if (error) throw new Error(getErrorMessage(error))
      return data as { answer: string; sources?: string[] }
    } catch {
      // Easter egg: Fall back to ELIZA when no AI backend
      await new Promise(r => setTimeout(r, 500 + Math.random() * 1000)) // Simulate thinking
      return { 
        answer: eliza.respond(question),
        sources: ['ELIZA (Weizenbaum, 1966)']
      }
    }
  }
}