/**
 *     ANJRpg - an open source Role Playing Game written in Java.
 *     Copyright (C) 2014 - 2026 Alexander Nilov
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

package ru.arifolth.anjrpg.interfaces;

import ru.arifolth.anjrpg.events.CombatDisengagedEvent;
import ru.arifolth.anjrpg.events.CombatEngagedEvent;
import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.GameStateChangedEvent;
import ru.arifolth.anjrpg.events.NpcKilledEvent;
import ru.arifolth.anjrpg.events.PlayerDiedEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Tracks which NPCs are currently engaged with the player and keeps
 * {@link GameState#BATTLE} / {@link GameState#CALM} in step with that.
 *
 * <p>Event-driven: engagement is reported through {@link CombatEngagedEvent} /
 * {@link CombatDisengagedEvent} (published by {@code NonPlayerCharacter} on the
 * actual transitions), {@link NpcKilledEvent} and {@link PlayerDiedEvent}. All
 * handlers run on the bus's single dispatcher thread, so {@code engagedEnemies}
 * is only ever touched from that thread; {@code inCombat} is volatile because
 * {@link #isInCombat()} may be read from the render thread.</p>
 *
 * <p>It also listens to {@link GameStateChangedEvent}. This replaces what the
 * old per-frame {@code update(tpf)} poll did implicitly: if a committed state
 * change ever leaves the game state out of step with the engagement set (for
 * example a pending {@code CALM} committing right after a new engagement),
 * the next state-change event re-proposes the correct state.</p>
 */
public class CombatTracker implements CombatTrackerInterface {
    final private static Logger LOGGER = Logger.getLogger(CombatTracker.class.getName());
    private final Set<CharacterInterface> engagedEnemies = new HashSet<>();
    private final GameStateManagerInterface gameStateManager;
    private volatile boolean inCombat = false;
    private volatile boolean playerDead = false;

    // Field-held listeners (Pattern A), same as SoundManager / KillTracker.
    private final GameEventListener<CombatEngagedEvent> engagedListener = this::onEngaged;
    private final GameEventListener<CombatDisengagedEvent> disengagedListener = this::onDisengaged;
    private final GameEventListener<NpcKilledEvent> killedListener = this::onNpcKilled;
    private final GameEventListener<PlayerDiedEvent> playerDiedListener = this::onPlayerDied;
    private final GameEventListener<GameStateChangedEvent> stateListener = this::onGameStateChanged;

    public CombatTracker(GameStateManagerInterface gameStateManager, EventBus eventBus) {
        this.gameStateManager = gameStateManager;

        eventBus.subscribe(CombatEngagedEvent.class, engagedListener);
        eventBus.subscribe(CombatDisengagedEvent.class, disengagedListener);
        eventBus.subscribe(NpcKilledEvent.class, killedListener);
        eventBus.subscribe(PlayerDiedEvent.class, playerDiedListener);
        eventBus.subscribe(GameStateChangedEvent.class, stateListener);
    }

    private void onEngaged(CombatEngagedEvent event) {
        if(playerDead)
            return;

        engagedEnemies.add(event.getAttacker());
        reconcile();
    }

    private void onDisengaged(CombatDisengagedEvent event) {
        engagedEnemies.remove(event.getAttacker());
        reconcile();
    }

    private void onNpcKilled(NpcKilledEvent event) {
        engagedEnemies.remove(event.getNpc());
        reconcile();
    }

    private void onPlayerDied(PlayerDiedEvent event) {
        playerDead = true;
        engagedEnemies.clear();
        inCombat = false;
        // No state proposal here: PlayerCharacter.die() already proposes GameState.DEATH itself.
    }

    private void onGameStateChanged(GameStateChangedEvent event) {
        if(GameState.DEATH.equals(event.getFrom())) {
            playerDead = false;
        }
        reconcile();
    }

    /**
     * Same rule the old polling {@code update(tpf)} applied every frame, now run
     * only when something relevant happened: in combat but not in BATTLE -> join
     * battle; out of combat but still in BATTLE -> leave battle. Proposals the
     * state machine does not accept ({@code GameState#isNextAcceptable}) are
     * simply dropped by {@code setGameState}, as before.
     */
    private void reconcile() {
        if(playerDead)
            return;

        inCombat = !engagedEnemies.isEmpty();

        GameState current = gameStateManager.getCurrentGameState();
        if(inCombat && !GameState.BATTLE.equals(current)) {
            joinBattle();
        } else if(!inCombat && GameState.BATTLE.equals(current)) {
            leaveBattle();
        }
    }

    private void leaveBattle() {
        gameStateManager.setGameState(GameState.CALM);
    }

    private void joinBattle() {
        gameStateManager.setGameState(GameState.BATTLE);
    }

    @Override
    public boolean isInCombat() {
        return inCombat;
    }
}
