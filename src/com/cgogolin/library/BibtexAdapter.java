package com.cgogolin.library;

import java.lang.Character;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;

import android.net.Uri;

import android .util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.View.OnClickListener;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.os.AsyncTask;

import android.webkit.MimeTypeMap;

public class BibtexAdapter extends BaseAdapter {
    
    public static final int STATUS_SORTING = 3;
    public static final int STATUS_FILTERING = 2;
    public static final int STATUS_NOT_INITIALIZED = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_FILE_NOT_FOUND = -1;
    public static final int STATUS_IO_EXCEPTION = -2;
    public static final int STATUS_IO_EXCEPTION_WHILE_CLOSING = -3;
    public static final int STATUS_INPUTSTREAM_NULL = -4;

    public enum SortMode {None, Date, Author, Journal}
    
    private ArrayList<BibtexEntry> bibtexEntryList;
    private ArrayList<BibtexEntry> displayedBibtexEntryList;
    private String filter = null;
    private int status = BibtexAdapter.STATUS_NOT_INITIALIZED;

    SortMode sortedAccodingTo = SortMode.None;
    String filteredAccodingTo = "";
    SortMode sortingAccodingTo = SortMode.None;
    String filteringAccodingTo = "";

    AsyncTask<String,Void,Void> applyFilterTask;
    AsyncTask<BibtexAdapter.SortMode,Void,Void> sortTask;
    
    public BibtexAdapter(InputStream inputStream) throws java.io.IOException
    {
        if(inputStream == null) {
            status = STATUS_INPUTSTREAM_NULL;
            return;
        }
        try{
            bibtexEntryList = BibtexParser.parse(inputStream);
        }
        catch (java.io.IOException e) {
            status = STATUS_IO_EXCEPTION;
            return;
        }
        finally{
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (java.io.IOException e) {
                    status = STATUS_IO_EXCEPTION_WHILE_CLOSING;
                }
            }
        }
            //Copy all entries to the filtered list
        displayedBibtexEntryList = new ArrayList<BibtexEntry>();
        displayedBibtexEntryList.addAll(bibtexEntryList);
        
