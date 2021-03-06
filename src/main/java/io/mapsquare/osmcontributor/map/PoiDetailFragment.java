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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.mapsquare.osmcontributor.OsmTemplateApplication;
import io.mapsquare.osmcontributor.R;
import io.mapsquare.osmcontributor.core.ConfigManager;
import io.mapsquare.osmcontributor.map.events.PleaseChangePoiPosition;
import io.mapsquare.osmcontributor.map.events.PleaseChangeValuesDetailPoiFragmentEvent;
import io.mapsquare.osmcontributor.map.events.PleaseDeletePoiFromMapEvent;
import io.mapsquare.osmcontributor.map.events.PleaseOpenEditionEvent;


public class PoiDetailFragment extends Fragment {

    @Inject
    EventBus eventBus;

    @Inject
    ConfigManager configManager;

    @BindView(R.id.poi_name)
    TextView editTextPoiName;

    @BindView(R.id.poi_type_name)
    TextView editTextPoiTypeName;

    @BindView(R.id.floating_action_menu)
    FloatingActionsMenu floatingActionMenu;

    @BindView(R.id.edit_poi_detail)
    FloatingActionButton floatingButtonEditPoi;

    @BindView(R.id.edit_poi_position)
    FloatingActionButton floatingButtonEditPosition;

    public PoiDetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_poi_detail, container, false);

        ((OsmTemplateApplication) getActivity().getApplication()).getOsmTemplateComponent().inject(this);
        ButterKnife.bind(this, rootView);

        if (!configManager.hasPoiModification()) {
            floatingButtonEditPoi.setIcon(R.drawable.eye);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        eventBus.register(this);
    }

    @Override
    public void onPause() {
        eventBus.unregister(this);
        super.onPause();
    }

    @OnClick(R.id.edit_poi_detail)
    public void editPoiOnClick() {
        eventBus.post(new PleaseOpenEditionEvent());
    }

    @OnClick(R.id.edit_poi_position)
    public void editPoiPositionOnClick() {
        eventBus.post(new PleaseChangePoiPosition());
    }

    @OnClick(R.id.delete_poi)
    public void deletePoiOnClick() {
        if (configManager.hasPoiModification()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

            alertDialog.setTitle(R.string.delete_poi_title);
            alertDialog.setMessage(R.string.delete_poi_confirm_message);
            alertDialog.setPositiveButton(R.string.delete_poi_positive_btn, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    eventBus.post(new PleaseDeletePoiFromMapEvent());
                }
            });

            alertDialog.setNegativeButton(R.string.delete_poi_negative_btn, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            alertDialog.show();
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.point_modification_forbidden), Toast.LENGTH_SHORT).show();
        }
    }

    private void setPoiName(String poiName) {
        floatingActionMenu.collapse();

        if (poiName != null && !poiName.isEmpty()) {
            editTextPoiName.setText(poiName);
            editTextPoiName.setTextColor(getResources().getColor(R.color.active_text));
        } else {
            editTextPoiName.setText(getResources().getString(R.string.no_poi_name));
            editTextPoiName.setTextColor(getResources().getColor(R.color.disable_text));
        }
    }

    private void setPoiType(String poiTypeName) {
        if (poiTypeName != null && !poiTypeName.isEmpty()) {
            editTextPoiTypeName.setText(poiTypeName);
            editTextPoiTypeName.setTextColor(getResources().getColor(R.color.active_text));
        } else {
            editTextPoiTypeName.setText(getResources().getString(R.string.no_poi_name));
            editTextPoiTypeName.setTextColor(getResources().getColor(R.color.disable_text));
        }
    }

    private void showMovePoi(boolean showing) {
        floatingButtonEditPosition.setVisibility(showing ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPleaseChangeValuesDetailPoiFragmentEvent(PleaseChangeValuesDetailPoiFragmentEvent event) {
        setPoiType(event.getPoiType());
        setPoiName(event.getPoiName());
        showMovePoi(!event.isWay());
    }
}
