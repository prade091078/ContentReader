package com.prade.contentreader;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> cTitles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter<String> adapter ;
    SQLiteDatabase contentDB ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listViewContent);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,cTitles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),WebContent.class);
                intent.putExtra("webContent",content.get(i));
                startActivity(intent);
            }
        });

        contentDB = this.openOrCreateDatabase("Contents",MODE_PRIVATE,null);
        contentDB.execSQL("CREATE TABLE IF NOT EXISTS contents (id INTEGER PRIMARY KEY, contentId INTEGER, title VARCHAR, webContent VARCHAR)");

       updateListView();

        //https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty
        try {
            DownloadTask task = new DownloadTask();
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public void updateListView()
    {
        Cursor c = contentDB.rawQuery("SELECT * FROM contents", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst())
        {
            cTitles.clear();
            content.clear();

            do{
                cTitles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

            adapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String>
    {

        @Override
        protected String doInBackground(String... link) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
          try {
              url = new URL(link[0]);

              urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader reader = new InputStreamReader(in);

            int data = reader.read();

            while (data != -1) {
                char current = (char) data;
                result += current;

                data = reader.read();

            }

              Log.i("URLcontent", result);

              JSONArray jsonArray = new JSONArray(result);
              int numOfItems = 20;
              if(jsonArray.length() < 20)
              {
                  numOfItems = jsonArray.length();
              }
              for(int i=0;i< numOfItems;i++) {
                   String contentId = jsonArray.getString(i);
                url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ contentId +".json?print=pretty");
                urlConnection = (HttpURLConnection)url.openConnection();
                 in =urlConnection.getInputStream();
                 reader = new InputStreamReader(in);
                 data = reader.read();

                 String contentInfo = "";
                 while(data !=-1)
                 {
                     char current = (char) data;
                     contentInfo +=current;

                     data = reader.read();
                 }
                // Log.i("contentInfo",contentInfo);
                  JSONObject jsonObject=new JSONObject(contentInfo);
                 if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                     String contentTitie = jsonObject.getString("title");
                     String contentURL = jsonObject.getString("url");
                     //Log.i("info", contentInfo + contentURL);
                     url = new URL(contentURL);
                     urlConnection = (HttpURLConnection)url.openConnection();
                     in =urlConnection.getInputStream();
                     reader = new InputStreamReader(in);
                     data = reader.read();

                     String webContentInfo = "";
                     while(data !=-1)
                     {
                         char current = (char) data;
                         webContentInfo +=current;

                         data = reader.read();
                     }
                     //Log.i("info", webContentInfo + contentURL);

                     String sql = "INSERT INTO contents (contentId, title, webContent) VALUES (? ,? ,?)";

                     SQLiteStatement statement = contentDB.compileStatement(sql);
                     statement.bindString(1, contentId); // from inside loop
                     statement.bindString(2, contentTitie);
                     statement.bindString(3, webContentInfo);

                     statement.execute();
                 }
              }


        } catch(MalformedURLException e) {
              e.printStackTrace();
          } catch(IOException e) {
              e.printStackTrace();
          }catch(JSONException e) {
              e.printStackTrace();
          }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

}
