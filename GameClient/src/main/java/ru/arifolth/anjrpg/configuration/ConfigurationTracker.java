package ru.arifolth.anjrpg.configuration;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 3:34
 * To change this template use File | Settings | File Templates.
 */
public abstract class ConfigurationTracker implements ConfigurationUpdateListener {
    public ConfigurationTracker() {
        Configuration.getInstance().subscribe(this);
    }
}