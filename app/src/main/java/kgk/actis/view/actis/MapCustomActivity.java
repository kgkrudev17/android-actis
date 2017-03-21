package kgk.actis.view.actis;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import kgk.actis.R;
import kgk.actis.dispatcher.Dispatcher;
import kgk.actis.map.Map;
import kgk.actis.map.MapClickListener;
import kgk.actis.map.MapManager;
import kgk.actis.map.MarkerClickListener;
import kgk.actis.map.event.MapChangeEvent;
import kgk.actis.map.event.MapReadyForUseEvent;
import kgk.actis.model.Signal;
import kgk.actis.stores.ActisStore;
import kgk.actis.view.actis.event.CenterMapEvent;

/**
 * Экран с полноразмерной картой
 */
public class MapCustomActivity extends AppCompatActivity implements MapClickListener,
                                                                    MarkerClickListener {

    private static final String TAG = MapCustomActivity.class.getSimpleName();

    public static final String EXTRA_SIGNAL = "extra_signal";

    private Dispatcher dispatcher;
    private ActisStore actisStore;

    private Signal signal;
    private Map map;

    @Bind(R.id.batteryView) TextView batteryView;
    @Bind(R.id.helpToolbarButton) ImageButton helpToolbarButton;
    @Bind(R.id.toolbar_title) TextView toolbarTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        ButterKnife.bind(this);
        prepareToolbar();
        initFluxDependencies();
        setFragment(new MapManager().loadPreferredMapFragmentForActis());
    }

    @Override
    public void onResume() {
        super.onResume();
        dispatcher.register(this);
        updateToolbarContent();
    }

    @Override
    public void onPause() {
        super.onPause();
        dispatcher.unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.actisApp_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.actis_navigation_menu_icon));
        toolbarTitle.setText(getString(R.string.map_custom_action_bar_label));
        helpToolbarButton = (ImageButton) findViewById(R.id.helpToolbarButton);
    }

    private void initFluxDependencies() {
        dispatcher = Dispatcher.getInstance(EventBus.getDefault());
        actisStore = ActisStore.getInstance(dispatcher);
    }

    private void setFragment(Fragment fragmentToSet) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, fragmentToSet)
                    .commit();
        }
    }

    private void updateToolbarContent() {
        if (batteryView != null) {
            batteryView.setVisibility(View.VISIBLE);

            int charge = actisStore.getSignal().getCharge();

            batteryView.setBackgroundDrawable(getResources().getDrawable(R.drawable.battery_icon_general));
            batteryView.setTextColor(getResources().getColor(android.R.color.white));

//            if (charge >= 70) {
//                batteryView.setBackgroundDrawable(getResources().getDrawable(R.drawable.battery_icon_high));
//                batteryView.setTextColor(getResources().getColor(R.color.actis_app_green_accent));
//            } else if (charge >= 35 && charge < 70) {
//                batteryView.setBackgroundDrawable(getResources().getDrawable(R.drawable.battery_icon_average));
//                batteryView.setTextColor(getResources().getColor(R.color.actis_app_yellow_accent));
//            } else {
//                batteryView.setBackgroundDrawable(getResources().getDrawable(R.drawable.battery_icon_low));
//                batteryView.setTextColor(getResources().getColor(R.color.actis_app_red_accent));
//            }

            batteryView.setText(charge + "%");
        }
    }

    ////

    @Override
    public void onMapClick(double latitude, double longitude) {
        map.clear();
        map.addCustomMarkerPoint(signal.getDirection(), signal.getLatitude(), signal.getLongitude(), Map.STANDARD_MARKER_TYPE);
    }

    @Override
    public void onMarkerClick(double latitude, double longitude) {
        // map.clear();
        map.addSignalInfoMarker(signal);
    }

    ////

    public void onEvent(MapReadyForUseEvent event) {
        map = event.getMap();
        map.setOnMapClickListener(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.signal = extras.getParcelable(EXTRA_SIGNAL);
        } else {
            this.signal = actisStore.getSignal();
        }

        assert signal != null;
        map.addCustomMarkerPoint(signal.getDirection(), signal.getLatitude(), signal.getLongitude(), Map.STANDARD_MARKER_TYPE);

        map.moveCamera(signal.getLatitude(), signal.getLongitude(), 13);

        if (signal.getSatellites() == 0) {
            map.addCircleZone(signal.getLatitude(), signal.getLongitude(), Map.DEFAULT_CIRCLE_ZONE_RADIUS);
        }

//        LatLng cameraCoordinates = MapManager.getCameraCoordinates();
//        int cameraZoom = MapManager.getCameraZoom();
//        if (cameraCoordinates != null && cameraZoom != 0) {
//            map.moveCamera(cameraCoordinates.latitude, cameraCoordinates.longitude, cameraZoom);
//        } else {
//            map.moveCamera(signal.getLatitude(), signal.getLongitude(), 13);
//        }

        map.setOnMarkerClickListener(this);
    }

    public void onEvent(MapChangeEvent event) {
        MapManager mapManager = new MapManager();
        mapManager.savePreferredMap(event.getPreferredMap());

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment mapFragment = fragmentManager
                .findFragmentById(R.id.fragmentContainer);

        mapFragment = new MapManager().loadPreferredMapFragmentForActis();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, mapFragment)
                .commit();
    }

    public void onEventMainThread(CenterMapEvent event) {
        map.moveCamera(signal.getLatitude(), signal.getLongitude(), map.getCurrentZoom());
    }

    ////

    @OnClick(R.id.helpToolbarButton)
    public void onClickHelpToolbarButton(View view) {
        Intent helpScreenIntent = new Intent(this, HelpActivity.class);
        helpScreenIntent.putExtra(HelpActivity.KEY_SCREEN_NAME, HelpActivity.MAP_SCREEN);
        startActivity(helpScreenIntent);
    }
}
