package com.company;

// Step 3: The Domain Class - Part 2

public class Song {

    private final String artist;
    private final String year;
    private final String album;
    private final String title;

    public Song(String artist, String year, String album, String title) {
        this.album = album;
        this.artist =  artist;
        this.title = title;
        this.year = year;
    }

    public String getArtist() {
        return artist;
    }

    public String getYear() {
        return year;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Song{" +
                "artist='" + artist + '\'' +
                ", year='" + year + '\'' +
                ", album='" + album + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
