USE TwitterDB;

-- 1. CLEANUP
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE mentions;
TRUNCATE TABLE trending_posts;
TRUNCATE TABLE user_pins;
TRUNCATE TABLE password_tokens;
TRUNCATE TABLE direct_messages;
TRUNCATE TABLE comment_likes;
TRUNCATE TABLE post_likes;
TRUNCATE TABLE follows;
TRUNCATE TABLE comments;
TRUNCATE TABLE posts;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 2. INSERT USERS (ID: 1 to 5)
-- =============================================
INSERT INTO users (username, email, password_hash, display_name, country_code) VALUES 
('alice',   'alice@example.com',   'password123', 'Alice Wonderland', 'US'),
('bob',     'bob@example.com',     'password123', 'Bob Builder',      'UK'),
('charlie', 'charlie@example.com', 'password123', 'Charlie Brown',    'CA'),
('dave',    'dave@example.com',    'password123', 'Dave Diver',       'US'),
('eve',     'eve@example.com',     'password123', 'Eve Explorer',     'FR');

-- =============================================
-- 3. INSERT FOLLOWS (Graph Connections)
-- =============================================
-- Alice follows Bob and Charlie (Alice's timeline will show their posts)
INSERT INTO follows (follower_id, following_id) VALUES (1, 2), (1, 3);
-- Bob follows Alice and Dave
INSERT INTO follows (follower_id, following_id) VALUES (2, 1), (2, 4);
-- Charlie follows Eve
INSERT INTO follows (follower_id, following_id) VALUES (3, 5);
-- Dave follows everyone (to see a full timeline)
INSERT INTO follows (follower_id, following_id) VALUES (4, 1), (4, 2), (4, 3), (4, 5);

-- =============================================
-- 4. INSERT POSTS (Simulating Timeline)
-- =============================================
-- Alice's Posts
INSERT INTO posts (user_id, content, view_count, created_at) VALUES 
(1, 'Hello world! This is my first post on this new platform.', 150, NOW() - INTERVAL 5 HOUR),
(1, 'Enjoying a nice cup of coffee this morning.', 45, NOW() - INTERVAL 3 HOUR);

-- Bob's Posts (Includes a Mention of Alice)
INSERT INTO posts (user_id, content, view_count, created_at) VALUES 
(2, 'Working on a new Java project. It is quite challenging.', 200, NOW() - INTERVAL 4 HOUR),
(2, 'Hey @alice, are you coming to the meeting later?', 300, NOW() - INTERVAL 2 HOUR);

-- Charlie's Posts (Popular content for Trending)
INSERT INTO posts (user_id, content, view_count, created_at) VALUES 
(3, 'Just saw the best movie of the year. Highly recommend it!', 5000, NOW() - INTERVAL 1 DAY),
(3, 'The weather is amazing today.', 10, NOW() - INTERVAL 10 MINUTE);

-- Eve's Post
INSERT INTO posts (user_id, content, view_count, created_at) VALUES 
(5, 'Travel photography is my passion. Here is a thought from Paris.', 1200, NOW() - INTERVAL 30 MINUTE);

-- =============================================
-- 5. INSERT MENTIONS (Sync with text content)
-- =============================================
-- Bob mentioned Alice in Post ID 4
INSERT INTO mentions (mentioned_user_id, post_id) VALUES (1, 4);

-- =============================================
-- 6. INSERT COMMENTS (Recursive Hierarchy)
-- =============================================
-- Thread on Bob's Project Post (Post ID 3)
-- 1. Alice comments on Bob's post
INSERT INTO comments (user_id, post_id, content, parent_comment_id) VALUES (1, 3, 'Good luck Bob! You can do it.', NULL); 
-- Assume ID is 1

-- 2. Bob replies to Alice (Level 1 reply)
INSERT INTO comments (user_id, post_id, content, parent_comment_id) VALUES (2, 3, 'Thanks Alice, I appreciate the support.', 1); 
-- Assume ID is 2

-- 3. Dave replies to Bob's reply (Level 2 reply)
INSERT INTO comments (user_id, post_id, content, parent_comment_id) VALUES (4, 3, 'I can help if you get stuck.', 2); 
-- Assume ID is 3

-- Separate comment on Charlie's Movie Post (Post ID 5)
INSERT INTO comments (user_id, post_id, content, parent_comment_id) VALUES (5, 5, 'What was the movie title?', NULL);

-- =============================================
-- 7. INSERT LIKES (Populate Counts)
-- =============================================
-- Everyone likes Charlie's popular post (ID 5)
INSERT INTO post_likes (user_id, post_id) VALUES (1, 5), (2, 5), (4, 5), (5, 5);

-- Alice and Dave like Bob's project post (ID 3)
INSERT INTO post_likes (user_id, post_id) VALUES (1, 3), (4, 3);

-- Alice likes Bob's reply comment (Comment ID 2)
INSERT INTO comment_likes (user_id, comment_id) VALUES (1, 2);

-- =============================================
-- 8. INSERT DIRECT MESSAGES (Inbox)
-- =============================================
-- Conversation: Bob -> Alice
INSERT INTO direct_messages (sender_id, receiver_id, content, sent_at) VALUES 
(2, 1, 'Hi Alice, did you see my post?', NOW() - INTERVAL 1 HOUR),
(2, 1, 'I need that file by tomorrow.', NOW() - INTERVAL 30 MINUTE);

-- Conversation: Alice -> Bob
INSERT INTO direct_messages (sender_id, receiver_id, content, sent_at) VALUES 
(1, 2, 'Yes, I saw it. Sending the file now.', NOW() - INTERVAL 15 MINUTE);

-- =============================================
-- 9. INSERT MISC DATA (Pins, Tokens, Trending)
-- =============================================
-- Alice pins her first post
INSERT INTO user_pins (user_id, post_id) VALUES (1, 1);

-- Simulate a password reset token for Eve
INSERT INTO password_tokens (user_id, token_hash, expires_at) VALUES 
(5, 'abc-123-token', DATE_ADD(NOW(), INTERVAL 1 HOUR));

-- Pre-calculate Trending (Normally done by Java, but inserting for immediate demo)
-- Charlie's Post #5 has 4 likes + 5000 views = High Score
INSERT INTO trending_posts (post_id, score) VALUES (5, 5040.00);
-- Eve's Post #7 has 0 likes + 1200 views
INSERT INTO trending_posts (post_id, score) VALUES (7, 1200.00);

COMMIT;