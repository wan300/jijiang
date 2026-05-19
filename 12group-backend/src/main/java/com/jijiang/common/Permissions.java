package com.jijiang.common;

public final class Permissions {
    private Permissions() {
    }

    // Service
    public static final String SERVICE_PUBLISH = "SERVICE_PUBLISH";
    public static final String SERVICE_REVIEW = "SERVICE_REVIEW";
    public static final String SERVICE_OFFLINE = "SERVICE_OFFLINE";

    // Order
    public static final String ORDER_CREATE = "ORDER_CREATE";
    public static final String ORDER_ACCEPT = "ORDER_ACCEPT";
    public static final String ORDER_DELIVER = "ORDER_DELIVER";
    public static final String ORDER_CONFIRM = "ORDER_CONFIRM";

    // Payment
    public static final String PAYMENT_CREATE = "PAYMENT_CREATE";
    public static final String PAYMENT_SYNC = "PAYMENT_SYNC";

    // Review
    public static final String REVIEW_SUBMIT = "REVIEW_SUBMIT";

    // Message
    public static final String MESSAGE_SEND = "MESSAGE_SEND";

    // User
    public static final String USER_VERIFY = "USER_VERIFY";
    public static final String USER_VERIFY_REVIEW = "USER_VERIFY_REVIEW";
    public static final String USER_DELETE = "USER_DELETE";

    // Deposit
    public static final String DEPOSIT_MANAGE = "DEPOSIT_MANAGE";
    public static final String DEPOSIT_PAY = "DEPOSIT_PAY";

    // Admin
    public static final String ADMIN_DASHBOARD = "ADMIN_DASHBOARD";
    public static final String ADMIN_ORDER_MANAGE = "ADMIN_ORDER_MANAGE";
    public static final String ADMIN_MANAGE = "ADMIN_MANAGE";
}
