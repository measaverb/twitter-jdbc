USE TwitterDB;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS mentions;
DROP TABLE IF EXISTS trending_posts;
DROP TABLE IF EXISTS user_pins;
DROP TABLE IF EXISTS password_tokens;
DROP TABLE IF EXISTS direct_messages;
DROP TABLE IF EXISTS comment_likes;
DROP TABLE IF EXISTS post_likes;
DROP TABLE IF EXISTS follows;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- Table: USERS
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    recovery_email VARCHAR(100),
    country_code VARCHAR(5),
    display_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Table: POSTS
CREATE TABLE posts (
    post_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_post_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
);

-- Table: COMMENTS
CREATE TABLE comments (
    comment_id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    parent_comment_id INT DEFAULT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_comment_post 
        FOREIGN KEY (post_id) REFERENCES posts(post_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent 
        FOREIGN KEY (parent_comment_id) REFERENCES comments(comment_id) 
        ON DELETE CASCADE
);

-- Table: FOLLOWS
CREATE TABLE follows (
    follower_id INT NOT NULL,
    following_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (follower_id, following_id),
    
    CONSTRAINT fk_follow_follower 
        FOREIGN KEY (follower_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_follow_following 
        FOREIGN KEY (following_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
);

-- Table: POST_LIKES
CREATE TABLE post_likes (
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, post_id),
    
    CONSTRAINT fk_like_post_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_like_post_post 
        FOREIGN KEY (post_id) REFERENCES posts(post_id) 
        ON DELETE CASCADE
);

-- Table: COMMENT_LIKES
CREATE TABLE comment_likes (
    user_id INT NOT NULL,
    comment_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, comment_id),
    
    CONSTRAINT fk_like_comment_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_like_comment_comment 
        FOREIGN KEY (comment_id) REFERENCES comments(comment_id) 
        ON DELETE CASCADE
);

-- Table: DIRECT_MESSAGES
CREATE TABLE direct_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_dm_sender 
        FOREIGN KEY (sender_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_dm_receiver 
        FOREIGN KEY (receiver_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
);

-- Table: MENTIONS
CREATE TABLE mentions (
    mention_id INT AUTO_INCREMENT PRIMARY KEY,
    mentioned_user_id INT NOT NULL,
    post_id INT DEFAULT NULL,
    comment_id INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mention_user 
        FOREIGN KEY (mentioned_user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_mention_post 
        FOREIGN KEY (post_id) REFERENCES posts(post_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_mention_comment 
        FOREIGN KEY (comment_id) REFERENCES comments(comment_id) 
        ON DELETE CASCADE,
        
    CONSTRAINT chk_mention_source CHECK (post_id IS NOT NULL OR comment_id IS NOT NULL)
);

-- Table: USER_PINS
CREATE TABLE user_pins (
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    pinned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, post_id),
    
    CONSTRAINT fk_pin_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_pin_post 
        FOREIGN KEY (post_id) REFERENCES posts(post_id) 
        ON DELETE CASCADE
);

-- Table: PASSWORD_TOKENS
CREATE TABLE password_tokens (
    token_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_token_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id) 
        ON DELETE CASCADE
);

-- Table: TRENDING_POSTS
CREATE TABLE trending_posts (
    post_id INT PRIMARY KEY,
    score DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_trending_post 
        FOREIGN KEY (post_id) REFERENCES posts(post_id) 
        ON DELETE CASCADE
);