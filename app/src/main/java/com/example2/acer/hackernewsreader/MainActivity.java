package com.example2.acer.hackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Map<Integer,String> articalURLs=new HashMap<Integer, String>();
    Map<Integer,String> articalTitles=new HashMap<Integer, String>();
    ArrayList<Integer> articalIds=new ArrayList<Integer>();
    SQLiteDatabase articalsDB;
    ArrayList<String> titles=new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    ArrayList<String> urls=new ArrayList<String>();
    ArrayList<String> content=new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView=(ListView)findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent i =new Intent(getApplicationContext(),ArticleActivity.class);
                i.putExtra("articleUrl",urls.get(position));
                i.putExtra("content",content.get(position));
                startActivity(i);
             // Log.i("articleURL",urls.get(position));
            }
        });
        articalsDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE , null);
        articalsDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task=new DownloadTask();
        try {
             task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void updateListView(){
        try {
            Log.i("UI UPDATE","Done");
            Cursor c=articalsDB.rawQuery("SELECT * FROM articles ORDER BY articleId DESC",null);
            int contentIndex=c.getColumnIndex("content");
            int urlIndex=c.getColumnIndex("url");
            int titleIndex=c.getColumnIndex("title");
            c.moveToFirst();
            titles.clear();
            urls.clear();

            while (c != null ){
                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
                content.add(c.getString(contentIndex));
                //  Log.i("articleId", Integer.toString(c.getInt(articleIdIndex)));
                //  Log.i("articleUrl", c.getString(urlIndex));
                //  Log.i("articleTitle", c.getString(titleIndex));
                c.moveToNext();
            }
            arrayAdapter.notifyDataSetChanged();

            //  Log.i("articleIDs",articalIds.toString());
            //  Log.i("articleTitles",articalTitles.toString());
            //  Log.i("articleURLs",articalURLs.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {

            String result="";
            URL url;
            HttpURLConnection urlConnection=null;
            try {
                url=new URL(urls[0]);
                urlConnection=(HttpURLConnection) url.openConnection();
                InputStream in=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);
                int data=reader.read();
                while (data != -1){
                    char current=(char)data;
                    result += current;
                    data=reader.read();
                }

                JSONArray jsonArray=new JSONArray(result);
                articalsDB.execSQL("DELETE FROM articles");
                for (int i=0; i< 20 ; i++){
                    String articalId=jsonArray.getString(i);
                    //  Log.i("Artical ID",jsonArray.getString(i));
                    DownloadTask getArtical=new DownloadTask();
                    //String articalInfo=getArtical.execute("https://hacker-news.firebaseio.com/v0/item/"+articalId+".json?print=pretty").get();
                    //JSONObject jsonObject=new JSONObject(articalInfo);
                  url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articalId+".json?print=pretty");
                   urlConnection=(HttpURLConnection)url.openConnection();
                    in= urlConnection.getInputStream();
                    reader=new InputStreamReader(in);
                    data=reader.read();
                    String articleInfo="";
                    while (data != -1){
                        char current=(char)data;
                        articleInfo += current;
                        data=reader.read();
                    }
                    JSONObject jsonObject=new JSONObject(articleInfo);
                    if (jsonObject.has("title") && jsonObject.has("url")) {
                        String articalTitel=jsonObject.getString("title");
                        String articalURL=jsonObject.getString("url");
                        String articleContent="";
                   /*
                        url=new URL(articalURL);
                        urlConnection=(HttpURLConnection)url.openConnection();
                        in= urlConnection.getInputStream();
                        reader=new InputStreamReader(in);
                        data=reader.read();
                        String articleContent="";
                        while (data != -1){
                            char current=(char)data;
                            articleInfo += current;
                            data=reader.read();
                        }
                   */
                        articalIds.add(Integer.valueOf(articalId));
                        articalTitles.put(Integer.valueOf(articalId),articalTitel);
                        articalURLs.put(Integer.valueOf(articalId),articalURL);
                        String sql="INSERT INTO articles (articleId, url, title, content) VALUES (? , ? , ? , ?)";
                        SQLiteStatement statement= articalsDB.compileStatement(sql);
                        statement.bindString(1,articalId);
                        statement.bindString(2,articalURL);
                        statement.bindString(3,articalTitel);
                        statement.bindString(4,articleContent);

                        statement.execute();
                        //  articalsDB.execSQL("INSERT INTO articles (articleId, url, title) VALUES ('"+articalId+"', '"+articalURL+"', '"+articalTitel+"')");
                    }
                    // Log.i("articalTitel",articalTitel);
                    //   Log.i("articalURL",articalURL);
                }

            }catch (Exception e){
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
