package net.wimpi.crowd.ldap.util;


public enum OuType {

    GROUP(Utils.OU_GROUPS), USER(Utils.OU_USERS), UNDEFINED(null);

    private final String text;

    OuType(String text) {

        this.text = text;
    }

    @Override
    public String toString() {

        return String.valueOf(text);
    }
}
