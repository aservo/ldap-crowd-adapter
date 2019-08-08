package net.wimpi.crowd.ldap.util;


public enum MemberOfSupport {

    OFF("off"), NORMAL("normal"), NESTED_GROUPS("nested-groups"), FLATTENING("flattening");

    private final String text;

    MemberOfSupport(String text) {

        this.text = text;
    }

    @Override
    public String toString() {

        return text;
    }

    public boolean allowMemberOfAttribute() {

        return !this.equals(MemberOfSupport.OFF);
    }
}
