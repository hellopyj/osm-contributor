/**
 * Copyright (C) 2016 eBusiness Information
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
package io.mapsquare.osmcontributor.core;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;

import javax.inject.Singleton;

import io.mapsquare.osmcontributor.BuildConfig;

@Singleton
public class StoreConfigManager implements ConfigManager {

    public StoreConfigManager() {
    }

    @Override
    public int getZoomVectorial() {
        return BuildConfig.ZOOM_VECTORIAL;
    }

    @Override
    public int getDefaultZoom() {
        return BuildConfig.DEFAULT_ZOOM;
    }

    @Override
    public int getZoomMaxProvider() {
        return BuildConfig.ZOOM_MAX_PROVIDER;
    }

    @Override
    public float getZoomMax() {
        return BuildConfig.ZOOM_MAX;
    }

    @Override
    public boolean hasPoiModification() {
        return true;
    }

    @Override
    public boolean hasPoiAddition() {
        return true;
    }

    @Override
    public boolean hasBounds() {
        return false;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public LatLng getDefaultCenter() {
        return new LatLng(BuildConfig.CENTER_LAT, BuildConfig.CENTER_LNG);
    }

    @Override
    public String getMapUrl() {
        return BuildConfig.MAP_URL;
    }

    @Override
    public String getBasePoiApiUrl() {
        return BuildConfig.BASE_OSM_URL;
    }

    @Override
    public String getBaseOverpassApiUrl() {
        return BuildConfig.BASE_OVERPASS_URL;
    }

    @Override
    public String getBingApiKey() {
        return BuildConfig.BING_API_KEY;
    }
}
