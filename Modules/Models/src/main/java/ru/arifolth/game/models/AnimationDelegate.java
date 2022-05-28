/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2021 Alexander Nilov
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.arifolth.game.models;

import com.jme3.animation.LoopMode;
import ru.arifolth.game.AnimationDelegateInterface;

public class AnimationDelegate implements AnimationDelegateInterface {
    private final PlayerCharacter playerCharacter;

    public AnimationDelegate(PlayerCharacter playerCharacter) {
        this.playerCharacter = playerCharacter;
    }

    @Override
    public void attackAnimation() {
        playerCharacter.getAttackChannel().setAnim(AnimConstants.ATTACK, 0.1f);
        playerCharacter.getAttackChannel().setLoopMode(LoopMode.DontLoop);
        playerCharacter.getAttackChannel().setSpeed(1f);
        playerCharacter.setActionTime(playerCharacter.getAttackChannel().getAnimMaxTime());
    }

    @Override
    public void stopAnimation() {
        playerCharacter.getAttackChannel().setAnim(AnimConstants.IDLE, 0f);
        playerCharacter.getAttackChannel().setSpeed(1f);
    }

    @Override
    public void idleAnimation() {
        playerCharacter.getAnimationChannel().setLoopMode(LoopMode.Loop);
        if (!playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.IDLE)) {
            playerCharacter.getAnimationChannel().setAnim(AnimConstants.IDLE, 0f);
            playerCharacter.getAnimationChannel().setSpeed(1f);
        }
    }

    @Override
    public void walkingAnimation() {
        //set the walking animation
        playerCharacter.getAnimationChannel().setLoopMode(LoopMode.Loop);
        if (!playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.WALK)) {
            playerCharacter.getAnimationChannel().setAnim(AnimConstants.WALK, 0.5f);
        }
        if (playerCharacter.isRunning()) {
            playerCharacter.getAnimationChannel().setSpeed(1.75f);
        } else {
            playerCharacter.getAnimationChannel().setSpeed(1f);
        }
    }

    @Override
    public void deathAnimation() {
        playerCharacter.getAnimationChannel().setLoopMode(LoopMode.DontLoop);
        if (!playerCharacter.getAnimationChannel().getAnimationName().equals(AnimConstants.DEATH)) {
            playerCharacter.getAnimationChannel().setAnim(AnimConstants.DEATH, 0.1f);
            playerCharacter.getAnimationChannel().setSpeed(1f);
        }
    }

    @Override
    public void blockAnimation() {
        //TODO: Show Blocking animation only in case attack is coming, do nothing otherwise
        playerCharacter.getAttackChannel().setAnim(AnimConstants.BLOCK, 0.1f);
        //TODO: ADD Blocking event
        playerCharacter.getAttackChannel().setLoopMode(LoopMode.DontLoop);
        playerCharacter.getAttackChannel().setSpeed(1f);
        playerCharacter.getAttackChannel().setTime(playerCharacter.getAttackChannel().getAnimMaxTime() / 2);
        playerCharacter.setActionTime(playerCharacter.getAttackChannel().getAnimMaxTime() / 2);
    }
}