/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.frodo.pulltorefreshswipelistview;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.frodo.pulltorefreshswipelistview.adapters.PackageAdapter;
import com.frodo.pulltorefreshswipelistview.adapters.PackageItem;
import com.frodo.pulltorefreshswipelistview.library.BaseSwipeListViewListener;
import com.frodo.pulltorefreshswipelistview.library.PullToRefreshBase;
import com.frodo.pulltorefreshswipelistview.library.PullToRefreshBase.Mode;
import com.frodo.pulltorefreshswipelistview.library.PullToRefreshSwipeListView;
import com.frodo.pulltorefreshswipelistview.library.SwipeListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class PullToRefreshSwipeListActivity extends ListActivity {

	static final int MENU_MANUAL_REFRESH = 0;
	static final int MENU_DISABLE_SCROLL = 1;
	static final int MENU_SET_MODE = 2;
	static final int MENU_DEMO = 3;

	private PullToRefreshSwipeListView mPullToRefreshSwipeListView;
	private SwipeListView swipeListView;

	private PackageAdapter adapter;
	private List<PackageItem> data;
	private ProgressDialog progressDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		mPullToRefreshSwipeListView = (PullToRefreshSwipeListView) findViewById(R.id.pull_refresh_list);

		// Set a listener to be invoked when the list should be refreshed.
		mPullToRefreshSwipeListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<SwipeListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<SwipeListView> refreshView) {
				String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

				// Update the LastUpdatedLabel
				refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);

				// Do work to refresh the list here.
				new ListAppTask().execute();
				progressDialog = new ProgressDialog(PullToRefreshSwipeListActivity.this);
				progressDialog.setMessage(getString(R.string.loading));
				progressDialog.setCancelable(false);
				progressDialog.show();
			}
		});

		// Add an end-of-list listener
		mPullToRefreshSwipeListView.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {

			@Override
			public void onLastItemVisible() {
				Toast.makeText(PullToRefreshSwipeListActivity.this, "End of List!", Toast.LENGTH_SHORT).show();
			}
		});

		swipeListView = mPullToRefreshSwipeListView.getRefreshableView();

		// Need to use the Actual ListView when registering for Context Menu
		registerForContextMenu(swipeListView);


		data = new ArrayList<>();

		adapter = new PackageAdapter(this, data);
		swipeListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			swipeListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

				@Override
				public void onItemCheckedStateChanged(ActionMode mode, int position,
													  long id, boolean checked) {
					mode.setTitle("Selected (" + swipeListView.getCountSelected() + ")");
				}

				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					return true;
				}

				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					return true;
				}

				@Override
				public void onDestroyActionMode(ActionMode mode) {
					swipeListView.unselectedChoiceStates();
				}

				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return false;
				}
			});
		}

		swipeListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
			@Override
			public void onOpened(int position, boolean toRight) {
			}

			@Override
			public void onClosed(int position, boolean fromRight) {
			}

			@Override
			public void onListChanged() {
			}

			@Override
			public void onMove(int position, float x) {
			}

			@Override
			public void onStartOpen(int position, int action, boolean right) {
				Log.d("swipe", String.format("onStartOpen %d - action %d", position, action));
			}

			@Override
			public void onStartClose(int position, boolean right) {
				Log.d("swipe", String.format("onStartClose %d", position));
			}

			@Override
			public void onClickFrontView(int position) {
				Log.d("swipe", String.format("onClickFrontView %d", position));
			}

			@Override
			public void onClickBackView(int position) {
				Log.d("swipe", String.format("onClickBackView %d", position));
			}

			@Override
			public void onDismiss(int[] reverseSortedPositions) {
				for (int position : reverseSortedPositions) {
					data.remove(position);
				}
				adapter.notifyDataSetChanged();
			}

		});

		swipeListView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_MANUAL_REFRESH, 0, "Manual Refresh");
		menu.add(0, MENU_DISABLE_SCROLL, 1,
				mPullToRefreshSwipeListView.isScrollingWhileRefreshingEnabled() ? "Disable Scrolling while Refreshing"
						: "Enable Scrolling while Refreshing");
		menu.add(0, MENU_SET_MODE, 0, mPullToRefreshSwipeListView.getMode() == PullToRefreshBase.Mode.BOTH ? "Change to MODE_PULL_DOWN"
				: "Change to MODE_PULL_BOTH");
		menu.add(0, MENU_DEMO, 0, "Demo");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle("Item: " + getListView().getItemAtPosition(info.position));
		menu.add("Item 1");
		menu.add("Item 2");
		menu.add("Item 3");
		menu.add("Item 4");

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem disableItem = menu.findItem(MENU_DISABLE_SCROLL);
		disableItem
				.setTitle(mPullToRefreshSwipeListView.isScrollingWhileRefreshingEnabled() ? "Disable Scrolling while Refreshing"
						: "Enable Scrolling while Refreshing");

		MenuItem setModeItem = menu.findItem(MENU_SET_MODE);
		setModeItem.setTitle(mPullToRefreshSwipeListView.getMode() == PullToRefreshBase.Mode.BOTH ? "Change to MODE_FROM_START"
				: "Change to MODE_PULL_BOTH");

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case MENU_MANUAL_REFRESH:
				new ListAppTask().execute();
				progressDialog = new ProgressDialog(this);
				progressDialog.setMessage(getString(R.string.loading));
				progressDialog.setCancelable(false);
				progressDialog.show();
				mPullToRefreshSwipeListView.setRefreshing(false);
				break;
			case MENU_DISABLE_SCROLL:
				mPullToRefreshSwipeListView.setScrollingWhileRefreshingEnabled(!mPullToRefreshSwipeListView
						.isScrollingWhileRefreshingEnabled());
				break;
			case MENU_SET_MODE:
				mPullToRefreshSwipeListView.setMode(mPullToRefreshSwipeListView.getMode() == Mode.BOTH ? Mode.PULL_FROM_START
						: Mode.BOTH);
				break;
			case MENU_DEMO:
				mPullToRefreshSwipeListView.demo();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	public class ListAppTask extends AsyncTask<Void, Void, List<PackageItem>> {

		protected List<PackageItem> doInBackground(Void... args) {
			PackageManager appInfo = getPackageManager();
			List<ApplicationInfo> listInfo = appInfo.getInstalledApplications(0);
			Collections.sort(listInfo, new ApplicationInfo.DisplayNameComparator(appInfo));

			List<PackageItem> data = new ArrayList<>();

			for (int index = 0; index < listInfo.size(); index++) {
				try {
					ApplicationInfo content = listInfo.get(index);
					if ((content.flags != ApplicationInfo.FLAG_SYSTEM) && content.enabled) {
						if (content.icon != 0) {
							PackageItem item = new PackageItem();
							item.setName(getPackageManager().getApplicationLabel(content).toString());
							item.setPackageName(content.packageName);
							item.setIcon(getPackageManager().getDrawable(content.packageName, content.icon, content));
							data.add(item);
						}
					}
				} catch (Exception e) {

				}
			}

			return data;
		}

		protected void onPostExecute(List<PackageItem> result) {
			data.clear();
			data.addAll(result);
			adapter.notifyDataSetChanged();
			if (progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}

			// Call onRefreshComplete when the list has been refreshed.
			mPullToRefreshSwipeListView.onRefreshComplete();

			super.onPostExecute(result);
		}
	}

}
