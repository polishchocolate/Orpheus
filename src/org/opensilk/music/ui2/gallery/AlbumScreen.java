/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.gallery;

import android.os.Bundle;

import com.andrew.apollo.R;

import javax.inject.Inject;

import flow.Layout;
import mortar.Blueprint;
import mortar.ViewPresenter;

/**
 * Created by drew on 10/3/14.
 */
@Layout(R.layout.gallery_album)
public class AlbumScreen implements Blueprint {

    @Override
    public String getMortarScopeName() {
        return getClass().getName();
    }

    @Override
    public Object getDaggerModule() {
        return new Module();
    }

    @dagger.Module (
            injects = AlbumView.class
    )
    public static class Module {

    }

    public static class Presenter extends ViewPresenter<AlbumView> {

        @Inject
        public Presenter() {

        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
        }

        @Override
        protected void onSave(Bundle outState) {
            super.onSave(outState);
        }

    }

}