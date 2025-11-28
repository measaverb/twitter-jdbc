package com.twitter.app;

import com.twitter.dao.InteractionDAO;
import com.twitter.dao.PostDAO;
import com.twitter.dao.UserDAO;
import com.twitter.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TwitterGUI extends JFrame {

    private final UserDAO userDAO = new UserDAO();
    private final PostDAO postDAO = new PostDAO();
    private final InteractionDAO interactionDAO = new InteractionDAO();
    private User currentUser;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private JTextField userField, regUserField, regEmailField;
    private JPasswordField passField, regPassField;
    private JPanel feedContainer;

    public TwitterGUI() {
        setTitle("Twitter Clone");
        setSize(500, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        mainPanel.add(createDashboardPanel(), "DASHBOARD");

        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JLabel title = new JLabel("Twitter Login", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(29, 161, 242));

        userField = new JTextField();
        userField.setBorder(BorderFactory.createTitledBorder("Username"));

        passField = new JPasswordField();
        passField.setBorder(BorderFactory.createTitledBorder("Password"));

        JButton loginBtn = new JButton("Log In");
        loginBtn.setBackground(new Color(29, 161, 242));
        loginBtn.setForeground(Color.BLACK);

        JButton regBtn = new JButton("Register");
        JButton forgotBtn = new JButton("Forgot Password?");

        loginBtn.addActionListener(e -> performLogin());
        regBtn.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        forgotBtn.addActionListener(e -> performRecovery());

        panel.add(title);
        panel.add(userField);
        panel.add(passField);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.add(loginBtn);
        btnPanel.add(regBtn);
        panel.add(btnPanel);
        panel.add(forgotBtn);

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        regUserField = new JTextField();
        regUserField.setBorder(BorderFactory.createTitledBorder("Username"));

        regEmailField = new JTextField();
        regEmailField.setBorder(BorderFactory.createTitledBorder("Email"));

        regPassField = new JPasswordField();
        regPassField.setBorder(BorderFactory.createTitledBorder("Password"));

        JButton regBtn = new JButton("Sign Up");
        regBtn.setForeground(Color.BLACK);

        JButton backBtn = new JButton("Back");

        regBtn.addActionListener(e -> performRegister());
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        panel.add(new JLabel("Create Account", SwingConstants.CENTER));
        panel.add(regUserField);
        panel.add(regEmailField);
        panel.add(regPassField);

        JPanel bp = new JPanel(new GridLayout(1, 2));
        bp.add(regBtn);
        bp.add(backBtn);
        panel.add(bp);

        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton refreshBtn = new JButton("Refresh");
        JButton searchBtn = new JButton("Search");
        JButton trendBtn = new JButton("Trends");
        JButton notifBtn = new JButton("Mentions");

        JButton netBtn = new JButton("Network");
        JButton msgBtn = new JButton("Messages");
        JButton postBtn = new JButton("Post");
        JButton logoutBtn = new JButton("Logout");

        refreshBtn.addActionListener(e -> loadTimeline());
        searchBtn.addActionListener(e -> performSearch());
        trendBtn.addActionListener(e -> showPopupList("Trending Posts", postDAO.getTrendingList()));
        notifBtn.addActionListener(e -> {
            List<String> mentions = userDAO.getMentionsList(currentUser.getId());
            showPopupList("Your Mentions", mentions);
        });
        netBtn.addActionListener(e -> showNetworkDialog());
        msgBtn.addActionListener(e -> showMessagesDialog());
        postBtn.addActionListener(e -> performWritePost());
        logoutBtn.addActionListener(e -> performLogout());


        toolBar.add(refreshBtn);
        toolBar.add(searchBtn);
        toolBar.add(trendBtn);
        toolBar.add(notifBtn);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(netBtn);
        toolBar.add(msgBtn);
        toolBar.add(postBtn);
        toolBar.add(logoutBtn);

        panel.add(toolBar, BorderLayout.NORTH);

        feedContainer = new JPanel();
        feedContainer.setLayout(new BoxLayout(feedContainer, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(feedContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPostPanel(PostDAO.TimelinePost post) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1000));

        JLabel meta = new JLabel(String.format("<html><b>@%s</b> <font color='gray'>[ID: %d]</font></html>", post.username, post.postId));

        JTextArea content = new JTextArea(post.content);
        content.setWrapStyleWord(true);
        content.setLineWrap(true);
        content.setEditable(false);
        content.setFont(new Font("SansSerif", Font.PLAIN, 14));
        content.setSize(new Dimension(400, 1));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bottom.setBackground(Color.WHITE);

        JLabel stats = new JLabel(String.format("Views:%d Likes:%d ", post.viewCount, post.likeCount));
        stats.setForeground(Color.GRAY);

        JButton likeBtn = new JButton(post.isLikedByCurrentUser ? "Unlike" : "Like");
        likeBtn.setForeground(post.isLikedByCurrentUser ? Color.RED : Color.BLUE);
        likeBtn.addActionListener(e -> {
            interactionDAO.toggleLikePost(currentUser.getId(), post.postId);
            loadTimeline();
        });

        JButton commentBtn = new JButton("Reply");
        commentBtn.setForeground(Color.BLACK);
        commentBtn.addActionListener(e -> performComment(post.postId));

        JButton toggleCommBtn = new JButton("View Comments");
        toggleCommBtn.setForeground(Color.BLACK);

        bottom.add(stats);
        bottom.add(likeBtn);
        bottom.add(commentBtn);
        bottom.add(toggleCommBtn);

        JPanel commentSection = new JPanel();
        commentSection.setLayout(new BoxLayout(commentSection, BoxLayout.Y_AXIS));
        commentSection.setBackground(new Color(245, 248, 250));
        commentSection.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
        commentSection.setVisible(false);
        toggleCommBtn.addActionListener(e -> {
            boolean isVisible = commentSection.isVisible();
            if (!isVisible) {
                commentSection.removeAll();
                List<InteractionDAO.CommentNode> roots = interactionDAO.getCommentTree(post.postId);

                if (roots.isEmpty()) {
                    JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    emptyPanel.setBackground(new Color(245, 248, 250));
                    JLabel empty = new JLabel("No comments yet.");
                    empty.setFont(new Font("SansSerif", Font.ITALIC, 12));
                    emptyPanel.add(empty);
                    commentSection.add(emptyPanel);
                } else {
                    for (InteractionDAO.CommentNode node : roots) {
                        renderCommentsRecursive(commentSection, node, 0, post.postId);
                    }
                }
                toggleCommBtn.setText("Collapse");
                commentSection.setVisible(true);
            } else {
                commentSection.setVisible(false);
                toggleCommBtn.setText("View Comments");
            }
            feedContainer.revalidate();
            feedContainer.repaint();
        });

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(Color.WHITE);
        centerContainer.add(content, BorderLayout.NORTH);
        centerContainer.add(bottom, BorderLayout.CENTER);
        centerContainer.add(commentSection, BorderLayout.SOUTH);

        card.add(meta, BorderLayout.NORTH);
        card.add(centerContainer, BorderLayout.CENTER);

        return card;
    }

    private void renderCommentsRecursive(JPanel container, InteractionDAO.CommentNode node, int depth, int postId) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setBackground(new Color(245, 248, 250));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        int leftPadding = 10 + (depth * 30);
        rowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, leftPadding, 5, 5)
        ));

        String html = String.format("<html><b>@%s</b> <font color='gray' size='2'>[ID:%d]</font><br>%s</html>",
                node.username, node.commentId, node.content);
        JLabel textLabel = new JLabel(html);

        JButton replyBtn = new JButton("Reply");
        replyBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        replyBtn.setMargin(new Insets(2, 5, 2, 5));
        replyBtn.setForeground(Color.BLACK);
        replyBtn.setFocusable(false);

        replyBtn.addActionListener(e -> performComment(postId, node.commentId));

        rowPanel.add(textLabel, BorderLayout.CENTER);
        rowPanel.add(replyBtn, BorderLayout.EAST);

        container.add(rowPanel);

        for (InteractionDAO.CommentNode child : node.replies) {
            renderCommentsRecursive(container, child, depth + 1, postId);
        }
    }

    private void showMessagesDialog() {
        JDialog d = new JDialog(this, "Messages", true);
        d.setSize(500, 400);
        d.setLocationRelativeTo(this);

        JTabbedPane tabs = new JTabbedPane();

        DefaultListModel<String> m = new DefaultListModel<>();
        List<InteractionDAO.DirectMessage> inbox = interactionDAO.getInbox(currentUser.getId());
        if (inbox.isEmpty()) m.addElement("No messages.");
        else {
            for (InteractionDAO.DirectMessage dm : inbox) {
                m.addElement(String.format("<html><b>From: @%s</b>: %s</html>", dm.sender, dm.content));
            }
        }
        tabs.addTab("Inbox", new JScrollPane(new JList<>(m)));

        JPanel p = new JPanel(new GridLayout(3, 1));
        JTextField to = new JTextField();
        to.setBorder(BorderFactory.createTitledBorder("To"));

        JTextField txt = new JTextField();
        txt.setBorder(BorderFactory.createTitledBorder("Message"));

        JButton send = new JButton("Send");
        send.setBackground(new Color(29, 161, 242));
        send.setForeground(Color.BLACK);

        send.addActionListener(ev -> {
            interactionDAO.sendDM(currentUser.getId(), to.getText(), txt.getText());
            d.dispose();
            JOptionPane.showMessageDialog(this, "Message Sent!");
        });

        p.add(to);
        p.add(txt);
        p.add(send);
        tabs.addTab("Compose", p);
        d.add(tabs);
        d.setVisible(true);
    }

    private void performRecovery() {
        String email = JOptionPane.showInputDialog(this, "Enter Email for Recovery:");
        if (email != null) {
            String token = userDAO.generateRecoveryToken(email);
            if (token != null) {
                String inToken = JOptionPane.showInputDialog(this, "Token Generated: " + token + "\nEnter Token:");
                String newPass = JOptionPane.showInputDialog(this, "Enter New Password:");
                if (userDAO.resetPassword(inToken, newPass))
                    JOptionPane.showMessageDialog(this, "Password Reset Success!");
                else JOptionPane.showMessageDialog(this, "Failed.");
            } else JOptionPane.showMessageDialog(this, "Email not found.");
        }
    }

    private void performSearch() {
        String[] options = {"Users", "Posts"};
        int type = JOptionPane.showOptionDialog(this, "Search for?", "Search", 0, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        String key = JOptionPane.showInputDialog(this, "Enter keyword:");
        if (key != null) {
            if (type == 0) userDAO.searchUsers(key);
            else postDAO.searchPosts(key);
            JOptionPane.showMessageDialog(this, "Results printed to Console.");
        }
    }

    private void performComment(int postId) {
        performComment(postId, 0);
    }

    private void performComment(int postId, int parentId) {
        String title = (parentId == 0) ? "Write a comment:" : "Reply to Comment ID " + parentId + ":";

        String txt = JOptionPane.showInputDialog(this, title);

        if (txt != null && !txt.trim().isEmpty()) {
            interactionDAO.addComment(currentUser.getId(), postId, txt, parentId);

            JOptionPane.showMessageDialog(this, "Comment Added!");

            loadTimeline();
        }
    }

    private void showPopupList(String title, List<String> items) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(400, 500);
        d.setLocationRelativeTo(this);
        DefaultListModel<String> m = new DefaultListModel<>();
        if (items.isEmpty()) m.addElement("No items found.");
        else items.forEach(m::addElement);
        d.add(new JScrollPane(new JList<>(m)));
        d.setVisible(true);
    }

    private void showNetworkDialog() {
        JDialog d = new JDialog(this, "Network", true);
        d.setSize(400, 400);
        d.setLocationRelativeTo(this);
        JTabbedPane tabs = new JTabbedPane();
        DefaultListModel<String> m1 = new DefaultListModel<>();
        interactionDAO.getFollowers(currentUser.getId()).forEach(m1::addElement);
        DefaultListModel<String> m2 = new DefaultListModel<>();
        interactionDAO.getFollowing(currentUser.getId()).forEach(m2::addElement);
        tabs.addTab("Followers", new JScrollPane(new JList<>(m1)));
        tabs.addTab("Following", new JScrollPane(new JList<>(m2)));
        d.add(tabs);
        d.setVisible(true);
    }

    private void performLogin() {
        User u = userDAO.loginUser(userField.getText(), new String(passField.getPassword()));
        if (u != null) {
            currentUser = u;
            cardLayout.show(mainPanel, "DASHBOARD");
            loadTimeline();
        } else JOptionPane.showMessageDialog(this, "Failed");
    }

    private void performRegister() {
        if (userDAO.registerUser(regUserField.getText(), regEmailField.getText(), new String(regPassField.getPassword()), null, null))
            cardLayout.show(mainPanel, "LOGIN");
    }

    private void performWritePost() {
        String s = JOptionPane.showInputDialog("Post Content:");
        if (s != null) {
            postDAO.createPost(currentUser.getId(), s);
            loadTimeline();
        }
    }

    private void performLogout() {
        currentUser = null;
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void loadTimeline() {
        feedContainer.removeAll();
        for (PostDAO.TimelinePost p : postDAO.getTimelinePosts(currentUser.getId())) {
            feedContainer.add(createPostPanel(p));
            feedContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        feedContainer.revalidate();
        feedContainer.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TwitterGUI().setVisible(true));
    }
}