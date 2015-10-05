/**
 * Copyright (C) 2015 eBusiness Information
 *
 * This file is part of OSM Contributor.
 *
 * OSM Contributor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OSM Contributor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OSM Contributor.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.mapsquare.osmcontributor.type;

import android.support.design.widget.Snackbar;
import android.view.View.OnClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import io.mapsquare.osmcontributor.R;
import io.mapsquare.osmcontributor.core.PoiManager;
import io.mapsquare.osmcontributor.core.events.PoiTypesLoaded;
import io.mapsquare.osmcontributor.core.model.PoiType;
import io.mapsquare.osmcontributor.core.model.PoiTypeTag;
import io.mapsquare.osmcontributor.type.dto.Combinations;
import io.mapsquare.osmcontributor.type.dto.CombinationsData;
import io.mapsquare.osmcontributor.type.dto.Suggestions;
import io.mapsquare.osmcontributor.type.dto.Wiki;
import io.mapsquare.osmcontributor.type.event.BasePoiTagEvent;
import io.mapsquare.osmcontributor.type.event.BasePoiTypeEvent;
import io.mapsquare.osmcontributor.type.event.PleaseDownloadPoiTypeSuggestionEvent;
import io.mapsquare.osmcontributor.type.event.PoiTagCreatedEvent;
import io.mapsquare.osmcontributor.type.event.PoiTagDeletedEvent;
import io.mapsquare.osmcontributor.type.event.PoiTypeCreatedEvent;
import io.mapsquare.osmcontributor.type.event.PoiTypeDeletedEvent;
import io.mapsquare.osmcontributor.type.event.PoiTypeSuggestedDownloadedEvent;
import io.mapsquare.osmcontributor.type.event.TypeSuggestionsDownloadedEvent;
import io.mapsquare.osmcontributor.type.rest.OsmTagInfoRestClient;
import io.mapsquare.osmcontributor.utils.EventCountDownTimer;
import io.mapsquare.osmcontributor.utils.StringUtils;
import timber.log.Timber;


@Singleton
public class TypeManager {

    private static final int SUGGESTIONS_PER_PAGE = 5;
    private static final int SUGGESTIONS_DELAY = 1000;

    private EventBus bus;
    private PoiManager poiManager;
    private OsmTagInfoRestClient tagInfoRestClient;
    private EventCountDownTimer suggestionsTimer;

    private final Object lock = new Object();
    private Snackbar lastSnackBar;

    private static List<String> tagsGroup = Arrays.asList("aerialway", "aeroway", "amenity", "barrier", "craft", "emergency", "geological",
            "highway", "cycleway", "busway", "historic", "landuse", "leisure", "man_made", "military",
            "natural", "office", "place", "power", "public_transport", "railway", "service", "route",
            "shop", "sport", "tourism", "waterway");

    @Inject
    public TypeManager(EventBus bus, PoiManager poiManager, OsmTagInfoRestClient tagInfoRestClient) {
        this.bus = bus;
        this.poiManager = poiManager;
        this.tagInfoRestClient = tagInfoRestClient;
        suggestionsTimer = new EventCountDownTimer(SUGGESTIONS_DELAY, SUGGESTIONS_DELAY, bus);
    }

    // ********************************
    // ************ Events ************
    // ********************************

    public void onEventBackgroundThread(InternalSavePoiTypeEvent event) {
        PoiType poiType = event.getPoiType();
        poiManager.savePoiType(poiType);
        bus.post(new PoiTypeCreatedEvent(poiType));
    }

    public void onEventBackgroundThread(InternalSavePoiTagEvent event) {
        PoiTypeTag poiTypeTag = event.getPoiTypeTag();
        PoiType poiType = poiTypeTag.getPoiType();
        poiManager.savePoiType(poiType);
        bus.post(new PoiTagCreatedEvent(poiTypeTag));
    }

    public void onEventBackgroundThread(InternalRemovePoiTypeEvent event) {
        PoiType poiType = event.getPoiType();
        poiManager.deletePoiType(poiType);
        Timber.i("Removed poi type %d", poiType.getId());
        bus.post(new PoiTypeDeletedEvent(poiType));
        bus.post(new PoiTypesLoaded(poiManager.loadPoiTypes()));
    }

    public void onEventBackgroundThread(InternalRemovePoiTagEvent event) {
        PoiTypeTag poiTypeTag = event.getPoiTypeTag();
        PoiType poiType = poiTypeTag.getPoiType(); // FIXME clone POI type to avoid border effects?
        poiType.getTags().remove(poiTypeTag);
        poiManager.savePoiType(poiType);
        Timber.i("Removed poi tag %d", poiTypeTag.getId());
        bus.post(new PoiTagDeletedEvent(poiTypeTag));
    }

    public void onEventBackgroundThread(InternalDownloadTypeSuggestionsEvent event) {
        Suggestions result = tagInfoRestClient.getSuggestions(event.getQuery(), event.getPage(), SUGGESTIONS_PER_PAGE);
        bus.post(new TypeSuggestionsDownloadedEvent(result));
    }

    public void onEventBackgroundThread(PleaseDownloadPoiTypeSuggestionEvent event) {
        bus.post(new PoiTypeSuggestedDownloadedEvent(getPoiTypeSuggested(event.getPoiTypeName())));
    }

    // ********************************
    // ************ public ************
    // ********************************

    /**
     * Create or edit a poi type.
     *
     * @param poiType The poi type to persist
     */
    public void savePoiType(PoiType poiType) {
        bus.post(new InternalSavePoiTypeEvent(poiType));
    }

    /**
     * Create or edit a poi type tag.
     *
     * @param poiTypeTag The poi type tag to persist
     */
    public void savePoiTag(PoiTypeTag poiTypeTag) {
        bus.post(new InternalSavePoiTagEvent(poiTypeTag));
    }

    /**
     * Delete a poi type after a short time.<br/>
     * A snackbar is shown in order to allow the user to cancel the deletion. If a snackbar was already
     * showing, it will be dismissed.
     * <p>
     * The deletion does not take place until the snackbar is dismissed or hidden by another snackbar.<br/>
     * The deletion does not take place if the undo action is clicked.
     * </p>
     *
     * @param item       The poi type to delete
     * @param snackbar   A new snackbar instance
     * @param undoAction The callback used for the undo action
     */
    public void deletePoiTypeDelayed(PoiType item, Snackbar snackbar, OnClickListener undoAction) {
        Timber.d("Delaying poi type %d removal", item.getId());
        displayNewUndoSnackbar(snackbar, undoAction, new DeleteTaskCallback(new InternalRemovePoiTypeEvent(item)));
    }

    /**
     * Delete a poi type tag after a short time.<br/>
     * A snackbar is shown in order to allow the user to cancel the deletion. If a snackbar was already
     * showing, it will be dismissed.
     * <p>
     * The deletion does not take place until the snackbar is dismissed or hidden by another snackbar.<br/>
     * The deletion does not take place if the undo action is clicked.
     * </p>
     *
     * @param item       The poi type tag to delete
     * @param snackbar   A new snackbar instance
     * @param undoAction The callback used for the undo action
     */
    public void deletePoiTagDelayed(PoiTypeTag item, Snackbar snackbar, OnClickListener undoAction) {
        Timber.d("Delaying poi tag %d removal", item.getId());
        displayNewUndoSnackbar(snackbar, undoAction, new DeleteTaskCallback(new InternalRemovePoiTagEvent(item)));
    }

    /**
     * Force the last deletion job to be done: if a snackbar was still visible, it will be dismissed
     * and its task will be executed.
     */
    public void finishLastDeletionJob() {
        synchronized (lock) {
            Snackbar snackbar = lastSnackBar;
            if (snackbar != null && snackbar.isShown()) {
                snackbar.dismiss();
                lastSnackBar = null;
            }
        }
    }

    /**
     * Request POI types suggestions for a given search query.
     * <br/>
     * A {@link TypeSuggestionsDownloadedEvent} will be posted to the event bus once suggestions
     * are downloaded.
     *
     * @param query The query.
     * @param page  The page number.
     */
    public void querySuggestions(String query, Integer page) {
        suggestionsTimer.cancel();
        suggestionsTimer.setEvent(new InternalDownloadTypeSuggestionsEvent(query, page));
        suggestionsTimer.start();
    }

    // *********************************
    // ************ private ************
    // *********************************

    /**
     * Return the PoiType suggested for a given key.
     *
     * @param key The name of the wished PoiType.
     * @return The suggested PoiType.
     */
    private PoiType getPoiTypeSuggested(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }

        List<Wiki> wikis = tagInfoRestClient.getWikiPages(key);
        PoiType poiType = new PoiType();
        poiType.setName(key);
        poiType.setIcon(key);
        int ordinal = 0;
        List<PoiTypeTag> poiTypeTags = new ArrayList<>();

        // Request for the English wiki and keep the tags of the tags_combination field.
        for (Wiki wiki : wikis) {
            if ("en".equals(wiki.getLang())) {
                for (String tagCombination : wiki.getTagsCombination()) {
                    String[] splitResult = tagCombination.split("=");

                    if (splitResult.length > 1) {
                        poiTypeTags.add(PoiTypeTag.builder()
                                .key(splitResult[0])
                                .value(splitResult[1])
                                .mandatory(true)
                                .poiType(poiType)
                                .ordinal(ordinal++)
                                .build());
                    } else {
                        poiTypeTags.add(PoiTypeTag.builder()
                                .key(tagCombination)
                                .mandatory(false)
                                .poiType(poiType)
                                .ordinal(ordinal++)
                                .build());
                    }
                }
                break;
            }
        }

        // If there was no relevant information in the English wiki, query for tags combinations.
        if (poiTypeTags.size() == 0) {
            Combinations combinations = tagInfoRestClient.getCombinations(key, 1, 5);
            for (CombinationsData data : combinations.getData()) {
                if (tagsGroup.contains(data.getOtherKey())) {
                    poiTypeTags.add(PoiTypeTag.builder()
                            .key(data.getOtherKey())
                            .value(key)
                            .mandatory(true)
                            .poiType(poiType)
                            .ordinal(ordinal++)
                            .build());
                } else {
                    poiTypeTags.add(PoiTypeTag.builder()
                            .key(data.getOtherKey())
                            .mandatory(false)
                            .poiType(poiType)
                            .ordinal(ordinal++)
                            .build());
                }
            }
        }

        poiType.setTags(poiTypeTags);
        return poiType;
    }

    private void displayNewUndoSnackbar(Snackbar snackbar, OnClickListener undoAction, Snackbar.Callback callback) {
        if (snackbar.isShown()) {
            throw new IllegalStateException("Snackbar is already shown");
        }
        snackbar.setAction(R.string.undo, undoAction);
        snackbar.setCallback(callback);

        synchronized (lock) {
            lastSnackBar = snackbar;
            snackbar.show();
        }
    }

    private void onSnackBarDismissed(Snackbar snackbar) {
        synchronized (lock) {
            if (lastSnackBar == snackbar) {
                lastSnackBar = null;
            }
        }
    }

    private class DeleteTaskCallback extends Snackbar.Callback {

        private final DeletionTask task;
        private boolean done;

        public DeleteTaskCallback(DeletionTask task) {
            this.task = task;
        }

        @Override
        public final void onDismissed(Snackbar snackbar, int event) {
            if (!done) {
                done = true;
                if (event != DISMISS_EVENT_ACTION) {
                    bus.post(task);
                }
                onSnackBarDismissed(snackbar);
            }
        }
    }

    private interface DeletionTask {
    }

    private class InternalSavePoiTypeEvent extends BasePoiTypeEvent {
        public InternalSavePoiTypeEvent(PoiType poiType) {
            super(poiType);
        }
    }

    private class InternalSavePoiTagEvent extends BasePoiTagEvent {
        public InternalSavePoiTagEvent(PoiTypeTag poiTypeTag) {
            super(poiTypeTag);
        }
    }

    private class InternalRemovePoiTypeEvent extends BasePoiTypeEvent implements DeletionTask {
        public InternalRemovePoiTypeEvent(PoiType poiType) {
            super(poiType);
        }
    }

    private class InternalRemovePoiTagEvent extends BasePoiTagEvent implements DeletionTask {
        public InternalRemovePoiTagEvent(PoiTypeTag poiTypeTag) {
            super(poiTypeTag);
        }
    }

    private class InternalDownloadTypeSuggestionsEvent {

        private final String query;
        private final Integer page;

        public InternalDownloadTypeSuggestionsEvent(String query, Integer page) {
            this.query = query;
            this.page = page;
        }

        public String getQuery() {
            return query;
        }

        public Integer getPage() {
            return page;
        }
    }
}
