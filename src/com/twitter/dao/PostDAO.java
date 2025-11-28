package com.twitter.dao;

import com.twitter.util.DBConnection;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;


public class PostDAO {

    public static class TimelinePost {
        public int postId;
        public String username;
        public String content;
        public int viewCount;
        public int likeCount;
        public boolean isLikedByCurrentUser;

        public TimelinePost(int id, String user, String text, int views, int likes, boolean liked) {
            this.postId = id;
            this.username = user;
            this.content = text;
            this.viewCount = views;
            this.likeCount = likes;
            this.isLikedByCurrentUser = liked;
        }
    }

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public void createPost(int userId, String content) {
        String insertPostSql = "INSERT INTO posts (user_id, content) VALUES (?, ?)";
        String insertMentionSql = "INSERT INTO mentions (mentioned_user_id, post_id) VALUES ((SELECT user_id FROM users WHERE username = ?), ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement postStmt = conn.prepareStatement(insertPostSql, Statement.RETURN_GENERATED_KEYS);
            postStmt.setInt(1, userId);
            postStmt.setString(2, content);
            postStmt.executeUpdate();

            ResultSet rs = postStmt.getGeneratedKeys();
            int postId = 0;
            if (rs.next()) postId = rs.getInt(1);

            Matcher matcher = MENTION_PATTERN.matcher(content);
            PreparedStatement mentionStmt = conn.prepareStatement(insertMentionSql);
            while (matcher.find()) {
                mentionStmt.setString(1, matcher.group(1));
                mentionStmt.setInt(2, postId);
                try {
                    mentionStmt.executeUpdate();
                } catch (SQLException ex) {
                }
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
            }
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
            }
        }
    }

    public List<TimelinePost> getTimelinePosts(int currentUserId) {
        List<TimelinePost> posts = new ArrayList<>();

        String sql = "SELECT p.post_id, u.username, p.content, p.view_count, " +
                "(SELECT COUNT(*) FROM post_likes WHERE post_id = p.post_id) as like_count, " +
                "(SELECT COUNT(*) FROM post_likes WHERE post_id = p.post_id AND user_id = ?) as is_liked " +
                "FROM posts p " +
                "JOIN users u ON p.user_id = u.user_id " +
                "WHERE p.user_id = ? " +
                "OR p.user_id IN (SELECT following_id FROM follows WHERE follower_id = ?) " +
                "ORDER BY p.created_at DESC LIMIT 20";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);
            pstmt.setInt(3, currentUserId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                posts.add(new TimelinePost(
                        rs.getInt("post_id"),
                        rs.getString("username"),
                        rs.getString("content"),
                        rs.getInt("view_count"),
                        rs.getInt("like_count"),
                        rs.getInt("is_liked") > 0
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }


    public void printTimeline(int currentUserId) {
        String sql = "SELECT p.post_id, u.username, p.content, p.view_count, p.created_at " +
                "FROM posts p " +
                "JOIN users u ON p.user_id = u.user_id " +
                "LEFT JOIN follows f ON f.following_id = p.user_id " +
                "WHERE p.user_id = ? OR f.follower_id = ? " +
                "ORDER BY p.created_at DESC LIMIT 10";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);

            ResultSet rs = pstmt.executeQuery();
            System.out.println("\n--- Your Timeline ---");
            while (rs.next()) {
                System.out.printf("[%s] @%s: %s (Views: %d)\n",
                        rs.getTimestamp("created_at"),
                        rs.getString("username"),
                        rs.getString("content"),
                        rs.getInt("view_count"));

                incrementViewCount(rs.getInt("post_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void incrementViewCount(int postId) {
        String sql = "UPDATE posts SET view_count = view_count + 1 WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, postId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public void searchPosts(String keyword) {
        String sql = "SELECT u.username, p.content, p.created_at FROM posts p " +
                "JOIN users u ON p.user_id = u.user_id " +
                "WHERE p.content LIKE ? ORDER BY p.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Search Results (Posts) ---");
            while (rs.next()) {
                System.out.printf("@%s: %s\n", rs.getString("username"), rs.getString("content"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void refreshTrending() {
        String sql = "INSERT INTO trending_posts (post_id, score) " +
                "SELECT p.post_id, (COUNT(pl.user_id) * 10 + p.view_count) as score " +
                "FROM posts p " +
                "LEFT JOIN post_likes pl ON p.post_id = pl.post_id " +
                "GROUP BY p.post_id " +
                "ORDER BY score DESC LIMIT 5 " +
                "ON DUPLICATE KEY UPDATE score = VALUES(score)";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM trending_posts");
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void viewTrending() {
        refreshTrending();

        String sql = "SELECT t.score, p.content, u.username FROM trending_posts t " +
                "JOIN posts p ON t.post_id = p.post_id " +
                "JOIN users u ON p.user_id = u.user_id " +
                "ORDER BY t.score DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- TRENDING NOW ---");
            int rank = 1;
            while (rs.next()) {
                System.out.printf("#%d (Score: %.0f) @%s: %s\n",
                        rank++, rs.getDouble("score"), rs.getString("username"), rs.getString("content"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTimelineList(int currentUserId) {
        List<String> tweets = new ArrayList<>();
        String sql = "SELECT u.username, p.content, p.view_count, p.created_at " +
                "FROM posts p " +
                "JOIN users u ON p.user_id = u.user_id " +
                "LEFT JOIN follows f ON f.following_id = p.user_id " +
                "WHERE p.user_id = ? OR f.follower_id = ? " +
                "ORDER BY p.created_at DESC LIMIT 20";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String tweet = String.format("<html><b>@%s</b> <font color='gray'>(%d views)</font><br>%s</html>",
                        rs.getString("username"),
                        rs.getInt("view_count"),
                        rs.getString("content"));
                tweets.add(tweet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tweets;
    }

    public List<String> getTrendingList() {
        refreshTrending();
        List<String> trends = new ArrayList<>();
        String sql = "SELECT t.score, p.content, u.username FROM trending_posts t " +
                "JOIN posts p ON t.post_id = p.post_id " +
                "JOIN users u ON p.user_id = u.user_id " +
                "ORDER BY t.score DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int rank = 1;
            while (rs.next()) {
                trends.add(String.format("<html><b>#%d (Score: %.0f)</b> @%s<br>%s</html>",
                        rank++, rs.getDouble("score"), rs.getString("username"), rs.getString("content")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trends;
    }

}