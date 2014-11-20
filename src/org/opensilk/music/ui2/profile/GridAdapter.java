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

package org.opensilk.music.ui2.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.widgets.GridTileDescription;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.Mortar;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 7/10/14.
 */
public class GridAdapter extends ArrayAdapter<Object> {

    @Inject ArtworkRequestManager artworkRequestor;
    @Inject OverflowHandlers.LocalAlbums albumsOverflowHandler;
    @Inject OverflowHandlers.LocalSongGroups songGroupOverflowHandler;

    public GridAdapter(Context context) {
        super(context, -1);
        Mortar.inject(getContext(), this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder vh;
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            switch (getItemViewType(position)) {
                case 2:
                    v = inflater.inflate(R.layout.gallery_grid_item_artwork4, parent, false);
                    break;
                case 1:
                    v = inflater.inflate(R.layout.gallery_grid_item_artwork2, parent, false);
                    break;
                case 0:
                default:
                    v = inflater.inflate(R.layout.gallery_grid_item_artwork, parent, false);
                    break;
            }
            vh = new ViewHolder(v);
            v.setTag(vh);
        } else {
            vh = (ViewHolder) v.getTag();
            vh.reset();
        }
        Object obj = getItem(position);
        if (obj instanceof LocalAlbum) {
            final LocalAlbum la = (LocalAlbum)obj;
            vh.title.setText(la.name);
            vh.subtitle.setText(la.artistName);
            PaletteObserver paletteObserver = vh.descriptionContainer != null
                    ? vh.descriptionContainer.getPaletteObserver() : null;
            vh.subscriptions.add(artworkRequestor.newAlbumRequest(vh.artwork,
                    paletteObserver, new ArtInfo(la.artistName, la.name, la.artworkUri), ArtworkType.THUMBNAIL));
            vh.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(getContext(), v);
                    albumsOverflowHandler.populateMenu(m, la);
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                return albumsOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), la);
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                    });
                    m.show();
                }
            });
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavUtils.openAlbumProfile(v.getContext(), la);
                }
            });
        } else if (obj instanceof LocalSongGroup) {
            final LocalSongGroup lsg = (LocalSongGroup) obj;
            vh.title.setText(lsg.name);
            String l2 = MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, lsg.albumIds.length)
                    + ", " + MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, lsg.songIds.length);
            vh.subtitle.setText(l2);
            switch (vh.artNumber) {
                case 4:
                    if (lsg.albumIds.length >= 4) {
                        vh.subscriptions.add(artworkRequestor.newAlbumRequest(vh.artwork4,
                                null, lsg.albumIds[3], ArtworkType.THUMBNAIL));
                        vh.subscriptions.add(artworkRequestor.newAlbumRequest(vh.artwork3,
                                null, lsg.albumIds[2], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 2:
                    if (lsg.albumIds.length >= 2) {
                        vh.subscriptions.add(artworkRequestor.newAlbumRequest(vh.artwork2,
                                null, lsg.albumIds[1], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 1:
                    if (lsg.albumIds.length >= 1) {
                        vh.subscriptions.add(artworkRequestor.newAlbumRequest(vh.artwork,
                                null, lsg.albumIds[0], ArtworkType.THUMBNAIL));
                    } else {
                        ((AnimatedImageView) vh.artwork).setDefaultImage();
                    }
            }
            vh.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(getContext(), v);
                    songGroupOverflowHandler.populateMenu(m, lsg);
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                return songGroupOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), lsg);
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                    });
                    m.show();
                }
            });
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavUtils.openSongGroupProfile(v.getContext(), lsg);
                }
            });
        }
        return v;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position == getCount()) {
            return -1;
        }
        Object obj = getItem(position);
        if (obj instanceof LocalAlbum) {
            return 0;
        } else if (obj instanceof LocalSongGroup) {
            LocalSongGroup lsg = (LocalSongGroup)obj;
            if (lsg.albumIds.length >= 4) {
                return 2;
            } else if (lsg.albumIds.length >= 2) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    public static class ViewHolder {

        final View itemView;
        @InjectView(R.id.artwork_thumb) AnimatedImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional AnimatedImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional AnimatedImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional AnimatedImageView artwork4;
        @InjectView(R.id.grid_description) @Optional GridTileDescription descriptionContainer;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        final CompositeSubscription subscriptions;
        final int artNumber;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
            if (artwork4 != null) {
                artNumber = 4;
            } else if (artwork2 != null) {
                artNumber = 2;
            } else {
                artNumber = 1;
            }
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            if (artwork2 != null) artwork2.setImageBitmap(null);
            if (artwork3 != null) artwork3.setImageBitmap(null);
            if (artwork4 != null) artwork4.setImageBitmap(null);
            if (descriptionContainer != null) descriptionContainer.resetBackground();
            subscriptions.clear();
        }

    }

}