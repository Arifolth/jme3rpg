/**
 * ANJRpg - an open source Role Playing Game written in Java.
 * Copyright (C) 2014 - 2026 Alexander Nilov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ru.arifolth.anjrpg.interfaces.stats;

public class CharacterStats extends BaseStats {
    private int level = 1;
    private float health;
    private float mana;
    private float stamina;

    public CharacterStats(float maxHealth, float maxMana, float maxStamina, int level) {
        super(maxHealth, maxMana, maxStamina);
        this.health = maxHealth;
        this.mana = maxMana;
        this.stamina = maxStamina;
    }

    // Getters & setters (no logic)
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = health; }

    public float getMana() { return mana; }
    public void setMana(float mana) { this.mana = mana; }

    public float getStamina() { return stamina; }
    public void setStamina(float stamina) { this.stamina = stamina; }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}