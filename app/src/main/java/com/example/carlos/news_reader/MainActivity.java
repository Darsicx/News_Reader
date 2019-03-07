package com.example.carlos.news_reader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
    String[] pruebas;
    SQLiteDatabase articlesDB;
    ArrayList<String> titulos =new ArrayList<>();
    ArrayList<String> contenido =new ArrayList<>();
    ArrayAdapter<String> adapter;
    ProgressDialog dialog;
    int total=5;

    public void actualizaLista(){
        Cursor c=articlesDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex=c.getColumnIndex("content");
        int titleIndex=c.getColumnIndex("title");

        if (c.moveToFirst()){
            titulos.clear();
            contenido.clear();

            do {
                titulos.add(c.getString(titleIndex));
                contenido.add(c.getString(contentIndex));
            }
            while (c.moveToNext());

            adapter.notifyDataSetChanged();
        }
    }

    public class BajarNews extends AsyncTask<String,Float,String>{

        protected void onPreExecute() {
            dialog.setProgress(0);
            dialog.setMax(total);
            dialog.show(); //Mostramos el diálogo antes de comenzar
        }

        @Override
        protected String doInBackground(String... urls) {
            int contador=0;
            String result="";


            try {
                URL url=new URL(urls[0]);
                HttpURLConnection urlConnection =(HttpURLConnection) url.openConnection();
                urlConnection.connect();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader= new InputStreamReader(in);
                int data=reader.read();
                while (data!= -1){
                    char current=(char) data;
                    result+=current;
                    data=reader.read();

                }
                Log.i("Arreglo",result);
                try {
                    JSONArray jsonArray=new JSONArray(result);
                    articlesDB.execSQL("DELETE FROM articles");
                    for (int j=1;j<total;j++){//7-10 FUNCA
                        String articleId=jsonArray.getString(j);
                        Log.i("prelorio",articleId);
                        url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                        urlConnection=(HttpURLConnection) url.openConnection();
                        in= urlConnection.getInputStream();
                        reader=new InputStreamReader(in);
                        data=reader.read();
                        String article="";
                        while (data!= -1){
                            char current=(char) data;
                            article+=current;
                            data=reader.read();
                        }
                        Log.i("JSON",article);
                        JSONObject object =new JSONObject(article);
                        if (!object.isNull("title") && !object.isNull("url")) {
                           // String articleID = object.getString("id");
                            String articleUrl = object.getString("url");
                            String articleTitle = object.getString("title");
                            Log.i("URL",articleUrl);
                            url=new URL(articleUrl);
                            urlConnection=(HttpURLConnection) url.openConnection();
                            in= urlConnection.getInputStream();
                            reader=new InputStreamReader(in);
                            data=reader.read();
                            String articleContent="";
                            while (data!= -1){
                                char current=(char) data;
                                articleContent+=current;
                                data=reader.read();
                            }
                            Log.i("Contenido",articleContent);
                            String sql="INSERT INTO articles (articleid,title,content) VALUES (?,?,?)";
                            SQLiteStatement statement= articlesDB.compileStatement(sql);
                            statement.bindString(1,articleId);
                            statement.bindString(2,articleTitle);
                            statement.bindString(3,articleContent);
                            contador+=1;
                            publishProgress((float) contador);
                            statement.execute();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            actualizaLista();
            dialog.dismiss();
//           pruebas =result.split(",");
//
//            for (int i=1; i<pruebas.length;i++){
//                notasVisuales.add(pruebas[i]);
//            }
//
//            lista.setAdapter(adapter);


        }
        protected void onProgressUpdate (Float... valores) {
            int p = Math.round(valores[0]);
            dialog.setProgress(p);
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView lista=findViewById(R.id.lista);
        adapter= new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,titulos);
        dialog=new ProgressDialog(this);
        dialog.setMessage("Descargando...");
        dialog.setTitle("Progreso");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
       // dialog.setCancelable(false);
        lista.setAdapter(adapter);

        try {
            BajarNews nuevas = new BajarNews();
            nuevas.execute( "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Aqui hay error", Toast.LENGTH_SHORT).show();
        }


        articlesDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INT PRIMARY KEY,articleid INT,title VARCHAR,content VARCHAR)");
        actualizaLista();

        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intento =new Intent(getApplicationContext(),DespInfo.class);
                intento.putExtra("content",contenido.get(position));
                startActivity(intento);
            }
        });




    }
}