        status = STATUS_OK;
    }


    public void onPreBackgroundOperation() {}
    public void onPostBackgroundOperation() {}
    public void onBackgroundOperationCanceled() {}
    
    public synchronized void filterInBackground(String filter) {
        if (filter == null || filteringAccodingTo.equals(filter))
            return;

        if(applyFilterTask!=null)
        {
            applyFilterTask.cancel(true);
        }
            
        applyFilterTask = new AsyncTask<String,Void,Void>() {
                @Override
                protected void onPreExecute() {
                        onPreBackgroundOperation();
                    }
                @Override
                protected Void doInBackground(String... filter) {
                    filteringAccodingTo = filter[0];
                    filter(filteringAccodingTo);
                    sortInBackground(sortingAccodingTo);
                    return null;
                }
                @Override
                protected void onPostExecute(Void v) {
                    notifyDataSetChanged();
                    onPostBackgroundOperation();
                }
            };
        applyFilterTask.execute(filter);
    }                              


    protected synchronized void filter(String... filter) {
        ArrayList<BibtexEntry> filteredTmpBibtexEntryList = new ArrayList<BibtexEntry>();
        if (filter[0].trim().equals(""))
        {
            filteredTmpBibtexEntryList.addAll(bibtexEntryList);
        }
        else
        {
            for ( BibtexEntry entry : bibtexEntryList ) {
                String blob = entry.getStringBlob().toLowerCase();
                String[] substrings = filter[0].toLowerCase().split(" ");
                boolean matches = true;
                for (String substring : substrings) 
                {
                    if ( !blob.contains(substring) ) {
                        matches = false;
                        break;
                    }
                }
                if (matches)
                    filteredTmpBibtexEntryList.add(entry);
            }
        }
        displayedBibtexEntryList = filteredTmpBibtexEntryList;
        filteredAccodingTo = filter[0];
    }
    

    public synchronized void sortInBackground(SortMode sortMode) {
        if(sortMode == null || sortingAccodingTo.equals(sortMode))
            return;
        
        if(sortTask!=null)
        {
            sortTask.cancel(true);
        }

        sortTask = new AsyncTask<BibtexAdapter.SortMode,Void,Void>() {
                @Override
                protected void onPreExecute() {
                        onPreBackgroundOperation();
                    }
                @Override
                protected Void doInBackground(BibtexAdapter.SortMode... sortMode) {
                    filterInBackground(filteringAccodingTo);//Does nothing if filtering is already done, else waits until filtering is finished
                    
                    sortingAccodingTo = sortMode[0];
                    sort(sortingAccodingTo);
                    return null;
                }
                @Override
                protected void onPostExecute(Void v) {
                    notifyDataSetChanged();
                    onPostBackgroundOperation();
                }        
            };
        sortTask.execute(sortMode);
    }

    
    protected synchronized void sort(SortMode sortMode) {
        switch(sortMode) {
            case None:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  entry1.getNumberInFile().compareTo(entry2.getNumberInFile());
                        }
                    });
                break;
            case Date:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry2.getDateFormated()+entry2.getNumberInFile()).compareTo(entry1.getDateFormated()+entry1.getNumberInFile());
                        }
                    });
                break;                
            case Author:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getAuthor()+entry1.getNumberInFile()).compareTo(entry2.getAuthor()+entry2.getNumberInFile());
                        }
                    });
                break;
            case Journal:
                Collections.sort(displayedBibtexEntryList, new Comparator<BibtexEntry>() {
                        @Override
                        public int compare(BibtexEntry entry1, BibtexEntry entry2) {
                            return  (entry1.getJournal()+entry1.getNumberInFile()).compareTo(entry2.getJournal()+entry2.getNumberInFile());
                        }
                    });
                break;
        }
        sortedAccodingTo = sortMode;
    }

    public synchronized void prepareForFiltering()
    {
        AsyncTask<Void, Void, Void> PrepareBibtexAdapterForFilteringTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... v) {
                    if(bibtexEntryList != null)
                        for ( BibtexEntry entry : bibtexEntryList ) {
                            entry.getStringBlob();
                        }
                    return null;
                }
            };
        PrepareBibtexAdapterForFilteringTask.execute();
    }

    private void setTextViewAppearance(TextView textView, String text){
        if(text.equals(""))
            textView.setVisibility(View.GONE);
        else
        {       
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override    
    public View getView(final int position, View convertView, ViewGroup parent)
    {
        final Context context = parent.getContext();
        final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    
        BibtexEntry entry = getItem(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.bibtexentry, null);
        }

        
        if(displayedBibtexEntryList == null || displayedBibtexEntryList.size() == 0) {
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_title), context.getString(R.string.no_matches));
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_authors), "");
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_journal), "");
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_doi), "");
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_arxiv), "");
        }
        else
        {
            setTextViewAppearance((TextView)convertView.findViewById(R.id.bibtex_title), entry.getTitle());
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_authors), entry.getAuthorsFormated(context));
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_journal), entry.getJournalFormated(context));
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_doi), entry.getDoiFormated(context));
            setTextViewAppearance((TextView) convertView.findViewById(R.id.bibtex_arxiv), entry.getEprintFormated());
            
            convertView.findViewById(R.id.LinearLayout02).setVisibility(View.GONE);
            
            convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout extraInfo = (LinearLayout)v.findViewById(R.id.LinearLayout02);
                        if(extraInfo.getVisibility() != View.VISIBLE)
                        {
                            extraInfo.removeAllViews();
                            
                            BibtexEntry entry = getItem(position);
                                //Add views here!!!                    
                            
                            
                            
                                //Read the Files list from the BibtexEntry
                            List<String> associatedFilesList = entry.getFiles();
                            if (associatedFilesList != null)
                            {
                                for (String file : associatedFilesList)
                                {
                                    final String path = getModifiedPath(file);//Path replacement can be done by overriding getModifiedPath()
                                    
                                    if (path == null || path.equals("")) continue;
                                    
                                    final Button button = new Button(context);
                                    button.setText(context.getString(R.string.file)+": "+path);
                                    button.setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(View v)
                                                {
                                                    Uri uri = Uri.parse("file://"+path); // Some PDF viewers seem to need this to open the file properly
                                                    if( uri != null && (new File(uri.getPath())).isFile() ) 
                                                    {
                                                            //Determine mime type
                                                        MimeTypeMap map = MimeTypeMap.getSingleton();
                                                        String extension ="";
                                                        if (path.lastIndexOf(".") != -1) extension = path.substring((path.lastIndexOf(".") + 1), path.length());
                                                        
                                                        String type = map.getMimeTypeFromExtension(extension);
                                                        
                                                            //Start application to open the file
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        intent.setDataAndType(uri, type);
//                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        try 
                                                        {
                                                            context.startActivity(intent);
                                                        }
                                                        catch (ActivityNotFoundException e) 
                                                        {
                                                            Toast.makeText(context, context.getString(R.string.no_application_to_view_files_of_type)+" "+type+".",Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                    else
                                                    {
                                                        Toast.makeText(context, context.getString(R.string.couldnt_find_file)+" "+path+".\n\n"+context.getString(R.string.path_conversion_hint),Toast.LENGTH_LONG).show();    
                                                    }
                                                }
                                        });
                                    extraInfo.addView(button);
                                }
                            }


                                //Read from the URLs list from the BibtexEntry
                            List<String> associatedUrlList = entry.getUrls(context);
                            if (associatedUrlList != null)
                            {
                                for (final String url : associatedUrlList)
                                {
                                    if ( url == null || url.equals("") ) continue;
                                    
                                    final Button button = new Button(context);
                                    button.setText(context.getString(R.string.url)+": "+url);
                                    button.setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(View v)
                                                {
                                                    
                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setData(Uri.parse(url));
                                                    try 
                                                    {
                                                        context.startActivity(intent);
                                                    }
                                                    catch (ActivityNotFoundException e) 
                                                    {
                                                        Toast.makeText(context, context.getString(R.string.error_opening_webbrowser),Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                        });
                                    extraInfo.addView(button);
                                }
                            }
                            
                            
                                //Read from the DOIs list from the BibtexEntry
                            List<String> associatedDoiList = entry.getDoiLinks(context);
                            if (associatedDoiList != null)
                            {
                                for (final String doi : associatedDoiList)
                                {
                                    if ( doi == null || doi.equals("") ) continue;
                                    
                                    final Button button = new Button(context);
                                    button.setText(context.getString(R.string.doi)+": "+doi);
                                    button.setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(View v)
                                                {
                                                    
                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setData(Uri.parse(doi));
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    try 
                                                    {
                                                        context.startActivity(intent);
                                                    }
                                                    catch (ActivityNotFoundException e) 
                                                    {
                                                        Toast.makeText(context, context.getString(R.string.error_opening_webbrowser),Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                        });
                                    extraInfo.addView(button);
                                }   
                            }

                                //Add a share button
//                    final String entryString = getEntryAsString(position);
                            final String entryString = entry.getEntryAsString();
                            final Button button = new Button(context);
                            button.setText(context.getString(R.string.share));
                            button.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v)
                                        {
                                            Intent shareIntent = new Intent();
                                            shareIntent.setAction(Intent.ACTION_SEND);
                                            shareIntent.setType("plain/text");
                                            shareIntent.setType("*/*");
                                            shareIntent.putExtra(Intent.EXTRA_TEXT, entryString);
                                            try 
                                            {
                                                context.startActivity(shareIntent);
                                            }
                                            catch (ActivityNotFoundException e) 
                                            {
                                                Toast.makeText(context, context.getString(R.string.error_starting_share_intent),Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                });
                            extraInfo.addView(button);
                            
                            extraInfo.setVisibility(View.VISIBLE);
                        } else {
                            extraInfo.setVisibility(View.GONE);
                        }
                    }
                }
                );
        }
        return convertView;
    }

    @Override
    public int getCount() {
        if(displayedBibtexEntryList == null || displayedBibtexEntryList.size() == 0)
            return 1;
        else
            return displayedBibtexEntryList.size();
    }

    @Override
    public BibtexEntry getItem(int position) {
        if(displayedBibtexEntryList == null || displayedBibtexEntryList.size() == 0) 
            return null;
        else
            return displayedBibtexEntryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }    

        //Can be overridento modify the path for opening files
    String getModifiedPath(String path) {
        return path;
    };
    

}
