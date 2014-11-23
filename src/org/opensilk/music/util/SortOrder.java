/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.util;

import android.provider.MediaStore;

/**
 * Created by drew on 7/16/14.
 */
public class SortOrder {
    public static final String LOCAL_ALBUM_SONGS;
    public static final String SONG_GROUP;
    public static final String LAST_ADDED;
    public static final String PLAYLIST_MEMBERS;
    public static final String LOCAL_ARTIST_SONGS;
    public static final String GENRE_MEMBERS;
    public static final String LOCAL_ALBUMS;

    static {
        LOCAL_ALBUM_SONGS = MediaStore.Audio.Media.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        SONG_GROUP = MediaStore.Audio.AudioColumns.ALBUM_KEY + ", " + LOCAL_ALBUM_SONGS;
        LAST_ADDED = MediaStore.Audio.AudioColumns.DATE_ADDED + " DESC";
        PLAYLIST_MEMBERS = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER;
        LOCAL_ARTIST_SONGS = MediaStore.Audio.AudioColumns.ALBUM_KEY + ", " + LOCAL_ALBUM_SONGS;
        GENRE_MEMBERS = MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER;
        LOCAL_ALBUMS = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;
    }

    public interface ArtistSortOrder {
        String ARTIST_A_Z = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;
        String ARTIST_Z_A = ARTIST_A_Z + " DESC";
        String ARTIST_NUMBER_OF_SONGS = MediaStore.Audio.Artists.NUMBER_OF_TRACKS + " DESC";
        String ARTIST_NUMBER_OF_ALBUMS = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS + " DESC";
    }

    public interface AlbumSortOrder {
        String ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;
        String ALBUM_Z_A = ALBUM_A_Z + " DESC";
        String ALBUM_NUMBER_OF_SONGS = MediaStore.Audio.Albums.NUMBER_OF_SONGS + " DESC";
        String ALBUM_ARTIST = MediaStore.Audio.Albums.ARTIST;
        String ALBUM_YEAR = MediaStore.Audio.Albums.FIRST_YEAR + " DESC";
    }

    public interface SongSortOrder {
        String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        String SONG_Z_A = SONG_A_Z + " DESC";
        String SONG_ARTIST = MediaStore.Audio.Media.ARTIST;
        String SONG_ALBUM = MediaStore.Audio.Media.ALBUM;
        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";
        String SONG_DURATION = MediaStore.Audio.Media.DURATION + " DESC";
        String SONG_DATE = MediaStore.Audio.Media.DATE_ADDED + " DESC";
        String SONG_FILENAME = MediaStore.Audio.Media.DATA;
    }

    public interface GenreSortOrder {
        String GENRE_A_Z = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER;
        String GENRE_Z_A = GENRE_A_Z + " DESC";
    }

    public interface PlaylistSortOrder {
        String PLAYLIST_A_Z = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER;
        String PLAYLIST_Z_A = PLAYLIST_A_Z + " DESC";
        String PLAYLIST_DATE = MediaStore.Audio.Playlists.DATE_ADDED + " DESC";
    }

    public interface AlbumSongSortOrder {
        String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        String SONG_Z_A = SONG_A_Z + " DESC";
        String SONG_TRACK_LIST = MediaStore.Audio.Media.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        String SONG_DURATION = SongSortOrder.SONG_DURATION;
        String SONG_FILENAME = SongSortOrder.SONG_FILENAME;
    }

    public static interface ArtistSongSortOrder {
        String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        String SONG_Z_A = SONG_A_Z + " DESC";
        String SONG_ALBUM = MediaStore.Audio.Media.ALBUM;
        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";
        String SONG_DURATION = MediaStore.Audio.Media.DURATION + " DESC";
        String SONG_DATE = MediaStore.Audio.Media.DATE_ADDED + " DESC";
        String SONG_FILENAME = SongSortOrder.SONG_FILENAME;
    }

    public static interface ArtistAlbumSortOrder {
        String ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;
        String ALBUM_Z_A = ALBUM_A_Z + " DESC";
        String ALBUM_NUMBER_OF_SONGS = MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS + " DESC";
        String ALBUM_YEAR = MediaStore.Audio.Artists.Albums.FIRST_YEAR + " DESC";
    }

}
