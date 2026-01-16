CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(user_id),
    order_id INTEGER REFERENCES orders(order_id),
    title VARCHAR(255) NOT NULL,
    message TEXT,
    type VARCHAR(50) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

CREATE TABLE IF NOT EXISTS landing_pages (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    is_published BOOLEAN DEFAULT FALSE,
    is_banner BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS banners (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(2048),
    metadata JSONB,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    is_published BOOLEAN DEFAULT FALSE,
    priority INTEGER DEFAULT 0,
    landing_page_id INTEGER REFERENCES landing_pages(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_banners_priority ON banners(priority DESC);
CREATE INDEX IF NOT EXISTS idx_banners_start_at ON banners(start_at);
CREATE INDEX IF NOT EXISTS idx_banners_end_at ON banners(end_at);

CREATE INDEX IF NOT EXISTS idx_landing_pages_created_at ON landing_pages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_landing_pages_published_at ON landing_pages(published_at DESC);

-- ============================================
-- Chatbot Tables (Sprint #5)
-- ============================================

-- Conversation storage
CREATE TABLE IF NOT EXISTS chatbot_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255),
    session_id VARCHAR(255) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    total_messages INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_chatbot_conversations_session ON chatbot_conversations(session_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_conversations_user ON chatbot_conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_conversations_started_at ON chatbot_conversations(started_at DESC);

-- Message storage with metadata
CREATE TABLE IF NOT EXISTS chatbot_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES chatbot_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    tokens_used INTEGER,
    latency_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_chatbot_messages_conversation ON chatbot_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_messages_created_at ON chatbot_messages(created_at DESC);

-- Prompt versions (for tracking)
CREATE TABLE IF NOT EXISTS chatbot_prompts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version VARCHAR(50) NOT NULL UNIQUE,
    template TEXT NOT NULL,
    active BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chatbot_prompts_version ON chatbot_prompts(version);
CREATE INDEX IF NOT EXISTS idx_chatbot_prompts_active ON chatbot_prompts(active);

-- Quality metrics
CREATE TABLE IF NOT EXISTS chatbot_quality_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID REFERENCES chatbot_messages(id) ON DELETE CASCADE,
    relevance_score FLOAT,
    contains_product_info BOOLEAN,
    requires_fallback BOOLEAN,
    user_feedback VARCHAR(20) CHECK (user_feedback IN ('helpful', 'not_helpful', 'inappropriate', NULL)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chatbot_quality_metrics_message ON chatbot_quality_metrics(message_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_quality_metrics_created_at ON chatbot_quality_metrics(created_at DESC);

-- Insert default prompt template
INSERT INTO chatbot_prompts (version, template, active)
VALUES ('v1.0', 'You are a helpful shopping assistant for ShopHub, an e-commerce platform.

CONTEXT:
{context}

CONVERSATION HISTORY:
{history}

USER QUERY:
{query}

INSTRUCTIONS:
1. Answer the user''s question about products professionally and concisely
2. If product information is provided in CONTEXT, use it to give specific recommendations
3. If the query is not about shopping/products, politely redirect to shopping topics
4. Keep responses under 150 words
5. Do not make up product information not provided in CONTEXT
6. If uncertain, say "Let me find that information for you" and suggest browsing categories

RESPONSE:', true)
ON CONFLICT (version) DO NOTHING;
