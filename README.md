# twitter-jdbc

A fully functional social networking application built from scratch using Java (Swing) and MySQL.

## Key Features

### User Management
*   **Authentication:** Secure Login and Registration.
*   **Account Recovery:** Password reset functionality using temporary tokens.
*   **Profile Management:** Usernames, emails, and country codes.

### Content & Timeline
*   **Live Timeline:** Fetches posts from the user and accounts they follow, sorted chronologically.
*   **Rich Posts:** Supports text content and tracks **View Counts**.
*   **Trending System:** An algorithm calculates "Trending Posts" based on recency, likes, and view counts.

### Social Interactions
*   **Recursive Comments:** Supports infinite nesting of replies (threaded comments).
*   **Likes:** Users can like/unlike posts; updates are handled via atomic database transactions.
*   **Mentions:** Automatic detection of `@username` in posts/comments using Regex, triggering notifications.
*   **Follow System:** Many-to-Many relationship allowing users to build their network.

### Messaging
*   **Direct Messages (DM):** Private inbox for 1-on-1 communication.
*   **Notifications:** Dedicated view for mentions.

## Technology Stack

*   **Language:** Java (JDK 17+)
*   **Database:** MySQL 8.0
*   **Connectivity:** JDBC (MySQL Connector/J)
*   **GUI:** Java Swing (JFrame, JPanel, CardLayout)
*   **Tools:** IntelliJ IDEA, MySQL Workbench

## Database Schema

The project utilizes a **Master Schema** designed to support scalability:

*   **Core:** `users`, `posts`, `follows`
*   **Recursive:** `comments` (Self-referencing Foreign Key)
*   **Features:** `post_likes`, `comment_likes`, `mentions`, `direct_messages`
*   **Security & Analytics:** `password_tokens`, `trending_posts`, `user_pins`

## How to Run

### 1. Database Setup
1.  Open **MySQL Workbench**.
2.  Create a database named `twitter_clone`.
3.  Run the provided `ddl_script.sql` to create tables.
4.  Run `insert_data.sql` to populate the database with dummy users and posts.

### 2. Configure Java Client
1.  Open the project in **IntelliJ IDEA**.
2.  Navigate to `src/com/twitter/util/DBConnection.java`.
3.  Update the `USER` and `PASSWORD` constants with your MySQL credentials.

### 3. Launch Application
1.  Run `TwitterGUI.java` located in `src/com/twitter/app`.
2.  **Login Credentials (Dummy Data):**
    *   **Username:** `alice`
    *   **Password:** `password123`
