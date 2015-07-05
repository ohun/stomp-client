package com.ohun.stomp.protocol;

public enum Version {

    VERSION_1_0("1.0", 1.0F),
    VERSION_1_1("1.1", 1.1F),
    VERSION_1_2("1.2", 1.2F);

    public static Version toVersion(String value) {
        for (Version version : values()) {
            if (version.name.equals(value)) return version;
        }
        return VERSION_1_2;
    }

    public static String supported() {
        return VERSION_1_0.name + ',' + Version.VERSION_1_1.name + ',' + VERSION_1_2.name;
    }

    public final String name;
    public final float value;

    Version(String name, float value) {
        this.name = name;
        this.value = value;
    }


}