/**
 ANJRpg - an open source Role Playing Game written in Java.
 Copyright (C) 2014 - 2026 Alexander Nilov
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arifolth.anjrpg.menu;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedList;
import com.simsilica.lemur.list.CellRenderer;
import com.simsilica.lemur.style.ElementId;
import ru.arifolth.anjrpg.events.EventBus;
import ru.arifolth.anjrpg.events.GameEventListener;
import ru.arifolth.anjrpg.events.JournalEntryAddedEvent;
import ru.arifolth.anjrpg.interfaces.RolePlayingGameInterface;
import ru.arifolth.anjrpg.quest.JournalEntry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.simsilica.lemur.component.BorderLayout.Position.Center;
import static com.simsilica.lemur.component.BorderLayout.Position.North;
import static com.simsilica.lemur.component.BorderLayout.Position.West;

public class JournalPanel implements MenuPanel {

    private static final float TITLE_COLUMN_WIDTH = 200f;
    private static final float DESC_COLUMN_WIDTH  = 400f;
    private static final float TITLE_FONT_SIZE    = 20f;
    private static final float DESC_FONT_SIZE     = 16f;
    private static final float LIST_HEIGHT        = 400f;
    private static final float COLUMN_GAP         = 10f;

    /** Fully transparent but still pickable: keeps mouse-wheel hits landing on the list. */
    private static final ColorRGBA INVISIBLE_PICK_QUAD = new ColorRGBA(0f, 0f, 0f, 0f);

    private Container container;
    private final Application app;

    // ListBox gives us the viewport, the scrollbar and mouse-wheel scrolling
    // without us touching any mouse-event classes.
    private ListBox<JournalEntry> listBox;
    private final VersionedList<JournalEntry> listModel = new VersionedList<>();

    /** Scratch label used only to measure text width while word-wrapping. */
    private Label measureProbe;
    /** Entries are immutable: wrapped text is computed once per entry: [title, description]. */
    private final Map<JournalEntry, String[]> wrapCache = new HashMap<>();

    // render thread only: both the initial population and the enqueued live additions run there
    private final Set<JournalEntry> shownEntries = new HashSet<>();

    public JournalPanel(Application app) {
        this.app = app;
        RolePlayingGameInterface game = (RolePlayingGameInterface) app;

        container = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
        container.setBackground(null);

        Label title = container.addChild(new Label("Journal"));
        title.setFontSize(32);
        title.setInsets(new Insets3f(10, 10, 0, 10));

        container.addChild(createHeaderRow());

        listBox = new ListBox<>(listModel, new JournalEntryCellRenderer(), null);
        // Invisible quad: the list stays visually transparent (stone texture shows
        // through) but the mouse picker still hits it, which is what the internal
        // wheel-scrolling of ListBox relies on.
        listBox.setBackground(new QuadBackgroundComponent(INVISIBLE_PICK_QUAD));
        listBox.setPreferredSize(new Vector3f(TITLE_COLUMN_WIDTH + DESC_COLUMN_WIDTH + 40, LIST_HEIGHT, 0));
        listBox.setInsets(new Insets3f(0, 10, 10, 10));
        container.addChild(listBox);

        // Subscribe first, then take the snapshot: an entry that lands in between shows up
        // in both, and shownEntries drops the duplicate - nothing is ever missed.
        EventBus eventBus = game.getEventBus();
        eventBus.subscribe(JournalEntryAddedEvent.class, new WeakEntryListener(this, eventBus));

        game.getGameLogicCore().getJournalService().getAllEntries().forEach(this::addEntry);

        // Defer until after the first layout pass so the slider's internal arrow
        // buttons have their final positions (we detect up vs down by y).
        app.enqueue(() -> {
            makeSliderArrowsWork();
            return null;
        });
    }

    private Container createHeaderRow() {
        Container header = new Container(new BorderLayout());
        header.setBackground(null);
        header.setInsets(new Insets3f(10, 10, 5, 10));

        Label th = new Label("Quest");
        th.setFontSize(TITLE_FONT_SIZE);
        th.setPreferredSize(new Vector3f(TITLE_COLUMN_WIDTH, 30, 0));
        header.addChild(th, West);

        Label dh = new Label("Description");
        dh.setFontSize(TITLE_FONT_SIZE);
        header.addChild(dh, Center);
        return header;
    }

    private void addEntry(JournalEntry entry) {
        if (!shownEntries.add(entry)) {
            return;
        }
        listModel.add(entry);
    }

    private void onEntryAdded(JournalEntryAddedEvent event) {
        addEntry(event.getEntry());
    }

    /**
     * The '^'/'v' glyphs of the ListBox scrollbar belong to its internal Slider.
     * Wheel and thumb-drag already move the slider model correctly, but the
     * arrow buttons' own stepping never does in this build. So we locate those
     * two buttons inside the slider, drop whatever commands they carry and
     * wire them to step the very same model by exactly one row per click.
     * Purely additive: if the glyphs turn out to be texture only (no Button
     * spatials inside the slider), this is a silent no-op.
     */
    private void makeSliderArrowsWork() {
        List<Button> arrows = new ArrayList<>();
        collectButtons(listBox.getSlider(), arrows);
        if (arrows.size() < 2) {
            return; // texture-only slider: nothing to wire
        }
        // Lemur local y decreases downwards: the topmost button is the up-arrow
        arrows.sort((a, b) -> Float.compare(b.getLocalTranslation().y, a.getLocalTranslation().y));

        Button up = arrows.get(0);
        Button down = arrows.get(1);

        List<Command<? super Button>> upCmds = up.getClickCommands();
        if (upCmds != null) {
            upCmds.clear();
        }
        up.addClickCommands(source -> stepSliderRows(1));

        List<Command<? super Button>> downCmds = down.getClickCommands();
        if (downCmds != null) {
            downCmds.clear();
        }
        down.addClickCommands(source -> stepSliderRows(-1));
    }

    private void collectButtons(Spatial spatial, List<Button> out) {
        if (spatial instanceof Button) {
            out.add((Button) spatial);
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                collectButtons(child, out);
            }
        }
    }

    /**
     * Steps the ListBox scroll model by whole rows. The model range is in row
     * units (0..maxRowIndex, upside down), so +1 moves the view up one row and
     * -1 moves it down - the same model the wheel and the thumb drag already
     * drive, which is why the thumb keeps following the arrows for free.
     * Flip the signs at the call sites if the direction feels inverted.
     */
    private void stepSliderRows(int rows) {
        double value = listBox.getSlider().getModel().getValue();
        listBox.getSlider().getModel().setValue(value + rows);
    }

    /**
     * InGameMenuState builds a brand new panel every time the menu opens and
     * never tells the old one it is gone (the MenuPanel lifecycle hooks are not
     * called for top-level panels), so a plain field-held listener would stay
     * subscribed forever and pin every discarded panel in memory. This listener
     * holds the panel weakly and unsubscribes itself on the first event after
     * the panel has been collected.
     */
    private static final class WeakEntryListener implements GameEventListener<JournalEntryAddedEvent> {
        private final WeakReference<JournalPanel> panel;
        private final EventBus eventBus;

        private WeakEntryListener(JournalPanel panel, EventBus eventBus) {
            this.panel = new WeakReference<>(panel);
            this.eventBus = eventBus;
        }

        @Override
        public void onEvent(JournalEntryAddedEvent event) {
            JournalPanel target = panel.get();
            if (target == null) {
                eventBus.unsubscribe(JournalEntryAddedEvent.class, this);
                return;
            }
            target.onEntryAdded(event);
        }
    }

    @Override
    public void buildUI() {
        // UI is built in the constructor
    }

    @Override
    public Container getContainer() { return container; }

    /**
     * Renders one journal row as a transparent two-column element:
     * fixed-width title pinned to the top on the West, wrapped description
     * filling the Center. No visible background quad: the parent panel's
     * texture must show through.
     */
    private class JournalEntryCellRenderer implements CellRenderer<JournalEntry> {

        @Override
        public void configureStyle(ElementId elementId, String style) {
            // no-op: this renderer styles its rows programmatically
        }

        @Override
        public Panel getView(JournalEntry value, boolean selected, Panel existing) {
            RowComponents row;
            if (existing instanceof RowComponents) {
                row = (RowComponents) existing;
            } else {
                row = new RowComponents();
            }

            String[] wrapped = wrapCache.computeIfAbsent(value, e -> new String[]{
                    wrap(e.getTitle(), TITLE_FONT_SIZE, TITLE_COLUMN_WIDTH - COLUMN_GAP),
                    wrap(e.getContent(), DESC_FONT_SIZE, DESC_COLUMN_WIDTH)
            });
            row.setTitle(wrapped[0]);
            row.setDescription(wrapped[1]);
            return row;
        }
    }

    /**
     * One table row. Extends Container so it can be returned as a Panel by the
     * renderer, while keeping strong references to its labels for cheap reuse
     * while scrolling.
     *
     * Structure (no text-alignment API needed):
     *   row (BorderLayout, transparent)
     *    +- West:   titleColumn (fixed width) +- North: title (pinned to top)
     *    +- Center: wrapped description
     */
    private static class RowComponents extends Container {
        private final Label titleLabel;
        private final Label descLabel;

        RowComponents() {
            super(new BorderLayout());
            setBackground(null);
            setInsets(new Insets3f(4, 6, 4, 6));

            Container titleColumn = new Container(new BorderLayout());
            titleColumn.setBackground(null);
            titleColumn.setPreferredSize(new Vector3f(TITLE_COLUMN_WIDTH, 10, 0));

            titleLabel = new Label("");
            titleLabel.setFontSize(TITLE_FONT_SIZE);
            titleLabel.setInsets(new Insets3f(0, 0, 0, COLUMN_GAP));
            titleColumn.addChild(titleLabel, North);
            addChild(titleColumn, West);

            descLabel = new Label("");
            descLabel.setFontSize(DESC_FONT_SIZE);
            addChild(descLabel, Center);
        }

        void setTitle(String text) {
            titleLabel.setText(text);
        }

        void setDescription(String text) {
            descLabel.setText(text);
        }
    }

    /**
     * Lemur's Label has no native word wrap: multi-line text must contain
     * explicit '\n' characters. This inserts them so every line fits into
     * maxWidth, measuring each word with a throw-away Label that uses the
     * same font size as the real label.
     */
    private String wrap(String text, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (measureProbe == null) {
            measureProbe = new Label("");
            measureProbe.setInsets(new Insets3f(0, 0, 0, 0));
        }
        measureProbe.setFontSize(fontSize);

        StringBuilder result = new StringBuilder(text.length() + 16);
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            float lineWidth = 0f;
            for (String word : paragraph.split("(?<=\\s)")) {
                measureProbe.setText(word);
                float wordWidth = measureProbe.getPreferredSize().x;
                if (lineWidth + wordWidth > maxWidth && line.length() > 0) {
                    result.append(line.toString().trim()).append('\n');
                    line.setLength(0);
                    lineWidth = 0f;
                    word = word.replaceAll("^\\s+", "");
                    if (word.isEmpty()) {
                        continue;
                    }
                    measureProbe.setText(word);
                    wordWidth = measureProbe.getPreferredSize().x;
                }
                line.append(word);
                lineWidth += wordWidth;
            }
            result.append(line.toString().trim()).append('\n');
        }
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }
}