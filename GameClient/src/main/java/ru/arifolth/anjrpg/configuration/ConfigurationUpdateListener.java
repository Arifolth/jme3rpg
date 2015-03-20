package ru.arifolth.anjrpg.configuration;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 3:33
 * To change this template use File | Settings | File Templates.
 */
public interface ConfigurationUpdateListener {
    /**
     * Allows subscribers to re-read changed variables
     * @throws Exception
     */
    public void reloadConfiguration() throws Exception;
}
