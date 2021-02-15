package ru.arifolth.game.models.factory;

import ru.arifolth.game.models.Character;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 20.12.12
 * Time: 23:15
 * To change this template use File | Settings | File Templates.
 */
public interface ICharacterFactory <T extends Character> {
    public T createCharacter(Class<T> clazz);
}
