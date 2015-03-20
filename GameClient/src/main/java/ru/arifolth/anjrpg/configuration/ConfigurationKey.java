package ru.arifolth.anjrpg.configuration;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 3:38
 * To change this template use File | Settings | File Templates.
 */
public enum ConfigurationKey {
    CONSTANT;

    public final String defaultValue;

    ConfigurationKey(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    ConfigurationKey () {
        this("");
    }
}