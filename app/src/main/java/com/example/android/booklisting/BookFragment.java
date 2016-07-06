package com.example.android.booklisting;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class BookFragment extends Fragment {
    ArrayList<Book> mBookList = new ArrayList<Book>();
    private BookAdapter mBookAdapter;

    public BookFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.setRetainInstance(true);

        final EditText queryText;

        final View rootView = inflater.inflate(R.layout.fragment_book, container, false);

        queryText = (EditText) rootView.findViewById(R.id.topic_query);
        final TextView noDataView = (TextView) rootView.findViewById(R.id.no_data_text_view);

        ImageButton searchButton = (ImageButton) rootView.findViewById(R.id.search_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = queryText.getText().toString();
                ConnectivityManager connMgr = (ConnectivityManager)
                        getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new FetchBookDataTask().execute(query);
                } else {
                    Toast.makeText(getActivity(), "No Internet Connection Available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBookAdapter = new BookAdapter(getActivity(), mBookList);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_book);
        listView.setAdapter(mBookAdapter);
        listView.setEmptyView(noDataView);

        return rootView;
    }

    public class FetchBookDataTask extends AsyncTask<String, Void, ArrayList<Book>> {
        private final String LOG_TAG = FetchBookDataTask.class.getSimpleName();

        private ArrayList<Book> fetchJSON(String result) throws JSONException {
            ArrayList<Book> bookList = new ArrayList<Book>();

            JSONObject object = new JSONObject(result);
            JSONArray array = object.getJSONArray("items");

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                JSONObject volumeInfo = item.getJSONObject("volumeInfo");
                String title = volumeInfo.getString("title");

                String author;
                if (volumeInfo.has("authors")) {
                    JSONArray authors = volumeInfo.getJSONArray("authors");
                    author = authors.getString(0);
                } else {
                    author = "Unknown Author";
                }

                Book book = new Book(title, author);
                bookList.add(book);
            }

            return bookList;
        }

        @Override
        protected ArrayList<Book> doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String dataJsonStr = null;

            int numBooks = 10;

            try {
                final String BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";
                final String API_KEY_PARAM = "key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(API_KEY_PARAM, BuildConfig.GOOGLE_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                dataJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return fetchJSON(dataJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Book> books) {
            if (books != null) {
                mBookAdapter.clear();
                for (Book b : books) {
                    mBookAdapter.add(b);
                }
            }
        }
    }
}
