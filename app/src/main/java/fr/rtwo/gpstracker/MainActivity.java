package fr.rtwo.gpstracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActionBarDrawerToggle mToggle;
    private FragmentManager mFragmentManager;

    private NavigationView.OnNavigationItemSelectedListener
            mNavItemListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

            return true;
        }
    };

    private static final int PERMISSIONS_REQUEST_ID = 0;
    private boolean mHasPermissions;

    private List<String> mPermissionList = Arrays.asList(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE);

    public boolean getHasPermissions() {
        return mHasPermissions;
    }

    private boolean checkPermissions() {
        boolean ret;
        List<String> missingPermissions = new ArrayList<String>();

        for (String permission: mPermissionList) {
            int granted = ContextCompat.checkSelfPermission(this, permission);

            if (granted != PackageManager.PERMISSION_GRANTED)
                missingPermissions.add(permission);
        }

        if (missingPermissions.size() > 0) {
            String[] permissionsArray = new String[missingPermissions.size()];

            ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toArray(permissionsArray),
                    PERMISSIONS_REQUEST_ID);

            ret = false;
        } else {
            ret = true;
        }

        return ret;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        for (int granted: grantResults) {
            if (granted != PackageManager.PERMISSION_GRANTED)
                return;
        }

        mHasPermissions = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Check permissions
        mHasPermissions = checkPermissions();

        // Attach toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Attach navigation
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        mToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navDrawerOpen, R.string.navDrawerClose);

        drawer.addDrawerListener(mToggle);
        mToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(mNavItemListener);

        // Create default fragment
        if (savedInstanceState == null) {
            mFragmentManager = getSupportFragmentManager();

            Fragment fragment = new FragmentRecord();
            mFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
