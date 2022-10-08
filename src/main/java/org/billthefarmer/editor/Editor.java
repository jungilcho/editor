////////////////////////////////////////////////////////////////////////////////
//
//  Editor - Text editor for Android
//
//  Copyright © 2017  Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
////////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.editor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import android.support.v4.content.FileProvider;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.ref.WeakReference;

import java.nio.charset.Charset;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Editor extends Activity
{
    public final static String TAG = "Editor";

    private Uri uri;
    private File file;
    private String path;
    private Uri content;
    private String match;
    private EditText textView;
    private TextView customView;
    private MenuItem searchItem;
    private SearchView searchView;
    private ScrollView scrollView;
    private Runnable updateHighlight;
    private Runnable updateWordCount;

    private ScaleGestureDetector scaleDetector;

    private Map<String, Integer> pathMap;
    private List<String> removeList;

    private boolean highlight = false;

    private boolean save = false;
    private boolean edit = false;
    private boolean view = false;

    private boolean wrap = false;
    private boolean suggest = true;

    private boolean changed = false;

    private long modified;

    private int theme = Constants.LIGHT;
    private int size = Constants.MEDIUM;
    private int type = Constants.MONO;

    private int syntax;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        getPreferenceValues(preferences);

        Set<String> pathSet = preferences.getStringSet(Constants.PREF_PATHS, null);
        pathMap = new HashMap<>();

        if (pathSet != null)
            for (String path : pathSet)
                pathMap.put(path, preferences.getInt(path, 0));

        removeList = new ArrayList<>();

        setTheme(getThemeId(theme));
        setContentView(getLayoutType());

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.vscroll);

        getActionBar().setSubtitle(match);
        getActionBar().setCustomView(R.layout.custom);
        getActionBar().setDisplayShowCustomEnabled(true);
        customView = (TextView) getActionBar().getCustomView();

        updateWordCount = this::wordCountText;

        if (savedInstanceState != null)
            edit = savedInstanceState.getBoolean(Constants.EDIT);

        setInputType();

        setSizeAndTypeface(size, type);

        openFile(savedInstanceState);

        setListeners();
    }

    private int getLayoutType() {
        return (wrap)? R.layout.wrap : R.layout.edit;
    }

    private void getPreferenceValues(SharedPreferences preferences) {
        save = preferences.getBoolean(Constants.PREF_SAVE, false);
        view = preferences.getBoolean(Constants.PREF_VIEW, true);
        wrap = preferences.getBoolean(Constants.PREF_WRAP, false);
        suggest = preferences.getBoolean(Constants.PREF_SUGGEST, true);
        highlight = preferences.getBoolean(Constants.PREF_HIGHLIGHT, false);

        theme = preferences.getInt(Constants.PREF_THEME, Constants.LIGHT);
        size = preferences.getInt(Constants.PREF_SIZE, Constants.MEDIUM);
        type = preferences.getInt(Constants.PREF_TYPE, Constants.MONO);
    }

    private void setInputType() {
        if (!edit)
            textView.setRawInputType(InputType.TYPE_NULL);

        else if (!suggest)
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    private void openFile(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Uri uri = intent.getData();

        switch (intent.getAction())
        {
        case Intent.ACTION_EDIT:
        case Intent.ACTION_VIEW:
            if ((savedInstanceState == null) && (uri != null))
                readFile(uri);

            getActionBar().setDisplayHomeAsUpEnabled(true);
            break;

        case Intent.ACTION_SEND:
            if (savedInstanceState == null)
            {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (uri != null)
                    readFile(uri);

                else if (text != null)
                {
                    newFile(text);
                    changed = true;
                }

                else
                    defaultFile();
            }
            break;

        case Constants.OPEN_NEW:
            if (savedInstanceState == null)
            {
                newFile();
                textView.postDelayed(() -> editClicked(null), Constants.UPDATE_DELAY);
            }
            break;

        case Intent.ACTION_MAIN:
            if (savedInstanceState == null)
                defaultFile();
            break;
        }
    }

    private int getThemeId(int theme)
    {
        if (theme == Constants.LIGHT) {
            return R.style.AppTheme;
        } else if (theme == Constants.DARK) {
            return R.style.AppDarkTheme;
        } else if (theme == Constants.BLACK) {
            return R.style.AppBlackTheme;
        } else if (theme == Constants.RETRO) {
            return R.style.AppRetroTheme;
        }
        return R.style.AppTheme;
    }

    private void setListeners()
    {
        scaleDetector =
            new ScaleGestureDetector(this, new ScaleListener());

        if (textView != null)
        {
            textView.addTextChangedListener(new TextWatcher()
            {
                // afterTextChanged
                @Override
                public void afterTextChanged(Editable s)
                {
                    if (!changed)
                    {
                        changed = true;
                        invalidateOptionsMenu();
                    }

                    if (updateHighlight != null)
                    {
                        textView.removeCallbacks(updateHighlight);
                        textView.postDelayed(updateHighlight, Constants.UPDATE_DELAY);
                    }

                    if (updateWordCount != null)
                    {
                        textView.removeCallbacks(updateWordCount);
                        textView.postDelayed(updateWordCount, Constants.UPDATE_DELAY);
                    }
                }

                // beforeTextChanged
                @Override
                public void beforeTextChanged(CharSequence s,
                                              int start,
                                              int count,
                                              int after)
                {
                    if (searchItem != null &&
                        searchItem.isActionViewExpanded())
                    {
                        final CharSequence query = searchView.getQuery();

                        textView.postDelayed(() ->
                        {
                            if (searchItem != null &&
                                searchItem.isActionViewExpanded())
                            {
                                if (query != null)
                                    searchView.setQuery(query, false);
                            }
                        }, Constants.UPDATE_DELAY);
                    }
                }

                // onTextChanged
                @Override
                public void onTextChanged(CharSequence s,
                                          int start,
                                          int before,
                                          int count) {}
            });

            // onFocusChange
            textView.setOnFocusChangeListener((v, hasFocus) ->
            {
                // Hide keyboard
                InputMethodManager manager = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
                if (!hasFocus)
                    manager.hideSoftInputFromWindow(v.getWindowToken(), 0);

                if (updateHighlight != null)
                {
                    textView.removeCallbacks(updateHighlight);
                    textView.postDelayed(updateHighlight, Constants.UPDATE_DELAY);
                }
            });

            // onLongClick
            textView.setOnLongClickListener(v ->
            {
                // Do nothing if already editable
                if (edit)
                    return false;

                // Get scroll position
                int y = scrollView.getScrollY();
                // Get height
                int height = scrollView.getHeight();
                // Get width
                int width = scrollView.getWidth();

                // Get offset
                int line = textView.getLayout()
                    .getLineForVertical(y + height / 2);
                int offset = textView.getLayout()
                    .getOffsetForHorizontal(line, width / 2);
                // Set cursor
                textView.setSelection(offset);

                // Set editable with or without suggestions
                if (suggest)
                    textView
                    .setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                else
                    textView
                    .setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                // Change size and typeface temporarily as workaround for yet
                // another obscure feature of some versions of android
                textView.setTextSize((size == Constants.TINY)? Constants.HUGE: Constants.TINY);
                textView.setTextSize(size);
                textView.setTypeface((type == Constants.NORMAL)?
                                     Typeface.MONOSPACE:
                                     Typeface.DEFAULT, Typeface.NORMAL);
                textView.setTypeface((type == Constants.NORMAL)?
                                     Typeface.DEFAULT:
                                     Typeface.MONOSPACE, Typeface.NORMAL);
                // Update boolean
                edit = true;

                // Restart
                recreate(this);

                return false;
            });

            textView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener()
            {
                private boolean keyboard;

                // onGlobalLayout
                @Override
                public void onGlobalLayout()
                {
                    if (updateHighlight != null)
                    {
                        int rootHeight = scrollView.getRootView().getHeight();
                        int height = scrollView.getHeight();

                        boolean shown = (((rootHeight - height) /
                                         (double) rootHeight) >
                                         Constants.KEYBOARD_RATIO);

                        if (shown != keyboard)
                        {
                            if (!shown)
                            {
                                textView.removeCallbacks(updateHighlight);
                                textView.postDelayed(updateHighlight,
                                                     Constants.UPDATE_DELAY);
                            }

                            keyboard = shown;
                        }
                    }
                }
            });
        }

        if (scrollView != null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                // onScrollChange
                scrollView.setOnScrollChangeListener((v, x, y, oldX, oldY) ->
                {
                    if (updateHighlight != null)
                    {
                        textView.removeCallbacks(updateHighlight);
                        textView.postDelayed(updateHighlight, Constants.UPDATE_DELAY);
                    }
                });

            else
                // onScrollChange
                scrollView.getViewTreeObserver()
                    .addOnScrollChangedListener(() ->
                {
                    if (updateHighlight != null)
                    {
                        textView.removeCallbacks(updateHighlight);
                        textView.postDelayed(updateHighlight, Constants.UPDATE_DELAY);
                    }
                });
        }
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        getSavedInstanceValues(savedInstanceState);
        invalidateOptionsMenu();

        file = new File(path);
        uri = Uri.fromFile(file);

        if (content != null)
            setTitle(FileUtils.getDisplayName(this, content, null, null));

        else
            setTitle(uri.getLastPathSegment());

        if (match == null)
            match = Constants.UTF_8;
        getActionBar().setSubtitle(match);

        checkHighlight();

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedReload,
                        R.string.reload, R.string.cancel, this::onClick);
    }

    private void getSavedInstanceValues(Bundle savedInstanceState) {
        path = savedInstanceState.getString(Constants.PATH);
        edit = savedInstanceState.getBoolean(Constants.EDIT);
        changed = savedInstanceState.getBoolean(Constants.CHANGED);
        match = savedInstanceState.getString(Constants.MATCH);
        modified = savedInstanceState.getLong(Constants.MODIFIED);
        content = savedInstanceState.getParcelable(Constants.CONTENT);
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();

        // Save current path
        savePath(path);

        // Stop highlighting
        textView.removeCallbacks(updateHighlight);
        textView.removeCallbacks(updateWordCount);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(Constants.PREF_SAVE, save);
        editor.putBoolean(Constants.PREF_VIEW, view);
        editor.putBoolean(Constants.PREF_WRAP, wrap);
        editor.putBoolean(Constants.PREF_SUGGEST, suggest);
        editor.putBoolean(Constants.PREF_HIGHLIGHT, highlight);
        editor.putInt(Constants.PREF_THEME, theme);
        editor.putInt(Constants.PREF_SIZE, size);
        editor.putInt(Constants.PREF_TYPE, type);

        // Add the set of recent files
        editor.putStringSet(Constants.PREF_PATHS, pathMap.keySet());

        // Add a position for each file
        for (String path : pathMap.keySet())
            editor.putInt(path, pathMap.get(path));

        // Remove the old ones
        for (String path : removeList)
            editor.remove(path);

        editor.apply();

        // Save file
        if (changed && save)
            saveFile();
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putParcelable(Constants.CONTENT, content);
        outState.putLong(Constants.MODIFIED, modified);
        outState.putBoolean(Constants.CHANGED, changed);
        outState.putString(Constants.MATCH, match);
        outState.putBoolean(Constants.EDIT, edit);
        outState.putString(Constants.PATH, path);
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        // Set up search view
        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

        // Show find all item
        menu.findItem(R.id.findAll).setVisible(menu.findItem(R.id.search).isActionViewExpanded());

        menu.findItem(R.id.edit).setVisible(!edit);
        menu.findItem(R.id.view).setVisible(edit);

        menu.findItem(R.id.save).setVisible(changed);

        menu.findItem(R.id.viewFile).setChecked(view);
        menu.findItem(R.id.autoSave).setChecked(save);
        menu.findItem(R.id.wrap).setChecked(wrap);
        menu.findItem(R.id.suggest).setChecked(suggest);
        menu.findItem(R.id.highlight).setChecked(highlight);

        switch (theme)
        {
        case Constants.LIGHT:
            menu.findItem(R.id.light).setChecked(true);
            break;

        case Constants.DARK:
            menu.findItem(R.id.dark).setChecked(true);
            break;

        case Constants.BLACK:
            menu.findItem(R.id.black).setChecked(true);
            break;

        case Constants.RETRO:
            menu.findItem(R.id.retro).setChecked(true);
            break;
        }

        switch (size)
        {
        case Constants.SMALL:
            menu.findItem(R.id.small).setChecked(true);
            break;

        case Constants.MEDIUM:
            menu.findItem(R.id.medium).setChecked(true);
            break;

        case Constants.LARGE:
            menu.findItem(R.id.large).setChecked(true);
            break;
        }

        switch (type)
        {
        case Constants.MONO:
            menu.findItem(R.id.mono).setChecked(true);
            break;

        case Constants.NORMAL:
            menu.findItem(R.id.normal).setChecked(true);
            break;
        }

        // Get the charsets
        Set<String> keySet = Charset.availableCharsets().keySet();
        // Get the submenu
        MenuItem item = menu.findItem(R.id.charset);
        item.setTitle(match);
        SubMenu sub = item.getSubMenu();
        sub.clear();
        // Add charsets contained in both sets
        sub.add(Menu.NONE, R.id.charsetItem, Menu.NONE, R.string.detect);
        for (String s : keySet) sub.add(Menu.NONE, R.id.charsetItem, Menu.NONE, s);

        // Get a list of recent files
        List<Long> list = new ArrayList<>();
        Map<Long, String> map = new HashMap<>();

        // Get the last modified dates
        for (String path : pathMap.keySet())
        {
            File file = new File(path);
            long last = file.lastModified();
            list.add(last);
            map.put(last, path);
        }

        // Sort in reverse order
        Collections.sort(list);
        Collections.reverse(list);

        // Get the submenu
        item = menu.findItem(R.id.openRecent);
        sub = item.getSubMenu();
        sub.clear();

        // Add the recent files
        for (long date : list)
        {
            String path = map.get(date);

            // Remove path prefix
            CharSequence name =
                path.replaceFirst(Environment
                                  .getExternalStorageDirectory()
                                  .getPath() + File.separator, "");
            // Create item
            sub.add(Menu.NONE, R.id.fileItem, Menu.NONE, TextUtils.ellipsize
                    (name, new TextPaint(), Constants.MENU_SIZE,
                     TextUtils.TruncateAt.MIDDLE))
                // Use condensed title to save path as API doesn't
                // work as documented
                .setTitleCondensed(name);
        }

        // Add clear list item
        sub.add(Menu.NONE, R.id.clearList, Menu.NONE, R.string.clearList);

        return true;
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case android.R.id.home:
            onBackPressed();
            break;
        case R.id.newFile:
            newFile();
            break;
        case R.id.edit:
            editClicked(item);
            break;
        case R.id.view:
            viewClicked(item);
            break;
        case R.id.open:
            openFile();
            break;
        case R.id.save:
            saveCheck();
            break;
        case R.id.saveAs:
            saveAs();
            break;
        case R.id.clearList:
            clearList();
            break;
        case R.id.findAll:
            findAll();
            break;
        case R.id.viewMarkdown:
            viewMarkdown();
            break;
        case R.id.viewFile:
            viewFileClicked(item);
            break;
        case R.id.autoSave:
            autoSaveClicked(item);
            break;
        case R.id.wrap:
            wrapClicked(item);
            break;
        case R.id.suggest:
            suggestClicked(item);
            break;
        case R.id.highlight:
            highlightClicked(item);
            break;
        case R.id.light:
            lightClicked(item);
            break;
        case R.id.dark:
            darkClicked(item);
            break;
        case R.id.black:
            blackClicked(item);
            break;
        case R.id.retro:
            retroClicked(item);
            break;
        case R.id.small:
            smallClicked(item);
            break;
        case R.id.medium:
            mediumClicked(item);
            break;
        case R.id.large:
            largeClicked(item);
            break;
        case R.id.mono:
            monoClicked(item);
            break;
        case R.id.normal:
            normalClicked(item);
            break;
        case R.id.about:
            aboutClicked();
            break;
        case R.id.fileItem:
            openRecent(item);
            break;
        case R.id.charsetItem:
            setCharset(item);
            break;
        }

        // Close text search
        if (searchItem != null && searchItem.isActionViewExpanded() &&
                item.getItemId() != R.id.findAll)
            searchItem.collapseActionView();

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        if (changed)
            alertDialog(R.string.appName, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                finish();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                changed = false;
                finish();
                break;
            }
        });

        else
            finish();
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        if (resultCode == RESULT_CANCELED)
            return;

        switch (requestCode)
        {
        case Constants.OPEN_DOCUMENT:
            content = data.getData();
            readFile(content);
            break;

        case Constants.CREATE_DOCUMENT:
            content = data.getData();
            setTitle(FileUtils.getDisplayName(this, content, null, null));
            saveFile();
            break;
        }
    }

    // dispatchTouchEvent
    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        scaleDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        // Check Ctrl key
        if (event.isCtrlPressed())
        {
            switch (keyCode)
            {
                // Edit, View
            case KeyEvent.KEYCODE_E:
                if (event.isShiftPressed())
                    viewClicked(null);
                else
                    editClicked(null);
                break;
                // New
            case KeyEvent.KEYCODE_N:
                newFile();
                break;
                // Open
            case KeyEvent.KEYCODE_O:
                openFile();
                break;
                // Save, Save as
            case KeyEvent.KEYCODE_S:
                if (event.isShiftPressed())
                    saveAs();
                else
                    saveCheck();
                break;

            default:
                return super.onKeyDown(keyCode, event);
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // editClicked
    private void editClicked(MenuItem item)
    {
        // Get scroll position
        int y = scrollView.getScrollY();
        // Get height
        int height = scrollView.getHeight();
        // Get width
        int width = scrollView.getWidth();

        // Get offset
        int line = textView.getLayout()
            .getLineForVertical(y + height / 2);
        int offset = textView.getLayout()
            .getOffsetForHorizontal(line, width / 2);
        // Set cursor
        textView.setSelection(offset);

        // Set editable with or without suggestions
        if (suggest)
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        else
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Change size and typeface temporarily as workaround for yet
        // another obscure feature of some versions of android
        textView.setTextSize((size == Constants.TINY)? Constants.HUGE: Constants.TINY);
        textView.setTextSize(size);
        textView.setTypeface((type == Constants.NORMAL)?
                             Typeface.MONOSPACE:
                             Typeface.DEFAULT, Typeface.NORMAL);
        textView.setTypeface((type == Constants.NORMAL)?
                             Typeface.DEFAULT:
                             Typeface.MONOSPACE, Typeface.NORMAL);
        // Update boolean
        edit = true;

        // Recreate
        recreate(this);
    }

    // viewClicked
    private void viewClicked(MenuItem item)
    {
        // Set read only
        textView.setRawInputType(InputType.TYPE_NULL);
        textView.clearFocus();

        // Update boolean
        edit = false;

        // Update menu
        invalidateOptionsMenu();
    }

    // newFile
    private void newFile()
    {
        // Check if file changed
        if (changed)
            alertDialog(R.string.newFile, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                newFile(null);
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                newFile(null);
                break;
            }

            invalidateOptionsMenu();
        });

        else
            newFile(null);

        invalidateOptionsMenu();
    }

    // newFile
    private void newFile(String text)
    {
        if (text != null)
            textView.setText(text);

        else
        {
            textView.setText("");
            changed = false;
        }

        file = getNewFile();
        uri = Uri.fromFile(file);
        path = uri.getPath();

        setTitle(uri.getLastPathSegment());
        match = Constants.UTF_8;
        getActionBar().setSubtitle(match);
    }

    // getNewFile
    private File getNewFile()
    {
        File documents = new
            File(Environment.getExternalStorageDirectory(), Constants.DOCUMENTS);
        return new File(documents, Constants.NEW_FILE);
    }

    // getDefaultFile
    private File getDefaultFile()
    {
        File documents = new
            File(Environment.getExternalStorageDirectory(), Constants.DOCUMENTS);
        return new File(documents, Constants.EDIT_FILE);
    }

    // defaultFile
    private void defaultFile()
    {
        file = getDefaultFile();

        uri = Uri.fromFile(file);
        path = uri.getPath();

        if (file.exists())
            readFile(uri);

        else
        {
            setTitle(uri.getLastPathSegment());
            match = Constants.UTF_8;
            getActionBar().setSubtitle(match);
        }
    }

    // setCharset
    private void setCharset(MenuItem item)
    {
        match = item.getTitle().toString();
        getActionBar().setSubtitle(match);
    }

    // alertDialog
    private void alertDialog(int title, int message,
                             int positiveButton, int negativeButton,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(positiveButton, listener);
        builder.setNegativeButton(negativeButton, listener);

        // Create the AlertDialog
        builder.show();
    }

    // alertDialog
    private void alertDialog(int title, String message, int neutralButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setNeutralButton(neutralButton, null);

        // Create the AlertDialog
        builder.show();
    }

    // savePath
    private void savePath(String path)
    {
        if (path == null)
            return;

        // Save the current position
        pathMap.put(path, scrollView.getScrollY());

        // Get a list of files
        List<Long> list = new ArrayList<>();
        Map<Long, String> map = new HashMap<>();
        for (String name : pathMap.keySet())
        {
            File file = new File(name);
            list.add(file.lastModified());
            map.put(file.lastModified(), name);
        }

        // Sort in reverse order
        Collections.sort(list);
        Collections.reverse(list);

        int count = 0;
        for (long date : list)
        {
            String name = map.get(date);

            // Remove old files
            if (count >= Constants.MAX_PATHS)
            {
                pathMap.remove(name);
                removeList.add(name);
            }

            count++;
        }
    }

    // openRecent
    private void openRecent(MenuItem item)
    {
        // Get path from condensed title
        String name = item.getTitleCondensed().toString();
        File file = new File(name);

        // Check absolute file
        if (!file.isAbsolute())
            file = new File(Environment.getExternalStorageDirectory(), name);
        // Check it exists
        if (file.exists())
        {
            Uri uri = Uri.fromFile(file);

            if (changed)
                alertDialog(R.string.openRecent, R.string.modified,
                            R.string.save, R.string.discard, (dialog, id) ->
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    saveFile();
                    readFile(uri);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    changed = false;
                    readFile(uri);
                    break;
                }
            });
            else
                readFile(uri);
        }
    }

    // saveAs
    private void saveAs()
    {
        // Remove path prefix
        String name =
            path.replaceFirst(Environment
                              .getExternalStorageDirectory()
                              .getPath() + File.separator, "");
        // Open dialog
        saveAsDialog(name, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                EditText text = ((Dialog) dialog).findViewById(R.id.pathText);
                String string = text.getText().toString();

                // Ignore empty string
                if (string.isEmpty())
                    return;

                file = new File(string);

                // Check absolute file
                if (!file.isAbsolute())
                    file = new
                        File(Environment.getExternalStorageDirectory(), string);

                // Set interface title
                uri = Uri.fromFile(file);
                String title = uri.getLastPathSegment();
                setTitle(title);

                path = file.getPath();
                content = null;
                saveFile();
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.setType(Constants.TEXT_WILD);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_TITLE, uri.getLastPathSegment());
                    startActivityForResult(intent, Constants.CREATE_DOCUMENT);
                }
                break;
            }
        });
    }

    // saveAsDialog
    private void saveAsDialog(String path,
                              DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save);
        builder.setMessage(R.string.choose);

        // Add the buttons
        builder.setPositiveButton(R.string.save, listener);
        builder.setNegativeButton(R.string.cancel, listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            builder.setNeutralButton(R.string.storage, listener);

        // Create edit text
        LayoutInflater inflater = (LayoutInflater) builder.getContext()
            .getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.save_path, null);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.show();
        TextView text = dialog.findViewById(R.id.pathText);
        text.setText(path);
    }

    // clearList
    private void clearList()
    {
        removeList.addAll(pathMap.keySet());
        pathMap.clear();
    }

    // findAll
    public void findAll()
    {
        // Get search string
        String search = searchView.getQuery().toString();

        FindTask findTask = new FindTask(this);
        findTask.execute(search);
    }

    // viewMarkdown
    private void viewMarkdown()
    {
        String text = textView.getText().toString();

        // Use common mark
        Parser parser = Parser.builder().build();
        Node document = parser.parse(text);
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String html = renderer.render(document);

        File file = new File(getCacheDir(), Constants.HTML_FILE);
        file.deleteOnExit();

        try (FileWriter writer = new FileWriter(file))
        {
            // Add HTML header and footer to make a valid page.
            writer.write(Constants.HTML_HEAD);
            writer.write(html);
            writer.write(Constants.HTML_TAIL);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // Get file provider uri
            Uri uri = FileProvider.getUriForFile
                (this, Constants.FILE_PROVIDER, file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, Constants.TEXT_HTML);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // viewFileClicked
    private void viewFileClicked(MenuItem item)
    {
        view = !view;
        item.setChecked(view);
    }

    // autoSaveClicked
    private void autoSaveClicked(MenuItem item)
    {
        save = !save;
        item.setChecked(save);
    }

    // wrapClicked
    private void wrapClicked(MenuItem item)
    {
        wrap = !wrap;
        item.setChecked(wrap);
	recreate(this);
    }

    // suggestClicked
    private void suggestClicked(MenuItem item)
    {
        suggest = !suggest;
        item.setChecked(suggest);

        if (suggest)
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        else
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                     InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	recreate(this);
    }

    // highlightClicked
    private void highlightClicked(MenuItem item)
    {
        highlight = !highlight;
        item.setChecked(highlight);

        checkHighlight();
    }

    // lightClicked
    private void lightClicked(MenuItem item)
    {
        theme = Constants.LIGHT;
        item.setChecked(true);
	recreate(this);
    }

    // darkClicked
    private void darkClicked(MenuItem item)
    {
        theme = Constants.DARK;
        item.setChecked(true);
	recreate(this);
    }

    // blackClicked
    private void blackClicked(MenuItem item)
    {
        theme = Constants.BLACK;
        item.setChecked(true);
	recreate(this);
    }

    // retroClicked
    private void retroClicked(MenuItem item)
    {
        theme = Constants.RETRO;
        item.setChecked(true);
	recreate(this);
    }

    // smallClicked
    private void smallClicked(MenuItem item)
    {
        size = Constants.SMALL;
        item.setChecked(true);

        textView.setTextSize(size);
    }

    // mediumClicked
    private void mediumClicked(MenuItem item)
    {
        size = Constants.MEDIUM;
        item.setChecked(true);

        textView.setTextSize(size);
    }

    // largeClicked
    private void largeClicked(MenuItem item)
    {
        size = Constants.LARGE;
        item.setChecked(true);

        textView.setTextSize(size);
    }

    // monoClicked
    private void monoClicked(MenuItem item)
    {
        type = Constants.MONO;
        item.setChecked(true);

        textView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    }

    // normalClicked
    private void normalClicked(MenuItem item)
    {
        type = Constants.NORMAL;
        item.setChecked(true);

        textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
    }

    // setSizeAndTypeface
    private void setSizeAndTypeface(int size, int type)
    {
        // Set size
        textView.setTextSize(size);

        // Set type
        switch (type)
        {
        case Constants.MONO:
            textView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            break;

        case Constants.NORMAL:
            textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            break;
        }
    }

    // aboutClicked
    private void aboutClicked()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.appName);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        SpannableStringBuilder spannable =
            new SpannableStringBuilder(getText(R.string.version));
        Pattern pattern = Pattern.compile("%s");
        Matcher matcher = pattern.matcher(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              BuildConfig.VERSION_NAME);
        matcher.reset(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              dateFormat.format(BuildConfig.BUILT));
        builder.setMessage(spannable);

        // Add the button
        builder.setPositiveButton(R.string.ok, null);

        // Create the AlertDialog
        Dialog dialog = builder.show();

        // Set movement method
        TextView text = dialog.findViewById(android.R.id.message);
        if (text != null)
        {
            text.setTextAppearance(builder.getContext(),
                                   android.R.style.TextAppearance_Small);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    // recreate
    private void recreate(Context context)
    {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();
    }

    // openFile
    private void openFile()
    {
        // Check if file changed
        if (changed)
            alertDialog(R.string.open, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                getFile();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                changed = false;
                getFile();
                break;
            }
        });

        else
            getFile();

    }

    // getFile
    private void getFile()
    {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.REQUEST_OPEN);
                return;
            }
        }

        // Open parent folder
        File dir = file.getParentFile();
        getFile(dir);
    }

    // getFile
    private void getFile(File dir)
    {
        // Get list of files
        List<File> fileList = getList(dir);

        // Get list of folders
        List<String> dirList = new ArrayList<>();
        dirList.add(File.separator);
        dirList.addAll(Uri.fromFile(dir).getPathSegments());

        // Pop up dialog
        openDialog(dirList, fileList, (dialog, which) ->
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                DialogInterface.BUTTON_NEUTRAL == which)
            {
                // Use storage
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType(Constants.TEXT_WILD);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, Constants.OPEN_DOCUMENT);
                return;
            }

            if (Constants.FOLDER_OFFSET <= which)
            {
                File file = new File(File.separator);
                for (int i = 0; i <= which - Constants.FOLDER_OFFSET; i++)
                    file = new File(file, dirList.get(i));
                if (file.isDirectory())
                    getFile(file);
                return;
            }

            File selection = fileList.get(which);
            if (selection.isDirectory())
                getFile(selection);

            else
                readFile(Uri.fromFile(selection));
        });
    }

    // getList
    private List<File> getList(File dir)
    {
        List<File> list;
        File[] files = dir.listFiles();
        // Check files
        if (files == null)
        {
            // Create a list with just the parent folder and the
            // external storage folder
            list = new ArrayList<>();
            if (dir.getParentFile() == null)
                list.add(dir);

            else
                list.add(dir.getParentFile());

            list.add(Environment.getExternalStorageDirectory());

            return list;
        }

        // Sort the files
        Arrays.sort(files);
        // Create a list
        list = new ArrayList<>(Arrays.asList(files));
        // Remove hidden files
        Iterator<File> iterator = list.iterator();
        while (iterator.hasNext())
        {
            File item = iterator.next();
            if (item.getName().startsWith("."))
                iterator.remove();
        }

        // Add parent folder
        if (dir.getParentFile() == null)
            list.add(0, dir);

        else
            list.add(0, dir.getParentFile());

        return list;
    }

    // openDialog
    private void openDialog(List<String> dirList, List<File> fileList,
                            DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Constants.FOLDER);

        // Add the adapter
        FileAdapter adapter = new FileAdapter(builder.getContext(), fileList);
        builder.setAdapter(adapter, listener);

        // Add storage button
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            builder.setNeutralButton(R.string.storage, listener);
        // Add cancel button
        builder.setNegativeButton(R.string.cancel, null);

        // Create the Dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Find the content view
        View view = dialog.findViewById(android.R.id.content);
        // Find the title view
        while (view instanceof ViewGroup)
            view = ((ViewGroup)view).getChildAt(0);
        // Get the parent view
        ViewGroup parent = (ViewGroup) view.getParent();
        // Replace content with scroll view
        parent.removeAllViews();
        HorizontalScrollView scroll = new
            HorizontalScrollView(dialog.getContext());
        parent.addView(scroll);
        // Add a row of folder buttons
        LinearLayout layout = new LinearLayout(dialog.getContext());
        scroll.addView(layout);
        for (String dir: dirList)
        {
            Button button = new Button(dialog.getContext());
            button.setId(dirList.indexOf(dir) + Constants.FOLDER_OFFSET);
            button.setText(dir);
            button.setOnClickListener((v) ->
            {
                listener.onClick(dialog, v.getId());
                dialog.dismiss();
            });
            layout.addView(button);
        }

        // Scroll to the end
        scroll.postDelayed(() ->
                scroll.fullScroll(View.FOCUS_RIGHT), Constants.POSITION_DELAY);
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case Constants.REQUEST_SAVE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .WRITE_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, save file
                    saveFile();
            break;

        case Constants.REQUEST_READ:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, read file
                    readFile(uri);
            break;

        case Constants.REQUEST_OPEN:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, open file
                    getFile();
            break;
        }
    }

    // readFile
    private void readFile(Uri uri)
    {
        if (uri == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.REQUEST_READ);
                this.uri = uri;
                return;
            }
        }

        long size;
        if (Constants.CONTENT.equalsIgnoreCase(uri.getScheme()))
            size = FileUtils.getSize(this, uri, null, null);

        else
        {
            File file = new File(uri.getPath());
            size = file.length();
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Size " + size);

        if (size > Constants.TOO_LARGE)
        {
            String large = getString(R.string.tooLarge);
            large = String.format(large, FileUtils.getReadableFileSize(size));
            alertDialog(R.string.appName, large, R.string.ok);
            return;
        }

        // Stop highlighting
        textView.removeCallbacks(updateHighlight);
        textView.removeCallbacks(updateWordCount);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Uri: " + uri);

        // Attempt to resolve content uri
        if (Constants.CONTENT.equalsIgnoreCase(uri.getScheme()))
        {
            content = uri;
            uri = resolveContent(uri);
        }

        else
            content = null;

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Uri: " + uri);

        // Read into new file if unresolved
        if (Constants.CONTENT.equalsIgnoreCase(uri.getScheme()))
        {
            file = getNewFile();
            Uri defaultUri = Uri.fromFile(file);
            path = defaultUri.getPath();

            setTitle(FileUtils.getDisplayName(this, content, null, null));
        }

        // Read file
        else
        {
            this.uri = uri;
            path = uri.getPath();
            file = new File(path);

            setTitle(uri.getLastPathSegment());
        }

        textView.setText(R.string.loading);

        ReadTask read = new ReadTask(this);
        read.execute(uri);

        changed = false;
        modified = file.lastModified();
        savePath(path);
        invalidateOptionsMenu();
    }

    // resolveContent
    private Uri resolveContent(Uri uri)
    {
        String path = FileUtils.getPath(this, uri);

        if (path != null)
        {
            File file = new File(path);
            if (file.canRead())
                uri = Uri.fromFile(file);
        }

        return uri;
    }

    // saveCheck
    private void saveCheck()
    {
        Uri uri = Uri.fromFile(file);
        Uri newUri = Uri.fromFile(getNewFile());
        if (newUri.getPath().equals(uri.getPath()))
            saveAs();

        else
            saveFile();
    }

    // saveFile
    private void saveFile()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.REQUEST_SAVE);
                return;
            }
        }

        // Stop highlighting
        textView.removeCallbacks(updateHighlight);
        textView.removeCallbacks(updateWordCount);

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedOverwrite,
                        R.string.overwrite, R.string.cancel, (dialog, id) ->
        {
            if (id == DialogInterface.BUTTON_POSITIVE) {
                saveFile(file);
            }
        });

        else
        {
            if (content == null)
                saveFile(file);

            else
            {
                saveFile(content);
                content = null;
            }
        }
    }

    // saveFile
    private void saveFile(File file)
    {
        CharSequence text = textView.getText();
        write(text, file);
    }

    // saveFile
    private void saveFile(Uri uri)
    {
        CharSequence text = textView.getText();
        try (OutputStream outputStream =
             getContentResolver().openOutputStream(uri, "rwt"))
        {
            write(text, outputStream);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
        }
    }

    // write
    private void write(CharSequence text, File file)
    {
        file.getParentFile().mkdirs();

        String charset = Constants.UTF_8;
        if (match != null && !match.equals(getString(R.string.detect)))
            charset = match;

        try (BufferedWriter writer = new BufferedWriter
             (new OutputStreamWriter(new FileOutputStream(file), charset)))
        {
            writer.append(text);
            writer.flush();
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }

        changed = false;
        invalidateOptionsMenu();
        modified = file.lastModified();
        savePath(file.getPath());
    }

    // write
    private void write(CharSequence text, OutputStream os)
    {
        String charset = Constants.UTF_8;
        if (match != null && !match.equals(getString(R.string.detect)))
            charset = match;

        try (BufferedWriter writer =
             new BufferedWriter(new OutputStreamWriter(os, charset)))
        {
            writer.append(text);
            writer.flush();
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }

        changed = false;
        invalidateOptionsMenu();
    }

    // checkHighlight
    private void checkHighlight()
    {
        // No syntax
        syntax = Constants.NO_SYNTAX;

        // Check extension
        if (highlight && file != null)
        {
            String ext = FileUtils.getExtension(file.getName());
            if (ext != null)
            {
                String type = FileUtils.getMimeType(file);

                if (ext.matches(Constants.CC_EXT))
                    syntax = Constants.CC_SYNTAX;

                else if (ext.matches(Constants.HTML_EXT))
                    syntax = Constants.HTML_SYNTAX;

                else if (ext.matches(Constants.CSS_EXT))
                    syntax = Constants.CSS_SYNTAX;

                else if (ext.matches(Constants.ORG_EXT))
                    syntax = Constants.ORG_SYNTAX;

                else if (ext.matches(Constants.MD_EXT))
                    syntax = Constants.MD_SYNTAX;

                else if (ext.matches(Constants.SH_EXT))
                    syntax = Constants.SH_SYNTAX;

                else if (!Constants.TEXT_PLAIN.equals(type))
                    syntax = Constants.DEF_SYNTAX;

                else
                    syntax = Constants.NO_SYNTAX;

                // Add callback
                if (textView != null && syntax != Constants.NO_SYNTAX)
                {
                    if (updateHighlight == null)
                        updateHighlight = this::highlightText;

                    textView.removeCallbacks(updateHighlight);
                    textView.postDelayed(updateHighlight, Constants.UPDATE_DELAY);

                    return;
                }
            }
        }

        // Remove highlighting
        if (updateHighlight != null)
        {
            textView.removeCallbacks(updateHighlight);
            textView.postDelayed(updateHighlight, Constants.UPDATE_DELAY);

            updateHighlight = null;
        }
    }

    // highlightText
    private void highlightText()
    {
        // Get visible extent
        int top = scrollView.getScrollY();
        int height = scrollView.getHeight();

        int line = textView.getLayout().getLineForVertical(top);
        int start = textView.getLayout().getLineStart(line);
        int first = textView.getLayout().getLineStart(line + 1);

        line = textView.getLayout().getLineForVertical(top + height);
        int end = textView.getLayout().getLineEnd(line);
        int last = (line == 0)? end:
            textView.getLayout().getLineStart(line - 1);

        // Move selection if outside range
        if (textView.getSelectionStart() < start)
            textView.setSelection(first);

        if (textView.getSelectionStart() > end)
            textView.setSelection(last);

        // Get editable
        Editable editable = textView.getEditableText();

        // Get current spans
        ForegroundColorSpan[] spans =
            editable.getSpans(start, end, ForegroundColorSpan.class);
        // Remove spans
        for (ForegroundColorSpan span: spans)
            editable.removeSpan(span);

        Matcher matcher;

        switch (syntax)
        {
        case Constants.NO_SYNTAX:
            // Get current spans
            spans = editable.getSpans(0, editable.length(),
                                      ForegroundColorSpan.class);
            // Remove spans
            for (ForegroundColorSpan span: spans)
                editable.removeSpan(span);
            break;

        case Constants.CC_SYNTAX:
            matcher = Constants.KEYWORDS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.TYPES);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CLASS);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.NUMBER);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.YELLOW);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.ANNOTATION);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CONSTANT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.LTGRAY);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.OPERATOR);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CC_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case Constants.HTML_SYNTAX:
            matcher = Constants.HTML_TAGS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.HTML_ATTRS);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.QUOTED);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.HTML_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case Constants.CSS_SYNTAX:
            matcher = Constants.CSS_STYLES.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CSS_HEX);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CC_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case Constants.ORG_SYNTAX:
            matcher = Constants.ORG_HEADER.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            matcher.region(start, end).usePattern(Constants.ORG_EMPH);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.ORG_LINK);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.ORG_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case Constants.MD_SYNTAX:
            matcher = Constants.MD_HEADER.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.MD_LINK);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.MD_EMPH);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.MD_CODE);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case Constants.SH_SYNTAX:
            matcher = Constants.KEYWORDS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.NUMBER);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.YELLOW);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CONSTANT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.LTGRAY);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.SH_VAR);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.OPERATOR);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.QUOTED);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.SH_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case Constants.DEF_SYNTAX:
            matcher = Constants.KEYWORDS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.TYPES);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CLASS);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.NUMBER);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.YELLOW);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.CONSTANT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.LTGRAY);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(Constants.QUOTED);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;
        }
    }

    // wordCountText
    private void wordCountText()
    {
        int words = 0;
        Matcher matcher = Constants.WORD_PATTERN.matcher(textView.getText());
        while (matcher.find())
        {
            words++;
        }

        if (customView != null)
        {
            String string = String.format(Locale.getDefault(), "%d\n%d",
                                          words, textView.length());
            customView.setText(string);
        }
    }

    // onActionModeStarted
    @Override
    public void onActionModeStarted(ActionMode mode)
    {
        super.onActionModeStarted(mode);

        // If there's a file
        if (file != null)
        {
            // Get the mime type
            String type = FileUtils.getMimeType(file);
            // If the type is not text/plain
            if (!Constants.TEXT_PLAIN.equals(type))
            {
                // Get the start and end of the selection
                int start = textView.getSelectionStart();
                int end = textView.getSelectionEnd();
                // And the text
                CharSequence text = textView.getText();

                // Get a pattern and a matcher for delimiter
                // characters
                Matcher matcher = Constants.PATTERN_CHARS.matcher(text);

                // Find the first match after the end of the selection
                if (matcher.find(end))
                {
                    // Update the selection end
                    end = matcher.start();

                    // Get the matched char
                    char c = text.charAt(end);

                    // Check for opening brackets
                    if (Constants.BRACKET_CHARS.indexOf(c) == -1)
                    {
                        switch (c)
                        {
                            // Check for close brackets and look for
                            // the open brackets
                        case ')':
                            c = '(';
                            break;

                        case ']':
                            c = '[';
                            break;

                        case '}':
                            c = '{';
                            break;

                        case '>':
                            c = '<';
                            break;
                        }

                        String string = text.toString();
                        // Do reverse search
                        start = string.lastIndexOf(c, start) + 1;

                        // Check for included newline
                        if (start > string.lastIndexOf('\n', end))
                            // Update selection
                            textView.setSelection(start, end);
                    }
                }
            }
        }
    }

    // checkMode
    private void checkMode(CharSequence text)
    {
        boolean change = false;

        CharSequence first = text.subSequence
            (0, Math.min(text.length(), Constants.FIRST_SIZE));
        CharSequence last = text.subSequence
            (Math.max(0, text.length() - Constants.LAST_SIZE), text.length());
        for (CharSequence sequence: new CharSequence[]{first, last})
        {
            Matcher matcher = Constants.MODE_PATTERN.matcher(sequence);
            if (matcher.find())
            {
                matcher.region(matcher.start(1), matcher.end(1));
                matcher.usePattern(Constants.OPTION_PATTERN);
                while (matcher.find())
                {
                    boolean no = "no".equals(matcher.group(2));

                    if ("vw".equals(matcher.group(3)))
                    {
                        if (view == no)
                        {
                            view = !no;
                            change = true;
                        }
                    }

                    else if ("ww".equals(matcher.group(3)))
                    {
                        if (wrap == no)
                        {
                            wrap = !no;
                            change = true;
                        }
                    }

                    else if ("sg".equals(matcher.group(3)))
                    {
                        if (suggest == no)
                        {
                            suggest = !no;
                            change = true;
                        }
                    }

                    else if ("hs".equals(matcher.group(3)))
                    {
                        if (highlight == no)
                        {
                            highlight = !no;
                            checkHighlight();
                        }
                    }

                    else if ("th".equals(matcher.group(3)))
                    {
                        if (":l".equals(matcher.group(4)))
                        {
                            if (theme != Constants.LIGHT)
                            {
                                theme = Constants.LIGHT;
                                change = true;
                            }
                        }

                        else if (":d".equals(matcher.group(4)))
                        {
                            if (theme != Constants.DARK)
                            {
                                theme = Constants.DARK;
                                change = true;
                            }
                        }

                        else if (":b".equals(matcher.group(4)))
                        {
                            if (theme != Constants.BLACK)
                            {
                                theme = Constants.BLACK;
                                change = true;
                            }
                        }

                        else if (":r".equals(matcher.group(4)))
                        {
                            if (theme != Constants.RETRO)
                            {
                                theme = Constants.RETRO;
                                change = true;
                            }
                        }
                    }

                    else if ("ts".equals(matcher.group(3)))
                    {
                        if (":l".equals(matcher.group(4)))
                        {
                            if (size != Constants.LARGE)
                            {
                                size = Constants.LARGE;
                                textView.setTextSize(size);
                            }
                        }

                        else if (":m".equals(matcher.group(4)))
                        {
                            if (size != Constants.MEDIUM)
                            {
                                size = Constants.MEDIUM;
                                textView.setTextSize(size);
                            }
                        }

                        else if (":s".equals(matcher.group(4)))
                        {
                            if (size != Constants.SMALL)
                            {
                                size = Constants.SMALL;
                                textView.setTextSize(size);
                            }
                        }
                    }

                    else if ("tf".equals(matcher.group(3)))
                    {
                        if (":m".equals(matcher.group(4)))
                        {
                            if (type != Constants.MONO)
                            {
                                type = Constants.MONO;
                                textView.setTypeface
                                    (Typeface.MONOSPACE, Typeface.NORMAL);
                            }
                        }

                        else if (":p".equals(matcher.group(4)))
                        {
                            if (type != Constants.NORMAL)
                            {
                                type = Constants.NORMAL;
                                textView.setTypeface
                                    (Typeface.DEFAULT, Typeface.NORMAL);
                            }
                        }
                    }

                    else if ("cs".equals(matcher.group(3)))
                    {
                        if (":u".equals(matcher.group(4)))
                        {
                            match = Constants.UTF_8;
                            getActionBar().setSubtitle(match);
                        }
                    }
                }
            }
        }

        if (change)
            recreate(this);
    }

    // loadText
    private void loadText(CharSequence text)
    {
        if (textView != null)
            textView.setText(text);

        changed = false;

        // Check for saved position
        if (pathMap.containsKey(path))
            textView.postDelayed(() ->
                                 scrollView.smoothScrollTo
                                 (0, pathMap.get(path)),
                                 Constants.POSITION_DELAY);
        else
            textView.postDelayed(() ->
                                 scrollView.smoothScrollTo(0, 0),
                                 Constants.POSITION_DELAY);
        // Check mode
        checkMode(text);

        // Check highlighting
        checkHighlight();

        // Set read only
        if (view)
        {
            textView.setRawInputType(InputType.TYPE_NULL);

            // Update boolean
            edit = false;
        }

        else
        {
            // Set editable with or without suggestions
            if (suggest)
                textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                      InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            else
                textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                      InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                      InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            // Change typeface temporarily as workaround for yet another
            // obscure feature of some versions of android
            textView.setTypeface((type == Constants.NORMAL)?
                                 Typeface.MONOSPACE:
                                 Typeface.DEFAULT, Typeface.NORMAL);
            textView.setTypeface((type == Constants.NORMAL)?
                                 Typeface.DEFAULT:
                                 Typeface.MONOSPACE, Typeface.NORMAL);
            // Update boolean
            edit = true;
        }

        // Dismiss keyboard
        textView.clearFocus();

        // Update menu
        invalidateOptionsMenu();
    }

    private void onClick(DialogInterface dialog, int id) {
        if (id == DialogInterface.BUTTON_POSITIVE) {
            readFile(uri);
        }
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        private final BackgroundColorSpan span = new
            BackgroundColorSpan(Color.YELLOW);
        private Editable editable;
        private Matcher matcher;
        private int index;
        private int height;

        // onQueryTextChange
        @Override
        public boolean onQueryTextChange(String newText)
        {
            // Use regex search and spannable for highlighting
            height = scrollView.getHeight();
            editable = textView.getEditableText();

            // Reset the index and clear highlighting
            if (newText.length() == 0)
            {
                index = 0;
                editable.removeSpan(span);
                return false;
            }

            // Check pattern
            try
            {
                Pattern pattern = Pattern.compile(newText, Pattern.MULTILINE);
                matcher = pattern.matcher(editable);
            }

            catch (Exception e)
            {
                return false;
            }

            // Find text
            if (matcher.find(index))
            {
                // Get index
                index = matcher.start();

                // Check layout
                if (textView.getLayout() == null)
                    return false;

                // Get text position
                int line = textView.getLayout().getLineForOffset(index);
                int pos = textView.getLayout().getLineBaseline(line);

                // Scroll to it
                scrollView.smoothScrollTo(0, pos - height / 2);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            else
                index = 0;

            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Find next text
            if (matcher.find())
            {
                // Get index
                index = matcher.start();

                // Get text position
                int line = textView.getLayout().getLineForOffset(index);
                int pos = textView.getLayout().getLineBaseline(line);

                // Scroll to it
                scrollView.smoothScrollTo(0, pos - height / 2);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            else
            {
                matcher.reset();
                index = 0;
            }

            return true;
        }
    }

    // readFile
    private CharSequence readFile(File file)
    {
        StringBuilder text = new StringBuilder();
        // Open file
        try (BufferedReader reader = new BufferedReader
             (new InputStreamReader
              (new BufferedInputStream(new FileInputStream(file)))))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                text.append(line);
                text.append(System.getProperty("line.separator"));
            }

            return text;
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        return text;
    }

    // ScaleListener
    private class ScaleListener
        extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        // onScale
        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            size *= detector.getScaleFactor();
            size = Math.max(Constants.TINY, Math.min(size, Constants.HUGE));
            textView.setTextSize(size);
            invalidateOptionsMenu();

            return true;
        }
    }

    // FindTask
    private static class FindTask
            extends AsyncTask<String, Void, List<File>>
    {
        private final WeakReference<Editor> editorWeakReference;
        private String search;

        // FindTask
        public FindTask(Editor editor)
        {
            editorWeakReference = new WeakReference<>(editor);
        }

        // doInBackground
        @Override
        protected List<File> doInBackground(String... params)
        {
            // Create a list of matches
            List<File> matchList = new ArrayList<>();
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return matchList;

            search = params[0];
            // Check pattern
            Pattern pattern;
            try
            {
                pattern = Pattern.compile(search, Pattern.MULTILINE);
            }

            catch (Exception e)
            {
                return matchList;
            }

            // Get entry list
            List<File> entries = new ArrayList<>();
            for (String path : editor.pathMap.keySet())
            {
                File entry = new File(path);
                entries.add(entry);
            }
 
            // Check the entries
            for (File file : entries)
            {
                CharSequence content = editor.readFile(file);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find())
                    matchList.add(file);
            }

            return matchList;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(List<File> matchList)
        {
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return;

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(editor);
            builder.setTitle(R.string.findAll);

            // If found populate dialog
            if (!matchList.isEmpty())
            {
                List<String> choiceList = new ArrayList<>();
                for (File file : matchList)
                {
                    // Remove path prefix
                    String path = file.getPath();
                    String name =
                        path.replaceFirst(Environment
                                          .getExternalStorageDirectory()
                                          .getPath() + File.separator, "");

                    choiceList.add(name);
                }

                String[] choices = choiceList.toArray(new String[0]);
                builder.setItems(choices, (dialog, which) ->
                {
                    File file = matchList.get(which);
                    Uri uri = Uri.fromFile(file);
                    // Open the entry chosen
                    editor.readFile(uri);

                    // Put the search text back - why it
                    // disappears I have no idea or why I have to
                    // do it after a delay
                    editor.searchView.postDelayed(() ->
                      editor.searchView.setQuery(search, false), Constants.FIND_DELAY);
                });
            }

            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    // ReadTask
    private static class ReadTask
        extends AsyncTask<Uri, Void, CharSequence>
    {
        private final WeakReference<Editor> editorWeakReference;

        public ReadTask(Editor editor)
        {
            editorWeakReference = new WeakReference<>(editor);
        }

        // doInBackground
        @Override
        protected CharSequence doInBackground(Uri... uris)
        {
            StringBuilder stringBuilder = new StringBuilder();
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return stringBuilder;

            // Default UTF-8
            if (editor.match == null)
            {
                editor.match = Constants.UTF_8;
                editor.runOnUiThread(() ->
                    editor.getActionBar().setSubtitle(editor.match));
            }

            try (BufferedInputStream in = new BufferedInputStream
                 (editor.getContentResolver().openInputStream(uris[0])))
            {
                // Create reader
                BufferedReader reader;
                if (editor.match.equals(editor.getString(R.string.detect)))
                {
                    // Detect charset, using UTF-8 hint
                    CharsetMatch match = new
                        CharsetDetector().setDeclaredEncoding(Constants.UTF_8)
                        .setText(in).detect();

                    if (match != null)
                    {
                        editor.match = match.getName();
                        editor.runOnUiThread(() ->
                            editor.getActionBar().setSubtitle(editor.match));
                        reader = new BufferedReader(match.getReader());
                    }

                    else
                        reader = new BufferedReader
                            (new InputStreamReader(in));

                    if (BuildConfig.DEBUG && match != null)
                        Log.d(TAG, "Charset " + editor.match);
                }

                else
                     reader = new BufferedReader
                         (new InputStreamReader(in, editor.match));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }
            }

            catch (Exception e)
            {
                editor.runOnUiThread(() ->
                    editor.alertDialog(R.string.appName,
                                       e.getMessage(),
                                       R.string.ok));
                e.printStackTrace();
            }

            return stringBuilder;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(CharSequence result)
        {
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return;

            editor.loadText(result);
        }
    }
}
