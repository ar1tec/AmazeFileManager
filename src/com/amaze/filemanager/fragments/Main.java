package com.amaze.filemanager.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.MyAdapter;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.asynctasks.LoadList;
import com.amaze.filemanager.services.asynctasks.LoadSearchList;
import com.amaze.filemanager.services.asynctasks.MoveFiles;
import com.amaze.filemanager.services.asynctasks.SearchTask;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.HistoryManager;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.RootHelper;
import com.amaze.filemanager.utils.Shortcuts;
import com.fourmob.poppyview.PoppyViewHelper;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.containers.Permissions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class Main extends ListFragment {
    public File[] file;
    public ArrayList<Layoutelements> list, slist;
    public MyAdapter adapter;
    public Futils utils;
    public ArrayList<File> sFile;
    public ArrayList<File> mFile = new ArrayList<File>();
    public boolean selection;
    public boolean results = false;
    public ActionMode mActionMode;
    public SharedPreferences Sp;
    Drawable folder, apk, unknown, archive, text;
    Resources res;
    public LinearLayout buttons;
    public int sortby, dsort, asc;
    public int uimode;
    ArrayList<String> COPY_PATH = null, MOVE_PATH = null;
    public String home, current = Environment.getExternalStorageDirectory().getPath(), sdetails;
    Shortcuts sh = new Shortcuts();
    HashMap<String, Bundle> scrolls = new HashMap<String, Bundle>();
    Main ma = this;
    public HistoryManager history;
    IconUtils icons;
    HorizontalScrollView scroll,scroll1;
	boolean rememberLastPath;
    public boolean rootMode, mountSystem,showHidden;
    View footerView;
    ImageButton paste;
    private PoppyViewHelper mPoppyViewHelper;
    LinearLayout pathbar;
    UpdatePathBar updatePathBar=new UpdatePathBar();
    @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        icons = new IconUtils(Sp, getActivity());
        }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        ImageButton overflow=(ImageButton)getActivity().findViewById(R.id.action_overflow);
        overflow.setVisibility(View.VISIBLE);
        (overflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });
        paste=(ImageButton)getActivity().findViewById(R.id.paste);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (COPY_PATH != null) {
                    String path1 = ma.current;
                    Intent intent = new Intent(getActivity(), CopyService.class);
                    intent.putExtra("FILE_PATHS", COPY_PATH);
                    intent.putExtra("COPY_DIRECTORY", path1);
                    getActivity().startService(intent);
                    COPY_PATH = null;
                }
                if (MOVE_PATH != null) {
                    new MoveFiles(utils.toFileArray(MOVE_PATH),ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,current);
                    MOVE_PATH = null;

                }invalidatePasteButton();

            }
        });
        utils = new Futils();
        res = getResources();
        mPoppyViewHelper = new PoppyViewHelper(getActivity());
        View poppyView = mPoppyViewHelper.createPoppyViewOnListView(android.R.id.list,R.layout.pooppybar);
        initPoppyViewListeners(poppyView);
        history = new HistoryManager(getActivity(), "Table1");
        rootMode = Sp.getBoolean("rootmode", false);
        mountSystem = Sp.getBoolean("mountsystem", false);
		showHidden=Sp.getBoolean("showHidden",true);
		rememberLastPath=Sp.getBoolean("rememberLastPath",false);
        int foldericon = Integer.parseInt(Sp.getString("folder", "1"));
        switch (foldericon) {
            case 0:
                folder = res.getDrawable(R.drawable.ic_grid_folder);
                break;
            case 1:
                folder = res.getDrawable(R.drawable.ic_grid_folder1);
                break;
            case 2:
                folder = res.getDrawable(R.drawable.ic_grid_folder2);
                break;
            default:
                folder = res.getDrawable(R.drawable.ic_grid_folder);
        }
        apk = res.getDrawable(R.drawable.ic_doc_apk);
        unknown = res.getDrawable(R.drawable.ic_doc_generic_am);
        archive = res.getDrawable(R.drawable.archive_blue);
        text = res.getDrawable(R.drawable.ic_doc_text_am);
        getSortModes();
        home = Sp.getString("home", System.getenv("EXTERNAL_STORAGE"));
        sdetails = Sp.getString("viewmode", "0");
        this.setRetainInstance(false);
		
        File f = new File(home);
		if(rememberLastPath){
			f=new File(Sp.getString("current",home));
		
			}
        buttons = (LinearLayout) getActivity().findViewById(R.id.buttons);
        pathbar = (LinearLayout) getActivity().findViewById(R.id.pathbar);
        pathbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                buttons.setVisibility(View.VISIBLE);
                updatePathBar.cancel(true);
                updatePathBar=new UpdatePathBar();
                updatePathBar.execute();
            }
        });
        scroll = (HorizontalScrollView) getActivity().findViewById(R.id.scroll);
        scroll1 = (HorizontalScrollView) getActivity().findViewById(R.id.scroll1);
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        ListView vl = getListView();
        if (uimode == 1) {
           float scale = getResources().getDisplayMetrics().density;
           int dpAsPixels = (int) (5 * scale + 0.5f);
           vl.setPadding(dpAsPixels, 0, dpAsPixels, 0);
           vl.setDivider(null);
           vl.setDividerHeight(dpAsPixels);

        }
        footerView=getActivity().getLayoutInflater().inflate(R.layout.divider,null);
        vl.addFooterView(footerView);
        vl.setFastScrollEnabled(true);
        if (savedInstanceState == null)
            loadlist(f, false);
        else {
            Bundle b = new Bundle();
            String cur = savedInstanceState.getString("current");
            b.putInt("index", savedInstanceState.getInt("index"));
            b.putInt("top", savedInstanceState.getInt("top"));
            scrolls.put(cur, b);
            list = savedInstanceState.getParcelableArrayList("list");
            createViews(list, true, new File(cur));
            if (savedInstanceState.getBoolean("selection")) {

                for (int i : savedInstanceState.getIntegerArrayList("position")) {
                    adapter.toggleChecked(i);
                }
            }
           
            getListView().setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int index = getListView().getFirstVisiblePosition();
        View vi = getListView().getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        outState.putInt("index", index);
        outState.putInt("top", top);
        outState.putParcelableArrayList("list", list);
        outState.putString("current", current);
        outState.putBoolean("selection", selection);
        if (selection) {
            outState.putIntegerArrayList("position", adapter.getCheckedItemPositions());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_extra, menu);
        initMenu(menu);

    }

    private void hideOption(int id, Menu menu) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showOption(int id, Menu menu) {
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }

    public void initMenu(Menu menu) {
        menu.findItem(R.id.item3).setIcon(icons.getCancelDrawable());
        menu.findItem(R.id.item4).setIcon(icons.getSearchDrawable());
        menu.findItem(R.id.item5).setIcon(icons.getNewDrawable());
    }

    public void onPrepareOptionsMenu(Menu menu) {
       /* hideOption(R.id.item8, menu);
        if (COPY_PATH != null) {
            showOption(R.id.item8, menu);
        }
        if (MOVE_PATH != null) {
            showOption(R.id.item8, menu);
        }*/
    }
    public void add() {
        AlertDialog.Builder ba = new AlertDialog.Builder(getActivity());
        ba.setTitle(utils.getString(getActivity(), R.string.add));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.select_dialog_item);
        adapter.add(utils.getString(getActivity(), R.string.folder));
        adapter.add(utils.getString(getActivity(), R.string.file));
        ba.setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                switch (p2) {

                    case 0:
                        final String path = ma.current;
                        AlertDialog.Builder ba1 = new AlertDialog.Builder(getActivity());
                        ba1.setTitle(utils.getString(getActivity(), R.string.newfolder));
                        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                        final EditText edir = (EditText) v.findViewById(R.id.newname);
                        edir.setHint(utils.getString(getActivity(), R.string.entername));
                        ba1.setView(v);
                        ba1.setNegativeButton(utils.getString(getActivity(), R.string.cancel), null);
                        ba1.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface p1, int p2) {
                                String a = edir.getText().toString();
                                File f = new File(path + "/" + a);
                                if (!f.exists()) {
                                    f.mkdirs();
                                    Toast.makeText(getActivity(), "Folder Created", Toast.LENGTH_LONG).show();
                                } else {
                                    Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.fileexist), Style.ALERT).show();
                                }
                            }
                        });
                        ba1.show();
                        break;
                    case 1:
                        final String path1 = ma.current;
                        AlertDialog.Builder ba2 = new AlertDialog.Builder(getActivity());
                        ba2.setTitle(utils.getString(getActivity(), R.string.newfolder));
                        View v1 = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                        final EditText edir1 = (EditText) v1.findViewById(R.id.newname);
                        edir1.setHint(utils.getString(getActivity(), R.string.entername));
                        ba2.setView(v1);
                        ba2.setNegativeButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface p1, int p2) {
                                // TODO: Implement this method
                            }
                        });
                        ba2.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface p1, int p2) {
                                String a = edir1.getText().toString();
                                File f1 = new File(path1 + "/" + a);
                                if (!f1.exists()) {
                                    try {
                                        boolean b = f1.createNewFile();
                                        Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.filecreated), Style.CONFIRM).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.fileexist), Style.ALERT).show();
                                }
                            }
                        });
                        ba2.show();
                        break;
                }
            }
        });
        ba.show();


    }

    public void home() {
        ma.loadlist(new File(ma.home), false);
    }

    public void search() {
        final String fpath = ma.current;

        Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.searchpath) + fpath, Toast.LENGTH_LONG).show();
        AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
        a.setTitle(utils.getString(getActivity(), R.string.search));
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText e = (EditText) v.findViewById(R.id.newname);
        e.setHint(utils.getString(getActivity(), R.string.enterfile));
        a.setView(v);
        a.setNeutralButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        a.setPositiveButton(utils.getString(getActivity(), R.string.search), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                String a = e.getText().toString();
                Bundle b = new Bundle();
                b.putString("FILENAME", a);
                b.putString("FILEPATH", fpath);
                new SearchTask((MainActivity) getActivity(), ma).execute(b);

            }
        });
        a.show();
    }


    public void onListItemClicked(int position, View v) {
        if (results) {
            String path = slist.get(position).getDesc();


            final File f = new File(path);
            if (f.isDirectory()) {

                loadlist(f, false);
                results = false;
            } else {
                utils.openFile(f, (MainActivity) getActivity());
            }
        } else if (selection == true) {
            adapter.toggleChecked(position);
            mActionMode.invalidate();
            if (adapter.getCheckedItemPositions().size() == 0) {
                selection = false;
                mActionMode.finish();
                mActionMode = null;
            }

        } else {

            String path;Layoutelements l=list.get(position);
            if(!l.hasSymlink()){
            path= l.getDesc();}
            else{path=l.getSymlink();}
            final File f = new File(path);
            if (f.isDirectory()) {
                computeScroll();
                loadlist(f, false);
            } else {

                utils.openFile(f, (MainActivity) getActivity());
            }

        }
    }


    public void loadlist(File f, boolean back) {
        if(mActionMode!=null){mActionMode.finish();}
        new LoadList(back, ma).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (f));

    }

    @SuppressWarnings("unchecked")
    public void loadsearchlist(ArrayList<String> f) {

        new LoadSearchList(ma).execute(f);

    }


    public void createViews(ArrayList<Layoutelements> bitmap, boolean back, File f) {
        try {
            if (bitmap != null) {
                TextView footerText=(TextView) footerView.findViewById(R.id.footerText);
                if(bitmap.size()==0){
                    footerText.setText("No Files");
                }
                else{
                    footerText.setText("Tap and hold on a File or Folder for more options");
                }
                adapter = new MyAdapter(getActivity(), R.layout.rowlayout,
                        bitmap, ma);
                try {
                    setListAdapter(adapter);

                } catch (Exception e) {
                }
                results = false;
                current = f.getPath();
                if (back) {
                    if (scrolls.containsKey(current)) {
                        Bundle b = scrolls.get(current);

                        getListView().setSelectionFromTop(b.getInt("index"), b.getInt("top"));
                    }
                }
               /* try {
                    Intent i = new Intent("updatepager");
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
                bbar(current);
                getActivity().getActionBar().setSubtitle(f.getName());

            }
        } catch (Exception e) {
        }

    }


    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        private void hideOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(false);
        }

        private void showOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(true);
        }

        public void initMenu(Menu menu) {
            menu.findItem(R.id.cpy).setIcon(icons.getCopyDrawable());
            menu.findItem(R.id.cut).setIcon(icons.getCutDrawable());
            menu.findItem(R.id.delete).setIcon(icons.getDeleteDrawable());
            menu.findItem(R.id.all).setIcon(icons.getAllDrawable());
            menu.findItem(R.id.about).setIcon(icons.getAboutDrawable());


        }

        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();

            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            initMenu(menu);
            hideOption(R.id.sethome, menu);
            hideOption(R.id.rename, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.about, menu);
            hideOption(R.id.openwith, menu);
            hideOption(R.id.ex, menu);
            mode.setTitle(utils.getString(getActivity(), R.string.select));

            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = adapter.getCheckedItemPositions();
            mode.setSubtitle(positions.size() + " "
                    + utils.getString(getActivity(), R.string.itemsselected));
            if (positions.size() == 1) {
                showOption(R.id.permissions,menu);
                showOption(R.id.about, menu);
                showOption(R.id.rename, menu);
                File x = new File(list.get(adapter.getCheckedItemPositions().get(0))
                        .getDesc());
                if (x.isDirectory()) {
                    showOption(R.id.sethome, menu);
                } else if (x.getName().toLowerCase().endsWith(".zip") || x.getName().toLowerCase().endsWith(".jar") || x.getName().toLowerCase().endsWith(".apk")) {
                    showOption(R.id.ex, menu);
                    hideOption(R.id.sethome, menu);
                    showOption(R.id.share, menu);
                } else {
                    hideOption(R.id.ex, menu);
                    hideOption(R.id.sethome, menu);
                    showOption(R.id.openwith, menu);
                    showOption(R.id.share, menu);
                }
            } else {
                hideOption(R.id.ex, menu);
                hideOption(R.id.sethome, menu);
                hideOption(R.id.openwith, menu);
                hideOption(R.id.share, menu);
                hideOption(R.id.permissions,menu);
                hideOption(R.id.about, menu);
            }
            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            computeScroll();
            ArrayList<Integer> plist = adapter.getCheckedItemPositions();
            switch (item.getItemId()) {
                case R.id.sethome:
                    int pos = plist.get(0);
                    home = list.get(pos).getDesc();
                    Crouton.makeText(getActivity(),
                            utils.getString(getActivity(), R.string.newhomedirectory) + mFile.get(pos).getName(),
                            Style.INFO).show();
                    Sp.edit().putString("home", list.get(pos).getDesc()).apply();

                    mode.finish();
                    return true;
                case R.id.about:
                    utils.showProps(new File(list.get((plist.get(0))).getDesc()), getActivity());
                    mode.finish();
                    return true;
                case R.id.delete:
                    utils.deleteFiles(list,ma, plist);

                    mode.finish();

                    return true;
                case R.id.share:
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_SEND);
                    i.setType("*/*");
                    i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(list.get(plist.get(0)).getDesc())));
                    startActivity(i);
                    mode.finish();
                    return true;
                case R.id.all:
                    if (adapter.areAllChecked()) {
                        adapter.toggleChecked(false);
                    } else {
                        adapter.toggleChecked(true);
                    }
                    mode.invalidate();

                    return true;
                case R.id.rename:
                    final ActionMode m = mode;
                    final File f = new File(list.get(
                            (plist.get(0))).getDesc());
                    View dialog = getActivity().getLayoutInflater().inflate(
                            R.layout.dialog, null);
                    AlertDialog.Builder a = new AlertDialog.Builder(getActivity());
                    final EditText edit = (EditText) dialog
                            .findViewById(R.id.newname);
                    edit.setText(f.getName());
                    a.setView(dialog);
                    a.setTitle(utils.getString(getActivity(), R.string.rename));
                    a.setPositiveButton(utils.getString(getActivity(), R.string.save),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface p1, int p2) {
                                    boolean b = utils.rename(f, edit.getText()
                                            .toString());
                                    m.finish();
                                    updateList();
                                    if (b) {
                                        Crouton.makeText(getActivity(),
                                                utils.getString(getActivity(), R.string.renamed),
                                                Style.CONFIRM).show();
                                    } else {
                                        Crouton.makeText(getActivity(),
                                                utils.getString(getActivity(), R.string.renameerror),
                                                Style.ALERT).show();
                                    }
                                    // TODO: Implement this method
                                }
                            }
                    );
                    a.setNegativeButton(utils.getString(getActivity(), R.string.cancel),
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface p1, int p2) {
                                    m.finish();
                                    // TODO: Implement this method
                                }
                            }
                    );
                    a.show();
                    mode.finish();
                    return true;
                case R.id.book:
                    for (int i1 = 0; i1 < plist.size(); i1++) {
                        try {
                            sh.addS(new File(list.get(plist.get(i1)).getDesc()));

                        } catch (Exception e) {
                        }
                    }
                    Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.bookmarksadded), Style.CONFIRM).show();
                    mode.finish();
                    return true;
                case R.id.ex:
                    Intent intent = new Intent(getActivity(), ExtractService.class);
                    intent.putExtra("zip", list.get(
                            (plist.get(0))).getDesc());
                    getActivity().startService(intent);
                    mode.finish();
                    return true;
                case R.id.cpy:
                    ArrayList<String> copies = new ArrayList<String>();

                    for (int i2 = 0; i2 < plist.size(); i2++) {
                        copies.add(list.get(plist.get(i2)).getDesc());
                    }
                    COPY_PATH = copies;
                    invalidatePasteButton();
                    mode.finish();
                    return true;
                case R.id.cut:

                    ArrayList<String> copie = new ArrayList<String>();
                    for (int i3 = 0; i3 < plist.size(); i3++) {
                        copie.add(list.get(plist.get(i3)).getDesc());
                    }
                    MOVE_PATH = copie;
                    invalidatePasteButton();
                    mode.finish();
                    return true;
                case R.id.compress:
                    ArrayList<String> copies1 = new ArrayList<String>();
                    for (int i4 = 0; i4 < plist.size(); i4++) {
                        copies1.add(list.get(plist.get(i4)).getDesc());
                    }
                    utils.showNameDialog((MainActivity) getActivity(), copies1, current);
                    mode.finish();
                    return true;
                case R.id.openwith:
                    utils.openWith(new File(list.get(
                            (plist.get(0))).getDesc()), getActivity());
                    mode.finish();
                    return true;
                case R.id.permissions:
                    utils.setPermissionsDialog(new File(list.get(plist.get(0)).getDesc()),(Activity)getActivity());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // called when the user exits the action mode
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;


            selection = false;
            adapter.toggleChecked(false);


        }
    };

    public void bbar(String text) {
        try {
            buttons.removeAllViews();
            Bundle b = utils.getPaths(text, getActivity());
            ArrayList<String> names = b.getStringArrayList("names");
            ArrayList<String> rnames = new ArrayList<String>();

            for (int i = names.size() - 1; i >= 0; i--) {
                rnames.add(names.get(i));
            }

            ArrayList<String> paths = b.getStringArrayList("paths");
            final ArrayList<String> rpaths = new ArrayList<String>();

            for (int i = paths.size() - 1; i >= 0; i--) {
                rpaths.add(paths.get(i));
            }
            for (int i = 0; i < names.size(); i++) {
                final int index = i;
                if (rpaths.get(i).equals("/")) {
                    ImageButton ib = new ImageButton(getActivity());
                    ib.setImageDrawable(icons.getRootDrawable());
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            loadlist(new File("/"), false);
                            // TODO: Implement this method
                        }
                    });

                    buttons.addView(ib);
                } else if (rpaths.get(i).equals(Environment.getExternalStorageDirectory().getPath())) {
                    ImageButton ib = new ImageButton(getActivity());
                    ib.setImageDrawable(icons.getSdDrawable());
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            loadlist(new File(rpaths.get(index)), true);
                            // TODO: Implement this method
                        }
                    });

                    buttons.addView(ib);
                } else {
                    Button button = new Button(getActivity());
                    button.setText(rnames.get(index));
                    button.setTextColor(getResources().getColor(android.R.color.white));
                    button.setTextSize(13);

                    //	button.setBackgroundDrawable(getResources().getDrawable(R.drawable.listitem));
                    button.setOnClickListener(new Button.OnClickListener() {

                        public void onClick(View p1) {
                            loadlist(new File(rpaths.get(index)), true);
                            //	Toast.makeText(getActivity(),rpaths.get(index),Toast.LENGTH_LONG).show();
                            // TODO: Implement this method
                        }
                    });

                    buttons.addView(button);
                }
            }
            File f=new File(text);
            ((TextView)pathbar.findViewById(R.id.pathname)).setText(f.getName());
            TextView bapath=(TextView)pathbar.findViewById(R.id.fullpath);
            bapath.setAllCaps(true);
            bapath.setText(f.getPath());
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(View.FOCUS_RIGHT);
                    scroll1.fullScroll(View.FOCUS_RIGHT);
                }
            });
            updatePathBar.cancel(true);
            updatePathBar=new UpdatePathBar();
            updatePathBar.execute();
        } catch (NullPointerException e) {
            System.out.println("button view not available");
        }

    }



    public void computeScroll() {
        int index = getListView().getFirstVisiblePosition();
        View vi = getListView().getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        Bundle b = new Bundle();
        b.putInt("index", index);
        b.putInt("top", top);
        scrolls.put(current, b);
    }

    public void goBack() {
        File f = new File(current);
        if (!results) {
            loadlist(f.getParentFile(), true);
        } else {
            loadlist(f, true);
        }
    }

    private BroadcastReceiver receiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        updateList();
        }
    };
   public void updateList(){
       computeScroll();
       loadlist(new File(current), true);}

    public void getSortModes() {
        int t = Integer.parseInt(Sp.getString("sortby", "0"));
        if (t <= 2) {
            sortby = t;
            asc = 1;
        } else if (t >= 3) {
            asc = -1;
            sortby = t - 3;
        }
        dsort = Integer.parseInt(Sp.getString("dirontop", "0"));

    }

    @Override
    public void onResume() {
        super.onResume();
        (getActivity()).registerReceiver(receiver2, new IntentFilter("loadlist"));
    }

    @Override
    public void onPause() {
        super.onPause();
		if(rememberLastPath){
			Sp.edit().putString("current",current).apply();
		}
        (getActivity()).unregisterReceiver(receiver2);
    }
	@Override
    public void onStop() {
        super.onStop();
	
		}
    public ArrayList<Layoutelements> addTo(ArrayList<String[]> mFile) {
        ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
        for (int i = 0; i < mFile.size(); i++) {
            File f=new File(mFile.get(i)[0]);
            if (f.isDirectory()) {
                a.add(utils.newElement(folder, f.getPath(),mFile.get(i)[2],mFile.get(i)[1],utils.count(f)));

            } else {
                try {
                    a.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f.getPath()), f.getPath(),mFile.get(i)[2],mFile.get(i)[1],utils.getSize(mFile.get(i))));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }


    @Override
    public void onDestroy() {
       super.onDestroy();
		
		history.end();     }
    public class UpdatePathBar extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void bitmap) {
            if(!isCancelled()){
            buttons.setVisibility(View.GONE);
            pathbar.setVisibility(View.VISIBLE);}
        }
    }
    public void initPoppyViewListeners(View poppy){
        ((ImageView)poppy.findViewById(R.id.back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack();if(mActionMode!=null){mActionMode.finish();}
            }
        });
        ((ImageView)poppy.findViewById(R.id.home)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                home();if(mActionMode!=null){mActionMode.finish();}
            }
        });
        ((ImageView)poppy.findViewById(R.id.refresh)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ma.loadlist(new File(ma.current), false);if(mActionMode!=null){mActionMode.finish();}
            }
        });
        ((ImageView)poppy.findViewById(R.id.history)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utils.showHistoryDialog(ma);
            }
        });
        ((ImageView)poppy.findViewById(R.id.books)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utils.showBookmarkDialog(ma,sh);
            }
        });
    }public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.activity_extra, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.item3:
                        getActivity().finish();
                        break;
                    case R.id.item9:
                        Sp.edit().putString("home", ma.current).apply();
                        Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.newhomedirectory) + ma.home, Style.CONFIRM).show();
                        ma.home = ma.current;
                        break;
                    case R.id.item10:
                        utils.showSortDialog(ma);
                        break;
                    case R.id.item11:
                        utils.showDirectorySortDialog(ma);
                        break;
                    case R.id.item5:
                        add();
                        break;

                    case R.id.item4:
                        search();
                        break;
                }
                return false;
            }
        });
        popup.show();
    }
    public void invalidatePasteButton(){
        if(MOVE_PATH!=null || COPY_PATH!=null){
            paste.setVisibility(View.VISIBLE);
        }else paste.setVisibility(View.GONE);
    }
}
