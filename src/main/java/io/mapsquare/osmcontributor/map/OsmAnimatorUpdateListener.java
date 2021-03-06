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
package io.mapsquare.osmcontributor.map;

import android.animation.ValueAnimator;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

public class OsmAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
    public static final int STEPS_CENTER_ANIMATION = 150;

    final LatLng originPos;
    final LatLng destinationPos;
    final Double latStep;
    final Double lngStep;
    MapView mapView;

    public OsmAnimatorUpdateListener(LatLng originPos, LatLng destinationPos, MapView mapView) {
        this.originPos = originPos;
        this.destinationPos = destinationPos;
        this.mapView = mapView;
        latStep = (destinationPos.getLatitude() - originPos.getLatitude()) / STEPS_CENTER_ANIMATION;
        lngStep = (destinationPos.getLongitude() - originPos.getLongitude()) / STEPS_CENTER_ANIMATION;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        float animatedValue = (Float) valueAnimator.getAnimatedValue();
        Double lat = originPos.getLatitude() + latStep * animatedValue;
        Double lng = originPos.getLongitude() + lngStep * animatedValue;

        mapView.setCenter(new LatLng(lat, lng));
    }
}
