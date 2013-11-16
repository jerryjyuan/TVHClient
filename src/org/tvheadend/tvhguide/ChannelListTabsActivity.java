/*
 *  Copyright (C) 2013 Robert Siebert
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhguide;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ChannelListTabsActivity extends ActionBarActivity {

    private ActionBar actionBar = null;
    private boolean reconnect = false;
    private int prevTabPosition = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Apply the specified theme
        setTheme(Utils.getThemeId(this));

        super.onCreate(savedInstanceState);
        
        DatabaseHelper.init(this.getApplicationContext()); 
        
        // setup action bar for tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayUseLogoEnabled(Utils.showChannelIcons(this));

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                handleTabSelection(tab, ft);
            }

            @Override
            public void onTabReselected(Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                // Save the position of the tab that has been unselected.
                prevTabPosition = tab.getPosition();
                
                // Detach the channel list fragment, because another will be attached
                Fragment prevFragment = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
                if (prevFragment != null) {
                    ft.detach(prevFragment);
                }
            }
        };

        // Add the tabs
        Tab tab = actionBar.newTab().setText(R.string.channels).setTabListener(tabListener);
        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.recordings).setTabListener(tabListener);
        actionBar.addTab(tab);
//        tab = actionBar.newTab().setText(R.string.program_guide).setTabListener(tabListener);
//        actionBar.addTab(tab);
        tab = actionBar.newTab().setText(R.string.status).setTabListener(tabListener);
        actionBar.addTab(tab);
        
        // Restore the previously selected tab
        if (savedInstanceState != null) {
            int index = savedInstanceState.getInt("selected_channel_tab_index", 0);
            actionBar.setSelectedNavigationItem(index);
        }
    }

    /**
     * 
     * @param tab
     * @param ft
     */
    protected void handleTabSelection(Tab tab, FragmentTransaction ft) {
        switch (tab.getPosition()) {
        case 0:
            // Checks if the fragment is already initialized.
            // If not, it will be instantiated and added to the
            // activity. If it exists, it will simply attached to show it.
            Fragment currentFrag = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
            if (currentFrag == null) {
                Fragment fragment = Fragment.instantiate(this, ChannelListFragment.class.getName());
                ft.add(android.R.id.content, fragment, tab.getText().toString());
            }
            else {
                ft.attach(currentFrag);
            }
            break;
        case 1:
            // Show the list of recordings
            Intent intent = new Intent(this, RecordingListTabsActivity.class);
            startActivity(intent);
            break;
//        case 2:
//            // Show the program guide
//            Toast.makeText(this, "No implemented yet", Toast.LENGTH_SHORT).show();
//            break;
        case 2:
        	Fragment statusFrag = getSupportFragmentManager().findFragmentByTag(tab.getText().toString());
            if (statusFrag == null) {
                Fragment fragment = Fragment.instantiate(this, StatusFragment.class.getName());
                ft.add(android.R.id.content, fragment, tab.getText().toString());
            }
            else {
                ft.attach(statusFrag);
            }
            break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // If the user has pressed the back button, the currently selected tab
        // would be active (like the recordings tab) which would show nothing
        // here. So we set the previously selected tab.
        if (prevTabPosition >= 0 &&
                actionBar.getSelectedNavigationIndex() == 1) {
            actionBar.setSelectedNavigationItem(prevTabPosition);
            prevTabPosition = -1;
        }
        
        Utils.connect(this, reconnect);
        reconnect = false;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the currently selected tab
        int index = actionBar.getSelectedNavigationIndex();
        outState.putInt("selected_channel_tab_index", index);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Disable the refresh menu if no connection is available
        MenuItem item = menu.findItem(R.id.menu_refresh);
        if (item != null)
            item.setVisible(DatabaseHelper.getInstance().getSelectedConnection() != null);
       
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
        case R.id.menu_settings:
            // Save the current tab position so we show the previous tab
            // again when we return from the settings menu.
            prevTabPosition = actionBar.getSelectedNavigationIndex();
            // Now start the settings activity
            intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, R.id.menu_settings);
            return true;

        case R.id.menu_refresh:
            Utils.connect(this, true);
            return true;

        case R.id.menu_connections:
            // Save the current tab position so we show the previous tab
            // again when we return from the settings menu.
            prevTabPosition = actionBar.getSelectedNavigationIndex();
            // Show the manage connections activity where
            // the user can choose a connection
            intent = new Intent(this, SettingsManageConnectionsActivity.class);
            startActivityForResult(intent, R.id.menu_connections);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == R.id.menu_connections) {
            if (resultCode == RESULT_OK){
                reconnect = data.getBooleanExtra("reconnect", false);
            }
        }
    }

    public void setActionBarTitle(final String title) {
        actionBar.setTitle(title);
    }

    public void setActionBarSubtitle(final String subtitle) {
        actionBar.setSubtitle(subtitle);
    }
}
