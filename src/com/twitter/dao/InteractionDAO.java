package com.twitter.dao;

import com.twitter.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractionDAO {

    public void followUser(int followerId, String usernameToFollow) {
        String sql = "INSERT INTO follows (follower_id, following_id) " +
                "VALUES (?, (SELECT user_id FROM users WHERE username = ?))";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, followerId);
            pstmt.setString(2, usernameToFollow);
            int rows = pstmt.executeUpdate();
            if (rows > 0) System.out.println("Now following " + usernameToFollow);

        } catch (SQLException e) {
            System.out.println("Could not follow user (User might not exist or already followed).");
        }
    }

    public void sendDM(int senderId, String receiverUsername, String content) {
        String sql = "INSERT INTO direct_messages (sender_id, receiver_id, content) " +
                "VALUES (?, (SELECT user_id FROM users WHERE username = ?), ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, senderId);
            pstmt.setString(2, receiverUsername);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
            System.out.println("DM Sent!");

        } catch (SQLException e) {
            System.out.println("Failed to send DM.");
        }
    }

    public void toggleLikePost(int userId, int postId) {
        String checkSql = "SELECT 1 FROM post_likes WHERE user_id = ? AND post_id = ?";
        String insertSql = "INSERT INTO post_likes (user_id, post_id) VALUES (?, ?)";
        String deleteSql = "DELETE FROM post_likes WHERE user_id = ? AND post_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, postId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                PreparedStatement delStmt = conn.prepareStatement(deleteSql);
                delStmt.setInt(1, userId);
                delStmt.setInt(2, postId);
                delStmt.executeUpdate();
                System.out.println("Unliked post.");
            } else {
                PreparedStatement inStmt = conn.prepareStatement(insertSql);
                inStmt.setInt(1, userId);
                inStmt.setInt(2, postId);
                inStmt.executeUpdate();
                System.out.println("Liked post!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addComment(int userId, int postId, String content, Integer parentId) {
        String sql = "INSERT INTO comments (user_id, post_id, content, parent_comment_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, postId);
            pstmt.setString(3, content);
            if (parentId == 0) pstmt.setNull(4, Types.INTEGER); // Top level
            else pstmt.setInt(4, parentId);

            pstmt.executeUpdate();
            System.out.println("Comment added.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void viewComments(int postId) {
        String sql = "SELECT c.comment_id, c.parent_comment_id, u.username, c.content " +
                "FROM comments c JOIN users u ON c.user_id = u.user_id " +
                "WHERE c.post_id = ? ORDER BY c.created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, postId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Comments ---");
            while (rs.next()) {
                int id = rs.getInt("comment_id");
                int parentId = rs.getInt("parent_comment_id");
                String user = rs.getString("username");
                String text = rs.getString("content");

                if (parentId == 0) {
                    System.out.printf("[%d] @%s: %s\n", id, user, text);
                } else {
                    System.out.printf("    L__ [%d] Reply to [%d] @%s: %s\n", id, parentId, user, text);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFollowers(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT u.username FROM follows f " +
                "JOIN users u ON f.follower_id = u.user_id " +
                "WHERE f.following_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getFollowing(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT u.username FROM follows f " +
                "JOIN users u ON f.following_id = u.user_id " +
                "WHERE f.follower_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static class DirectMessage {
        public String sender;
        public String content;
        public String date;

        public DirectMessage(String sender, String content, String date) {
            this.sender = sender;
            this.content = content;
            this.date = date;
        }
    }

    public List<DirectMessage> getInbox(int userId) {
        List<DirectMessage> msgs = new ArrayList<>();
        String sql = "SELECT u.username, m.content, m.sent_at " +
                "FROM direct_messages m " +
                "JOIN users u ON m.sender_id = u.user_id " +
                "WHERE m.receiver_id = ? " +
                "ORDER BY m.sent_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                msgs.add(new DirectMessage(
                        rs.getString("username"),
                        rs.getString("content"),
                        rs.getTimestamp("sent_at").toString()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return msgs;
    }

    public List<String> getCommentsList(int postId) {
        List<String> comments = new ArrayList<>();
        String sql = "SELECT c.comment_id, c.parent_comment_id, u.username, c.content " +
                "FROM comments c JOIN users u ON c.user_id = u.user_id " +
                "WHERE c.post_id = ? ORDER BY c.created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, postId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("comment_id");
                int parentId = rs.getInt("parent_comment_id");
                String user = rs.getString("username");
                String text = rs.getString("content");

                if (parentId == 0) {
                    comments.add(String.format("<html><b>@%s</b> [ID:%d]: %s</html>", user, id, text));
                } else {
                    comments.add(String.format("<html>&nbsp;&nbsp;&nbsp;&nbsp;&#8627; <b>@%s</b> [ID:%d]: %s</html>", user, id, text));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    public static class CommentNode {
        public int commentId;
        public int parentId;
        public String username;
        public String content;
        public List<CommentNode> replies;

        public CommentNode(int id, int pid, String user, String text) {
            this.commentId = id;
            this.parentId = pid;
            this.username = user;
            this.content = text;
            this.replies = new ArrayList<>();
        }
    }

    public List<CommentNode> getCommentTree(int postId) {
        List<CommentNode> rootNodes = new ArrayList<>();
        Map<Integer, CommentNode> lookup = new HashMap<>();

        String sql = "SELECT c.comment_id, c.parent_comment_id, u.username, c.content " +
                "FROM comments c JOIN users u ON c.user_id = u.user_id " +
                "WHERE c.post_id = ? ORDER BY c.created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, postId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("comment_id");
                int pid = rs.getInt("parent_comment_id");
                String user = rs.getString("username");
                String content = rs.getString("content");

                CommentNode node = new CommentNode(id, pid, user, content);
                lookup.put(id, node);
            }

            for (CommentNode node : lookup.values()) {
                if (node.parentId == 0) {
                    rootNodes.add(node);
                } else {
                    CommentNode parent = lookup.get(node.parentId);
                    if (parent != null) {
                        parent.replies.add(node);
                    } else {
                        rootNodes.add(node);
                    }
                }
            }

        } catch (SQLException e) { e.printStackTrace(); }
        return rootNodes;
    }
}