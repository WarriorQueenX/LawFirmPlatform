package com.lawfirm.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {

    private static final long SESSION_TIMEOUT_MS =
            30L * 60 * 1000; // 30 minutes

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<String, Session> SESSIONS =
            new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static String createSession(Map<String, Object> user) {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        long now = System.currentTimeMillis();

        Session session = new Session(
                user.get("userId"),
                String.valueOf(user.get("username")),
                String.valueOf(user.get("role")),
                user.get("clientId"),
                user.get("lawyerId"),
                now
        );

        SESSIONS.put(token, session);
        return token;
    }

    public static Session getSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        Session session = SESSIONS.get(token);

        if (session == null) {
            return null;
        }

        if (System.currentTimeMillis() - session.createdAt
                >= SESSION_TIMEOUT_MS) {
            SESSIONS.remove(token, session);
            return null;
        }

        return session;
    }

    public static void invalidateSession(String token) {
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    public static final class Session {
        public final Object userId;
        public final String username;
        public final String role;
        public final Object clientId;
        public final Object lawyerId;
        private final long createdAt;

        private Session(
                Object userId,
                String username,
                String role,
                Object clientId,
                Object lawyerId,
                long createdAt
        ) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.clientId = clientId;
            this.lawyerId = lawyerId;
            this.createdAt = createdAt;
        }
    }
}
