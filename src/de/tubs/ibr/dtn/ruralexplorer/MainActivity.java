
package de.tubs.ibr.dtn.ruralexplorer;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.ruralexplorer.InfoFragment.OnInfoWindowListener;

public class MainActivity extends Activity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		LocationListener,
		OnInfoWindowListener {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	private LocationClient mLocationClient = null;
	private NodeManager mNodeManager = null;
	private Handler mBeaconHandler = null;
	
	private FrameLayout mLayoutDropShadow = null;
	private Boolean mInfoVisible = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// get info window drop shadow
		mLayoutDropShadow = (FrameLayout)findViewById(R.id.info_drop_shadow);
		
		// create a new handler
		mBeaconHandler = new Handler();

		// create a new location client
		mLocationClient = new LocationClient(this, this, this);

		GoogleMap map = ((MapFragment) getFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		// Other supported types include: MAP_TYPE_NORMAL,
		// MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID and MAP_TYPE_NONE
		map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
		
		// enable own location
		map.setMyLocationEnabled(true);
		
		// set listener for clicks on marker
		map.setOnMarkerClickListener(mMarkerListener);
		
		// move camera to zoom level 20
		map.moveCamera(CameraUpdateFactory.zoomTo(20.0f));
		
		// create a new NodeManager
		mNodeManager = new NodeManager(this, map);
	}
	
	@Override
	public void onBackPressed() {
		if (mInfoVisible) {
			// show / hide marker frame
			InfoFragment node = ((InfoFragment) getFragmentManager()
					.findFragmentById(R.id.info));
			
			node.setNode(null);
		} else {
			super.onBackPressed();
		}
	}

	private GoogleMap.OnMarkerClickListener mMarkerListener = new GoogleMap.OnMarkerClickListener() {
		@Override
		public boolean onMarkerClick(Marker marker) {
			// move to the marker
			GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 20));
			
			// show / hide marker frame
			InfoFragment node = ((InfoFragment) getFragmentManager()
					.findFragmentById(R.id.info));
			
			try {
				Node n = mNodeManager.get(marker);
				node.setNode(n);
			} catch (NodeNotFoundException e) {
				node.setNode(null);
			}
			
			return true;
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		mLocationClient.connect();
		mNodeManager.onStart();
	}

	@Override
	protected void onStop() {
		// stop beaconing
		mBeaconHandler.removeCallbacks(mBeaconProcess);
		
		mNodeManager.onStop();
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				Intent i = new Intent(this, SettingsActivity.class);
				startActivity(i);
				return true;
			
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(
						this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			// showErrorDialog(connectionResult.getErrorCode());
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		centerToLocation();
		
		Node n = mNodeManager.get(new SingletonEndpoint("dtn://test1"));
		Location l = mLocationClient.getLastLocation();
		
		if (l != null) {
			l.setLatitude(l.getLatitude() + 0.005);
			n.setLocation(l);
			n.setType(Node.Type.INGA);
			
			n = mNodeManager.get(new SingletonEndpoint("dtn://test2"));
			l = mLocationClient.getLastLocation();
			l.setLatitude(l.getLatitude() - 0.005);
			n.setLocation(l);
			n.setType(Node.Type.PI);
			
			n = mNodeManager.get(new SingletonEndpoint("dtn://test3"));
			l = mLocationClient.getLastLocation();
			l.setLongitude(l.getLongitude() - 0.005);
			n.setLocation(l);
			n.setType(Node.Type.ANDROID);
		}
		
		// start beaconing
		mBeaconHandler.post(mBeaconProcess);
	}
	
	private void centerToLocation() {
		GoogleMap map = ((MapFragment) getFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		
		Location location = mLocationClient.getLastLocation();
		if (location == null) return;
		
		LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 20));
	}

	@Override
	public void onDisconnected() {
		// stop beaconing
		mBeaconHandler.removeCallbacks(mBeaconProcess);
	}

	@Override
	public void onLocationChanged(Location location) {
		// LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
	}

	private Runnable mBeaconProcess = new Runnable() {
		@Override
		public void run() {
			// generate beacon
			Intent intent = new Intent(MainActivity.this, CommService.class);
			intent.setAction(CommService.ACTION_GENERATE_BEACON);
			intent.putExtra(Database.EXTRA_LOCATION, mLocationClient.getLastLocation());
			startService(intent);
			
			// next update in 10 seconds
			mBeaconHandler.postDelayed(mBeaconProcess, 10000);
		}
	};

	@Override
	public void onInfoWindowStateChanged(boolean visible, int height, int width) {
		GoogleMap map = ((MapFragment) getFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		
		if (visible) {
			map.setPadding(0, 0, 0, height);
			mLayoutDropShadow.setVisibility(View.VISIBLE);
			mInfoVisible = true;
		} else {
			map.setPadding(0, 0, 0, 0);
			mLayoutDropShadow.setVisibility(View.GONE);
			mInfoVisible = false;
		}
	}
}
