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

package org.opensilk.music.ui.cards;

import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.event.SongQueueCardClick;
import org.opensilk.music.widgets.PlayingIndicator;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/25/14.
 */
public class SongQueueCard extends AbsBundleableCard<RecentSong> {

    @Inject @ForFragment
    Bus mBus; //Injected by adapter

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    @InjectView(R.id.card_playing_indicator)
    protected PlayingIndicator mPlayingIndicator;

    public SongQueueCard(Context context, RecentSong song) {
        super(context, song, R.layout.library_queue_listcard_inner);
    }

    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.PLAY, mData));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        mCardSubTitle.setText(mData.artistName);
        String artist = mData.albumArtistName;
        if (TextUtils.isEmpty(artist)) {
            artist = mData.artistName;
        }
        ArtworkManager.loadImage(new ArtInfo(artist, mData.albumName, mData.artworkUri), mArtwork);
        maybeStartPlayingIndicator();
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_next);
        m.inflate(R.menu.popup_remove_from_queue); //XXX different from SongCard
        if (mData.isLocal) {
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_set_ringtone);
            m.inflate(R.menu.popup_delete);
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_play_next:
                        mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.PLAY_NEXT, mData));
                        return true;
                    case R.id.popup_remove_from_queue:
                        mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.REMOVE_FROM_QUEUE, mData));
                        return true;
                    case R.id.popup_add_to_playlist:
                        mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.ADD_TO_PLAYLIST, mData));
                        return true;
                    case R.id.popup_more_by_artist:
                        mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.MORE_BY_ARTIST, mData));
                        return true;
                    case R.id.popup_set_ringtone:
                        mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.SET_RINGTONE, mData));
                        return true;
                    case R.id.popup_delete:
                        mBus.post(new SongQueueCardClick(SongQueueCardClick.Event.DELETE, mData));
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        return R.layout.library_queue_listcard_inner;
    }

    @Override
    protected int getGridLayout() {
        throw new UnsupportedOperationException("No grid layout for queue");
    }

    /**
     * Conditionally starts playing indicator animation
     */
    protected void maybeStartPlayingIndicator() {
        if (mPlayingIndicator != null) {
            // Always set gone. else recycled views might end up with it showing
            if (mPlayingIndicator.isAnimating()) {
                mPlayingIndicator.stopAnimating(); //stopAnimating sets GONE
            } else {
                mPlayingIndicator.setVisibility(View.GONE);
            }
            if (mData.recentId == MusicUtils.getCurrentAudioId()) {
                if (MusicUtils.isPlaying()) {
                    mPlayingIndicator.startAnimating();
                } else {
                    mPlayingIndicator.setVisibility(View.VISIBLE);
                }
            }
        }
    }

}
