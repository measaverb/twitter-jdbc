package com.twitter.app;

import com.twitter.dao.InteractionDAO;
import com.twitter.dao.PostDAO;
import com.twitter.dao.UserDAO;
import com.twitter.model.User;

import java.util.Scanner;

public class Main {
    private static User currentUser = null;
    private static final Scanner scanner = new Scanner(System.in);

    private static final UserDAO userDAO = new UserDAO();
    private static final PostDAO postDAO = new PostDAO();
    private static final InteractionDAO interactionDAO = new InteractionDAO();

    public static void main(String[] args) {
        System.out.println("=== TWITTER DB CLONE PROJECT ===");
        while (true) {
            if (currentUser == null) showGuestMenu();
            else showUserMenu();
        }
    }

    private static void showGuestMenu() {
        System.out.println("\n1. Login\n2. Register\n3. Forgot Password\n4. Exit");
        System.out.print("Select: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                System.out.print("Username: ");
                String user = scanner.nextLine();
                System.out.print("Password: ");
                String pass = scanner.nextLine();
                currentUser = userDAO.loginUser(user, pass);
                if (currentUser != null) System.out.println("Welcome, " + currentUser.getUsername());
                else System.out.println("Login Failed.");
                break;
            case "2":
                System.out.print("Username: ");
                String rUser = scanner.nextLine();
                System.out.print("Email: ");
                String rEmail = scanner.nextLine();
                System.out.print("Password: ");
                String rPass = scanner.nextLine();
                userDAO.registerUser(rUser, rEmail, rPass, null, null);
                System.out.println("Registered! Please login.");
                break;
            case "3":
                System.out.print("Enter Email: ");
                String email = scanner.nextLine();
                String token = userDAO.generateRecoveryToken(email);
                if (token != null) {
                    System.out.println("[SIMULATION] Email sent! Your token is: " + token);
                    System.out.print("Enter Token: ");
                    String inToken = scanner.nextLine();
                    System.out.print("New Password: ");
                    String newPass = scanner.nextLine();
                    boolean reset = userDAO.resetPassword(inToken, newPass);
                    if (reset) System.out.println("Password reset successfully.");
                    else System.out.println("Invalid or expired token.");
                }
                break;
            case "4":
                System.exit(0);
        }
    }

    private static void showUserMenu() {
        System.out.println("\n--- HOME ---");
        System.out.println("1. View Timeline");
        System.out.println("2. View Trending (Analytics)");
        System.out.println("3. Notifications (Mentions)");
        System.out.println("4. Search");
        System.out.println("\n--- ACTIONS ---");
        System.out.println("5. Write Post");
        System.out.println("6. Interact with Post (Like/Comment)");
        System.out.println("7. Follow User");
        System.out.println("8. Direct Message");
        System.out.println("9. Logout");
        System.out.print("Select: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                postDAO.printTimeline(currentUser.getId());
                break;
            case "2":
                postDAO.viewTrending();
                break;
            case "3":
                userDAO.checkMentions(currentUser.getId());
                break;
            case "4":
                System.out.print("Search (U)sers or (P)osts? ");
                String type = scanner.nextLine();
                System.out.print("Keyword: ");
                String key = scanner.nextLine();
                if (type.equalsIgnoreCase("U")) userDAO.searchUsers(key);
                else postDAO.searchPosts(key);
                break;
            case "5":
                System.out.print("Content: ");
                postDAO.createPost(currentUser.getId(), scanner.nextLine());
                break;
            case "6":
                System.out.print("Enter Post ID: ");
                int pid = Integer.parseInt(scanner.nextLine());
                System.out.print("(L)ike or (C)omment or (V)iew Comments? ");
                String action = scanner.nextLine();
                if (action.equalsIgnoreCase("L")) {
                    interactionDAO.toggleLikePost(currentUser.getId(), pid);
                } else if (action.equalsIgnoreCase("C")) {
                    System.out.print("Content: ");
                    String txt = scanner.nextLine();
                    System.out.print("Reply to Comment ID (0 for none): ");
                    int cid = Integer.parseInt(scanner.nextLine());
                    interactionDAO.addComment(currentUser.getId(), pid, txt, cid);
                } else if (action.equalsIgnoreCase("V")) {
                    interactionDAO.viewComments(pid);
                }
                break;
            case "7":
                System.out.print("Username to follow: ");
                interactionDAO.followUser(currentUser.getId(), scanner.nextLine());
                break;
            case "8":
                System.out.print("To Username: ");
                String target = scanner.nextLine();
                System.out.print("Message: ");
                interactionDAO.sendDM(currentUser.getId(), target, scanner.nextLine());
                break;
            case "9":
                currentUser = null;
                break;
        }
    }
}