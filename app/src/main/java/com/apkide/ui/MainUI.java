package com.apkide.ui;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode;
import static com.apkide.ui.views.SplitLayout.OnSplitChangeListener;
import static java.lang.System.currentTimeMillis;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.apkide.common.command.Command;
import com.apkide.ui.browsers.BrowserPager;
import com.apkide.ui.databinding.UiMainBinding;
import com.apkide.ui.editors.EditorPager;
import com.apkide.ui.util.MenuCommand;
import com.apkide.ui.views.SplitLayout;
import com.google.android.material.tabs.TabLayout;

import java.util.HashMap;
import java.util.List;


public class MainUI extends StyledUI implements
		OnSharedPreferenceChangeListener,
		OnSplitChangeListener{
	private SharedPreferences myPreferences;
	private UiMainBinding myUiBinding;
	private MainUIViewModel myUIViewModel;
	private long myLastBackPressedTimestamps;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		App.init(this);
		super.onCreate(savedInstanceState);
		AppPreferences.registerListener(this);
		myUIViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(MainUIViewModel.class);
		myUiBinding = UiMainBinding.inflate(getLayoutInflater());
		setContentView(myUiBinding.getRoot());
		setSupportActionBar(myUiBinding.mainToolbar);

		getSplitLayout().setOnSplitChangeListener(this);
		myUiBinding.mainMoreButton.setOnClickListener(view -> {

			getSplitLayout().toggleSplit(() -> {

			});
		});

		myUiBinding.mainOpenFileButton.setOnClickListener(view -> {
			App.postRun(() -> {
				boolean projectOpen = App.getProjectService().isProjectOpened();
				if (!getSplitLayout().isSplit())
					getSplitLayout().openSplit();
				if (getBrowserPager().getIndex() != (projectOpen ?
						BrowserPager.PROJECT_BROWSER :
						BrowserPager.FILE_BROWSER)) {
					getBrowserPager().toggle(projectOpen ?
							BrowserPager.PROJECT_BROWSER :
							BrowserPager.FILE_BROWSER);
				}
			}, 100L);

		});
		
		getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (getSplitLayout().isSplit()) {
					getSplitLayout().closeSplit();
					return;
				}

				if (currentTimeMillis() - myLastBackPressedTimestamps < 2000) {
					shutdown();
				} else {
					myLastBackPressedTimestamps = currentTimeMillis();
					Toast.makeText(MainUI.this, "Press Exit again...", Toast.LENGTH_SHORT).show();
				}
			}
		});
		getEditorTabLayout().setupWithViewPager(getEditorPager(),true);
		boolean isSplit = getPreferences().getBoolean("isSplit", false);
		if (isSplit)
			getSplitLayout().openSplit();
		requestStorage();
	}

	public void shutdown() {
		finish();
	}
	
	
	public FrameLayout getEmptyFrame(){
		return myUiBinding.mainEmptyFrame;
	}
	
	public EditorPager getEditorPager(){
		return myUiBinding.mainEditorPager;
	}
	public TabLayout getEditorTabLayout(){
		return myUiBinding.mainEditorTabLayout;
	}

	public SplitLayout getSplitLayout() {
		return myUiBinding.mainSplitLayout;
	}

	public BrowserPager getBrowserPager() {
		return myUiBinding.mainBrowserPager;
	}

	private SharedPreferences getPreferences() {
		if (myPreferences == null)
			myPreferences = App.getPreferences("MainUI");
		return myPreferences;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		App.shutdown();
		AppPreferences.unregisterListener(this);
	}
	
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
		if (key == null) return;
		boolean recreated=false;
		
		if (key.startsWith("app.theme")){
			if (AppPreferences.isFollowSystemTheme()) {
				if (AppCompatDelegate.getDefaultNightMode() != MODE_NIGHT_FOLLOW_SYSTEM) {
					AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM);
				}else {
					setDefaultNightMode(AppPreferences.isNightTheme() ? MODE_NIGHT_YES : MODE_NIGHT_NO);
				}
			} else {
				setDefaultNightMode(AppPreferences.isNightTheme() ? MODE_NIGHT_YES : MODE_NIGHT_NO);
			}
			recreated=true;
		}
		
		if (recreated){
			recreate();
		}
	}

	@Override
	public void onSplitChanged(boolean isSplit) {
		myUiBinding.mainMoreButton.setVisibility(isSplit ? View.GONE : View.VISIBLE);
		getPreferences().edit().putBoolean("isSplit", isSplit).apply();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.ui_main_options, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		applyMenuCommand(menu);
		return super.onPrepareOptionsMenu(menu);
	}

	private final HashMap<Integer, MenuCommand> myCachedCommands = new HashMap<>(50);

	private void applyMenuCommand(Menu menu) {
		if (App.isShutdown()) return;

		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			MenuCommand cached;
			if (myCachedCommands.containsKey(item.getItemId()) &&
					(cached = myCachedCommands.get(item.getItemId())) != null) {

				item.setVisible(cached.isVisible());
				item.setEnabled(cached.isEnabled());
				if (cached.getTitle() != null)
					item.setTitle(cached.getTitle());
				if (cached.getIcon() != 0)
					item.setIcon(cached.getIcon());
			} else {
				List<Command> commands = AppCommands.getCommands();
				for (Command command : commands) {
					if (command instanceof MenuCommand &&
							item.getItemId() == ((MenuCommand) command).getId()) {

						myCachedCommands.put(item.getItemId(), (MenuCommand) command);
						item.setVisible(((MenuCommand) command).isVisible());
						item.setEnabled(command.isEnabled());
						if (((MenuCommand) command).getTitle() != null)
							item.setTitle(((MenuCommand) command).getTitle());
						if (((MenuCommand) command).getIcon() != 0)
							item.setIcon(((MenuCommand) command).getIcon());
					}
				}
			}

			if (item.hasSubMenu()) {
				if (item.getSubMenu() != null)
					applyMenuCommand(item.getSubMenu());
			}
		}

	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (myCachedCommands.containsKey(item.getItemId())) {
			MenuCommand command = myCachedCommands.get(item.getItemId());
			if (command != null && command.isEnabled()) {
				command.run();
				return true;
			}
		}
		List<Command> commands = AppCommands.getCommands();
		for (Command command : commands) {
			if (command instanceof MenuCommand && ((MenuCommand) command).getId() == item.getItemId()) {
				if (command.isEnabled()) {
					command.run();
					return true;
				}
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
}
