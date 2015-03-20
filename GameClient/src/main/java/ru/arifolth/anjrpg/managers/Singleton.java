package ru.arifolth.anjrpg.managers;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 27.12.12
 * Time: 1:36
 * To change this template use File | Settings | File Templates.
 */
public class Singleton
{
    private static Singleton INSTANCE = new Singleton();

    private Singleton() {}

    public static synchronized Singleton getInstance()
    {
        return INSTANCE;
    }
}
