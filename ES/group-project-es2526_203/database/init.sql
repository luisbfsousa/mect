CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL DEFAULT 'customer',
    is_locked BOOLEAN DEFAULT FALSE,
    is_deactivated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    category_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id INTEGER REFERENCES categories(category_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'categories_name_unique'
    ) THEN
        ALTER TABLE categories ADD CONSTRAINT categories_name_unique UNIQUE (name);
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS products (
    product_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    sku VARCHAR(100) UNIQUE,
    category_id INTEGER REFERENCES categories(category_id),
    stock_quantity INTEGER DEFAULT 0,
    low_stock_threshold INTEGER DEFAULT 10,
    images JSONB,
    specifications JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart (
    cart_id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES users(user_id),
    product_id INTEGER REFERENCES products(product_id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_product UNIQUE(user_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) REFERENCES users(user_id),
    order_status VARCHAR(50) NOT NULL DEFAULT 'pending',
    total_amount DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2),
    shipping_cost DECIMAL(10,2),
    shipping_address JSONB,
    billing_address JSONB,
    tracking_number VARCHAR(100),
    shipping_provider VARCHAR(50),
    estimated_delivery_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id INTEGER REFERENCES products(product_id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS reviews (
    review_id SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    user_id VARCHAR(255) REFERENCES users(user_id) ON DELETE SET NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255) NOT NULL,
    comment TEXT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    verified_purchase BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_product_review UNIQUE(user_id, product_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS reviews_unique_seed_idx ON reviews (product_id, title, user_name);

CREATE TABLE IF NOT EXISTS shipping_billing_info (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    
    shipping_full_name VARCHAR(255),
    shipping_address VARCHAR(500),
    shipping_city VARCHAR(255),
    shipping_postal_code VARCHAR(20),
    shipping_phone VARCHAR(50),
    
    billing_full_name VARCHAR(255),
    billing_address VARCHAR(500),
    billing_city VARCHAR(255),
    billing_postal_code VARCHAR(20),
    billing_phone VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    admin_user_id VARCHAR(255) NOT NULL REFERENCES users(user_id),
    target_user_id VARCHAR(255) NOT NULL REFERENCES users(user_id),
    action VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_admin_user_id ON audit_logs(admin_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target_user_id ON audit_logs(target_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);

CREATE TABLE blog_posts (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    image_url VARCHAR(500),
    theme VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    markdown_content TEXT NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS start_date TIMESTAMP;
ALTER TABLE landing_pages ADD COLUMN IF NOT EXISTS end_date TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_cart_user_id ON cart(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_product_id ON cart(product_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(order_status);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(rating);
CREATE INDEX IF NOT EXISTS idx_shipping_billing_user_id ON shipping_billing_info(user_id);
CREATE INDEX idx_blog_posts_status ON blog_posts(status);
CREATE INDEX idx_blog_posts_author ON blog_posts(author_id);
CREATE INDEX IF NOT EXISTS idx_landing_pages_created_at ON landing_pages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_landing_pages_published_at ON landing_pages(published_at DESC);

-- Chatbot tables
CREATE TABLE IF NOT EXISTS chatbot_conversations (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255),
    session_id VARCHAR(255) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    total_messages INTEGER DEFAULT 0
);

-- Ensure only one active conversation per session_id
CREATE UNIQUE INDEX IF NOT EXISTS uq_chatbot_active_session
    ON chatbot_conversations(session_id)
    WHERE ended_at IS NULL;

CREATE TABLE IF NOT EXISTS chatbot_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES chatbot_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    tokens_used INTEGER,
    latency_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_chatbot_messages_conversation ON chatbot_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_messages_created_at ON chatbot_messages(created_at);

INSERT INTO categories (name, description, parent_id) VALUES
('Electronics', 'Electronic devices and accessories', NULL),
('Clothing', 'Fashion and apparel', NULL),
('Home & Garden', 'Home improvement and garden supplies', NULL),
('Books', 'Books and reading materials', NULL),
('Sports', 'Sports equipment and accessories', NULL),
('Health', 'Health and wellness products', NULL),
('Jewelry', 'Jewelry and accessories', NULL),
('Science', 'Scientific equipment and educational materials', NULL)
ON CONFLICT (name) DO NOTHING;

INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('4K Smart TV', '55-inch 4K Ultra HD Smart TV with HDR', 599.99, 'ELEC-TV-001', 1, 15,
 '["https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=400"]'::jsonb,
 '{"screen_size": "55 inches", "resolution": "4K UHD", "smart_features": true, "hdr": true}'::jsonb),

('Bluetooth Speaker', 'Portable waterproof Bluetooth speaker', 79.99, 'ELEC-BS-001', 1, 60,
 '["https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=400"]'::jsonb,
 '{"battery": "12 hours", "waterproof": "IPX7", "connectivity": "Bluetooth 5.0"}'::jsonb),

('Wireless Mouse', 'Ergonomic wireless mouse with precision tracking', 29.99, 'ELEC-WM-001', 1, 120,
 '["https://images.unsplash.com/photo-1527814050087-3793815479db?w=400"]'::jsonb,
 '{"dpi": "3200", "battery": "18 months", "ergonomic": true}'::jsonb);

INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Denim Jacket', 'Classic denim jacket with vintage wash', 79.99, 'CLO-DJ-001', 2, 45,
 '["https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400"]'::jsonb,
 '{"material": "100% cotton denim", "sizes": "XS-XXL", "style": "classic"}'::jsonb),

('Athletic Leggings', 'High-waisted workout leggings with pockets', 39.99, 'CLO-AL-001', 2, 90,
 '["https://blakelyclothing.com/cdn/shop/files/18050-2BLACK_04LEADD_1500x.jpg?v=1714648401"]'::jsonb,
 '{"material": "moisture-wicking", "compression": "medium", "pockets": true}'::jsonb),

('Cotton T-Shirt', 'Soft premium cotton t-shirt in multiple colors', 24.99, 'CLO-TS-001', 2, 150,
 '["https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400"]'::jsonb,
 '{"material": "100% organic cotton", "fit": "regular", "colors": "10 options"}'::jsonb);


INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Robot Vacuum', 'Smart robot vacuum with app control', 299.99, 'HOME-RV-001', 3, 20,
 '["https://images.unsplash.com/photo-1558317374-067fb5f30001?w=400"]'::jsonb,
 '{"battery": "120 minutes", "mapping": true, "app_control": true, "suction": "2000Pa"}'::jsonb),

('Air Purifier', 'HEPA air purifier for large rooms', 189.99, 'HOME-AP-001', 3, 35,
 '["https://onbit.pt/media/nextgenimages/media/catalog/product/cache/ed7f56f27ae08b4c9b3ea8018cb6ab3c/t/o/top_png.webp"]'::jsonb,
 '{"coverage": "500 sq ft", "filter": "True HEPA", "noise_level": "24dB"}'::jsonb),

('Garden Tool Set', 'Complete 10-piece garden tool set with bag', 59.99, 'HOME-GTS-001', 3, 40,
 '["https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=400"]'::jsonb,
 '{"pieces": 10, "material": "stainless steel", "includes_bag": true}'::jsonb);


INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Hunger Games Collection', 'Bestselling fiction novel by Matt Haig', 89.99, 'BOOK-ML-001', 4, 100,
 '["https://m.media-amazon.com/images/I/71ijKMB7TIL._UF1000,1000_QL80_.jpg"]'::jsonb,
 '{"author": "Suzanne Collins", "pages": 384, "format": "paperback", "genre": "fiction"}'::jsonb),

('Sapiens', 'A brief history of humankind by Yuval Noah Harari', 19.99, 'BOOK-SAP-001', 4, 70,
 '["https://img.wook.pt/images/sapiens-yuval-noah-harari/MXwxNjEwNDQyM3wxMTY0OTc2M3wxNzAzMDIyOTMzMDAw/500x"]'::jsonb,
 '{"author": "Yuval Noah Harari", "pages": 464, "format": "paperback", "genre": "non-fiction"}'::jsonb),

('Cookbook Collection', 'Essential cooking techniques and recipes', 29.99, 'BOOK-CC-001', 4, 50,
 '["https://junipercustom.com/cdn/shop/files/PHCB3-phaidon-cookbook-set-2-1200_2048x.jpg?v=1690300492"]'::jsonb,
 '{"pages": 400, "recipes": "200+", "format": "hardcover", "illustrated": true}'::jsonb);

INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Dumbbell Set', 'Adjustable dumbbell set 5-50 lbs', 299.99, 'SPO-DB-001', 5, 25,
 '["https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=400"]'::jsonb,
 '{"weight_range": "5-50 lbs", "adjustable": true, "increments": "5 lbs"}'::jsonb),

('Tennis Racket', 'Professional tennis racket with carbon frame', 159.99, 'SPO-TR-001', 5, 40,
 '["https://www.lta.org.uk/490994/siteassets/play/adult/image/ball-racket.jpg?w=1200"]'::jsonb,
 '{"material": "carbon fiber", "weight": "300g", "grip_size": "4 3/8"}'::jsonb),

('Basketball', 'Official size and weight basketball', 34.99, 'SPO-BB-001', 5, 80,
 '["https://burnedsports.com/cdn/shop/files/wilson-buiten-basketbal-outdoor-maat-7-6-5-heren-kinderen-dames-authentic-rubber-nba-burned-sportsIMG_8168.webp?v=1758789609"]'::jsonb,
 '{"size": "official", "material": "composite leather", "indoor_outdoor": true}'::jsonb);

INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Digital Thermometer', 'Fast-reading digital thermometer', 14.99, 'HEAL-DT-001', 6, 120,
 '["https://i5.walmartimages.com/seo/MABIS-Digital-Thermometer-for-Adults-Oral-Thermometer-for-Adults-Children-and-Babies-Underarm-Temperature-Thermometer-60-Seconds-Readings_f0347a41-7bec-4506-b613-0f69546a2b50.9e915f8d999f1a726a0f8fa20549d14c.jpeg"]'::jsonb,
 '{"reading_time": "10 seconds", "memory": "last 10 readings", "fever_alarm": true}'::jsonb),

('Vitamin D Supplement', 'High-potency Vitamin D3 capsules', 19.99, 'HEAL-VD-001', 6, 200,
 '["https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400"]'::jsonb,
 '{"dosage": "5000 IU", "count": 120, "form": "softgels", "non_gmo": true}'::jsonb),

('Blood Pressure Monitor', 'Automatic upper arm blood pressure monitor', 49.99, 'HEAL-BP-001', 6, 45,
 '["https://images.unsplash.com/photo-1615486511484-92e172cc4fe0?w=400"]'::jsonb,
 '{"memory": "60 readings", "cuff_size": "adjustable", "irregular_heartbeat_detection": true}'::jsonb);

INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Silver Necklace', 'Sterling silver chain necklace', 89.99, 'JEW-SN-001', 7, 55,
 '["https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=400"]'::jsonb,
 '{"material": "925 sterling silver", "length": "18 inches", "clasp": "lobster"}'::jsonb),

('Diamond Stud Earrings', 'Classic diamond stud earrings in 14K gold', 299.99, 'JEW-DE-001', 7, 30,
 '["https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=400"]'::jsonb,
 '{"material": "14K white gold", "diamond_weight": "0.5 carat", "cut": "round brilliant"}'::jsonb),

('Leather Watch', 'Minimalist watch with genuine leather strap', 129.99, 'JEW-LW-001', 7, 40,
 '["https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=400"]'::jsonb,
 '{"case_material": "stainless steel", "strap": "genuine leather", "water_resistant": "50m"}'::jsonb);

INSERT INTO products (name, description, price, sku, category_id, stock_quantity, images, specifications) VALUES
('Microscope Kit', 'Student microscope with slides and accessories', 89.99, 'SCI-MK-001', 8, 35,
 '["https://www.murphyandson.co.uk/wp-content/uploads/2020/09/MICROKIT.png"]'::jsonb,
 '{"magnification": "40x-1000x", "led_illumination": true, "includes": "slides and specimens"}'::jsonb),

('Chemistry Set', 'Educational chemistry experiment kit', 59.99, 'SCI-CS-001', 8, 50,
 '["https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=400"]'::jsonb,
 '{"experiments": "50+", "age_range": "10+", "safety_equipment": true}'::jsonb),

('Telescope', 'Refractor telescope for stargazing', 199.99, 'SCI-TEL-001', 8, 20,
 '["https://cdn.mos.cms.futurecdn.net/VtkGQvZhCU3M7qtRnriPNZ-1200-80.jpg"]'::jsonb,
 '{"aperture": "70mm", "focal_length": "400mm", "tripod": true, "finder_scope": true}'::jsonb);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cart_updated_at BEFORE UPDATE ON cart
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reviews_updated_at BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shipping_billing_updated_at BEFORE UPDATE ON shipping_billing_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_blog_posts_timestamp 
BEFORE UPDATE ON blog_posts
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_landing_pages_timestamp 
BEFORE UPDATE ON landing_pages
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

INSERT INTO reviews (product_id, user_id, rating, title, comment, user_name, verified_purchase, created_at) VALUES
(1, NULL, 5, 'Excellent product!', 'I absolutely love this product. It exceeded my expectations in every way. Highly recommend!', 'John Doe', TRUE, CURRENT_TIMESTAMP - INTERVAL '13 days'),
(1, NULL, 4, 'Great value for money', 'Good quality product. Does exactly what it says. Only minor issue is the packaging could be better.', 'Jane Smith', TRUE, CURRENT_TIMESTAMP - INTERVAL '16 days'),
(1, NULL, 5, 'Perfect!', 'Works perfectly. Fast delivery and exactly as described. Will buy again!', 'Mike Johnson', TRUE, CURRENT_TIMESTAMP - INTERVAL '19 days'),
(2, NULL, 5, 'Best smartwatch I have owned', 'The battery life is amazing and the fitness tracking is very accurate. Love the AMOLED display!', 'Sarah Williams', TRUE, CURRENT_TIMESTAMP - INTERVAL '10 days'),
(2, NULL, 4, 'Great features', 'Solid smartwatch with good features. The app could be more intuitive but overall very satisfied.', 'Tom Brown', TRUE, CURRENT_TIMESTAMP - INTERVAL '12 days'),
(3, NULL, 5, 'Super comfortable!', 'These running shoes are incredibly comfortable. Perfect for long runs. Highly recommend for runners.', 'Emily Davis', TRUE, CURRENT_TIMESTAMP - INTERVAL '8 days'),
(3, NULL, 4, 'Good shoes', 'Nice shoes, good cushioning. Fit is true to size. Would buy again.', 'Chris Wilson', TRUE, CURRENT_TIMESTAMP - INTERVAL '15 days'),
(4, NULL, 5, 'Makes perfect coffee every time', 'This coffee maker is fantastic. Easy to use and clean. The programmable feature is very convenient.', 'Lisa Anderson', TRUE, CURRENT_TIMESTAMP - INTERVAL '20 days'),
(5, NULL, 5, 'Best yoga mat ever!', 'Non-slip surface works great. Good thickness and very durable. Perfect for my daily yoga practice.', 'Jessica Taylor', TRUE, CURRENT_TIMESTAMP - INTERVAL '5 days')
ON CONFLICT (product_id, title, user_name) DO NOTHING;
