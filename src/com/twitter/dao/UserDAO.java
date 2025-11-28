package com.twitter.dao;

import com.twitter.model.User;
import com.twitter.util.DBConnection;

import java.sql.*;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;


public class UserDAO {
    public boolean registerUser(String username, String email, String password, String recoveryEmail, String country) {
        String sql = "INSERT INTO users (username, email, password_hash, recovery_email, country_code) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, recoveryEmail);
            pstmt.setString(5, country);

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Error: Username or Email already exists.");
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User loginUser(String username, String password) {
        String sql = "SELECT user_id, username, email FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void searchUsers(String keyword) {
        String sql = "SELECT username, display_name FROM users WHERE username LIKE ? OR display_name LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");

            ResultSet rs = pstmt.executeQuery();
            System.out.println("\n--- Search Results (Users) ---");
            while (rs.next()) {
                System.out.println("User: @" + rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void checkMentions(int userId) {
        String sql = "SELECT m.created_at, u.username as author, " +
                "COALESCE(p.content, c.content) as content, " +
                "CASE WHEN m.post_id IS NOT NULL THEN 'Post' ELSE 'Comment' END as type " +
                "FROM mentions m " +
                "LEFT JOIN posts p ON m.post_id = p.post_id " +
                "LEFT JOIN comments c ON m.comment_id = c.comment_id " +
                "LEFT JOIN users u ON (p.user_id = u.user_id OR c.user_id = u.user_id) " +
                "WHERE m.mentioned_user_id = ? " +
                "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Your Mentions ---");
            while (rs.next()) {
                System.out.printf("[%s] You were mentioned in a %s by @%s: \"%s\"\n",
                        rs.getTimestamp("created_at"),
                        rs.getString("type"),
                        rs.getString("author"),
                        rs.getString("content"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String generateRecoveryToken(String email) {
        String token = UUID.randomUUID().toString();
        String sql = "INSERT INTO password_tokens (user_id, token_hash, expires_at) " +
                "VALUES ((SELECT user_id FROM users WHERE email = ?), ?, DATE_ADD(NOW(), INTERVAL 1 HOUR))";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, token);
            int rows = pstmt.executeUpdate();

            if (rows > 0) return token;

        } catch (SQLException e) {
            System.out.println("Email not found.");
        }
        return null;
    }

    public boolean resetPassword(String token, String newPassword) {
        String validateSql = "SELECT user_id FROM password_tokens WHERE token_hash = ? AND expires_at > NOW()";
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        String deleteTokenSql = "DELETE FROM password_tokens WHERE token_hash = ?";

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement vStmt = conn.prepareStatement(validateSql);
            vStmt.setString(1, token);
            ResultSet rs = vStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");

                PreparedStatement uStmt = conn.prepareStatement(updateSql);
                uStmt.setString(1, newPassword);
                uStmt.setInt(2, userId);
                uStmt.executeUpdate();

                PreparedStatement dStmt = conn.prepareStatement(deleteTokenSql);
                dStmt.setString(1, token);
                dStmt.executeUpdate();

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getMentionsList(int userId) {
        List<String> notifications = new ArrayList<>();

        String sql = "SELECT m.created_at, u.username as author, " +
                "COALESCE(p.content, c.content) as content, " +
                "CASE WHEN m.post_id IS NOT NULL THEN 'Post' ELSE 'Comment' END as type " +
                "FROM mentions m " +
                "LEFT JOIN posts p ON m.post_id = p.post_id " +
                "LEFT JOIN comments c ON m.comment_id = c.comment_id " +
                "LEFT JOIN users u ON (p.user_id = u.user_id OR c.user_id = u.user_id) " +
                "WHERE m.mentioned_user_id = ? " +
                "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String author = rs.getString("author");
                String type = rs.getString("type");
                String content = rs.getString("content");
                String date = rs.getString("created_at");

                String html = String.format(
                        "<html><b>@%s</b> mentioned you in a <b>%s</b> <font color='gray' size='2'>(%s)</font>:<br><i>\"%s\"</i><br><hr></html>",
                        author, type, date, content
                );
                notifications.add(html);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }
}