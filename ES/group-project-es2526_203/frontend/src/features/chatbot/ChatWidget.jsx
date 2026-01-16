import { useState, useEffect, useRef } from 'react';
import { MessageSquare, Send, X, ThumbsUp, ThumbsDown } from 'lucide-react';
import { API_BASE_URL } from '../../services/api';
import useDarkMode from '../../hooks/useDarkMode';

const ChatWidget = () => {
  const { isDark: isDarkMode } = useDarkMode();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const sessionIdRef = useRef(generateSessionId());
  const messagesEndRef = useRef(null);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Track chatbot opened event
  useEffect(() => {
    if (isOpen) {
      trackEvent('chatbot_opened', { session_id: sessionIdRef.current });
    }
  }, [isOpen]);

  const sendMessage = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = { role: 'user', content: input };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    const startTime = Date.now();

    try {
      const response = await fetch(`${API_BASE_URL}/chatbot/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: input,
          sessionId: sessionIdRef.current
        })
      });

      if (!response.ok) {
        throw new Error('Failed to get response');
      }

      const data = await response.json();
      const latency = Date.now() - startTime;

      setMessages(prev => [...prev, {
        role: 'assistant',
        content: data.response,
        id: data.messageId // Use actual message ID from backend
      }]);

      // Track success
      trackEvent('chatbot_message_sent', {
        session_id: sessionIdRef.current,
        latency_ms: latency,
        tokens_used: data.tokensUsed
      });

    } catch (error) {
      console.error('Chat error:', error);

      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, I encountered an error. Please try again.'
      }]);

      // Track error
      trackEvent('chatbot_error', {
        session_id: sessionIdRef.current,
        error_message: error.message
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleFeedback = async (messageId, feedback) => {
    try {
      await fetch(`${API_BASE_URL}/chatbot/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          messageId: messageId,
          feedback: feedback
        })
      });

      trackEvent('chatbot_feedback', {
        session_id: sessionIdRef.current,
        feedback: feedback
      });
    } catch (error) {
      console.error('Feedback error:', error);
    }
  };

  return (
    <>
      {/* Floating chat button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 bg-black hover:bg-gray-900 text-white rounded-full p-4 shadow-lg transition-colors z-50"
          aria-label="Open chat"
        >
          <MessageSquare size={24} />
        </button>
      )}

      {/* Chat window */}
      {isOpen && (
        <div className="fixed bottom-6 right-6 w-96 h-[600px] bg-card border border-border shadow-2xl rounded-lg flex flex-col z-50">
          {/* Header */}
          <div className="bg-black text-white p-4 rounded-t-lg flex justify-between items-center border-b border-gray-800">
            <div>
              <h3 className="font-semibold">ShopHub Assistant</h3>
              <p className="text-xs text-gray-400">
                Ask me about products!
              </p>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="hover:bg-gray-900 rounded p-1 transition-colors"
              aria-label="Close chat"
            >
              <X size={20} />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-muted/30">
            {messages.length === 0 && (
              <div className="text-center text-muted-foreground mt-8">
                <MessageSquare className="mx-auto mb-2" size={48} />
                <p>Hi! How can I help you today?</p>
                <p className="text-sm mt-2">Ask me about our products!</p>
              </div>
            )}

            {messages.map((msg, idx) => (
              <ChatMessage
                key={idx}
                message={msg}
                isDarkMode={isDarkMode}
                onFeedback={msg.role === 'assistant' && msg.id ? (feedback) => handleFeedback(msg.id, feedback) : null}
              />
            ))}

            {isLoading && <LoadingIndicator isDarkMode={isDarkMode} />}

            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <div className="p-4 border-t border-border bg-card rounded-b-lg">
            <div className="flex gap-2">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
                placeholder="Ask me about products..."
                className="flex-1 border border-input bg-background text-foreground rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-ring placeholder:text-muted-foreground"
                disabled={isLoading}
              />
              <button
                onClick={sendMessage}
                disabled={!input.trim() || isLoading}
                className={`${
                  isDarkMode ? 'bg-blue-500 hover:bg-blue-600' : 'bg-blue-600 hover:bg-blue-700'
                } text-white rounded px-4 py-2 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors`}
                aria-label="Send message"
              >
                <Send size={20} />
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

// ChatMessage component
const ChatMessage = ({ message, isDarkMode, onFeedback }) => {
  const [feedbackGiven, setFeedbackGiven] = useState(null);

  const handleFeedbackClick = (feedback) => {
    setFeedbackGiven(feedback);
    if (onFeedback) {
      onFeedback(feedback);
    }
  };

  return (
    <div className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] rounded-lg px-4 py-2 ${
          message.role === 'user'
            ? isDarkMode
              ? 'bg-blue-500 text-white'
              : 'bg-blue-600 text-white'
            : isDarkMode
            ? 'bg-gray-700 border border-gray-600 text-gray-100'
            : 'bg-white border border-gray-200 text-gray-800'
        }`}
      >
        <p className="text-sm whitespace-pre-wrap">{message.content}</p>

        {/* Feedback buttons for assistant messages */}
        {onFeedback && !feedbackGiven && (
          <div className={`flex gap-2 mt-2 pt-2 border-t ${
            isDarkMode ? 'border-gray-600' : 'border-gray-200'
          }`}>
            <button
              onClick={() => handleFeedbackClick('helpful')}
              className={`${
                isDarkMode ? 'text-gray-400 hover:text-green-400' : 'text-gray-500 hover:text-green-600'
              } transition-colors`}
              aria-label="Helpful"
            >
              <ThumbsUp size={16} />
            </button>
            <button
              onClick={() => handleFeedbackClick('not_helpful')}
              className={`${
                isDarkMode ? 'text-gray-400 hover:text-red-400' : 'text-gray-500 hover:text-red-600'
              } transition-colors`}
              aria-label="Not helpful"
            >
              <ThumbsDown size={16} />
            </button>
          </div>
        )}

        {feedbackGiven && (
          <div className={`text-xs ${
            isDarkMode ? 'text-gray-400 border-gray-600' : 'text-gray-500 border-gray-200'
          } mt-2 pt-2 border-t`}>
            Thanks for your feedback!
          </div>
        )}
      </div>
    </div>
  );
};

// Loading indicator
const LoadingIndicator = ({ isDarkMode }) => {
  return (
    <div className="flex justify-start">
      <div className={`${
        isDarkMode ? 'bg-gray-700 border-gray-600' : 'bg-white border-gray-200'
      } border rounded-lg px-4 py-3`}>
        <div className="flex space-x-2">
          <div className={`w-2 h-2 ${
            isDarkMode ? 'bg-gray-400' : 'bg-gray-400'
          } rounded-full animate-bounce`} style={{ animationDelay: '0ms' }}></div>
          <div className={`w-2 h-2 ${
            isDarkMode ? 'bg-gray-400' : 'bg-gray-400'
          } rounded-full animate-bounce`} style={{ animationDelay: '150ms' }}></div>
          <div className={`w-2 h-2 ${
            isDarkMode ? 'bg-gray-400' : 'bg-gray-400'
          } rounded-full animate-bounce`} style={{ animationDelay: '300ms' }}></div>
        </div>
      </div>
    </div>
  );
};

// Utility functions
function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
}

function trackEvent(eventName, properties = {}) {
  // Send to analytics endpoint
  fetch(`${API_BASE_URL}/analytics/track`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      event: eventName,
      timestamp: new Date().toISOString(),
      properties: {
        ...properties,
        user_agent: navigator.userAgent,
        page: window.location.pathname
      }
    })
  }).catch(err => console.error('Analytics error:', err));
}

export default ChatWidget;
