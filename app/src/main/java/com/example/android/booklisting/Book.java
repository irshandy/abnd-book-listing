package com.example.android.booklisting;

/**
 * Created by IrvinShandy on 7/1/16.
 */
public class Book {
    private String mTitle;
    private String mAuthor;

    public Book(String title, String author) {
        mTitle = title;
        mAuthor = author;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAuthor() {
        return mAuthor;
    }
}
