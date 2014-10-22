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

package org.opensilk.music.artwork;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import com.andrew.apollo.utils.ApolloUtils;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.ui2.loader.AlbumArtInfoLoader;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.opensilk.Fetch;
import de.umass.lastfm.opensilk.MusicEntryResponseCallback;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/21/14.
 */
@Singleton
public class AlbumArtworkRequestManager {

    final Context mContext;
    final AppPreferences mPreferences;
    final BitmapLruCache mL1Cache;
    final BitmapDiskLruCache mL2Cache;
    final RequestQueue mVolleyQueue;
    final Gson mGson;

    @Inject
    public AlbumArtworkRequestManager(@ForApplication Context mContext, AppPreferences mPreferences,
                                      BitmapLruCache mL1Cache, BitmapDiskLruCache mL2Cache,
                                      RequestQueue mVolleyQueue, Gson mGson) {
        this.mContext = mContext;
        this.mPreferences = mPreferences;
        this.mL1Cache = mL1Cache;
        this.mL2Cache = mL2Cache;
        this.mVolleyQueue = mVolleyQueue;
        this.mGson = mGson;
    }

    abstract class BaseArtworkRequest implements Subscription {
        final WeakReference<ImageView> imageViewWeakReference;
        final ArtInfo artInfo;
        final ArtworkType artworkType;

        Subscription subscription;
        boolean unsubscribed = false;

        BaseArtworkRequest(ImageView imageView, ArtInfo artInfo, ArtworkType artworkType) {
            this.imageViewWeakReference = new WeakReference<>(imageView);
            this.artInfo = artInfo;
            this.artworkType = artworkType;
        }

        public Subscription start() {
            tryForCache();
            return this;
        }

        @Override
        public void unsubscribe() {
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
            imageViewWeakReference.clear();
            unsubscribed = true;
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }

        void setDefaultImage() {
            imageViewWeakReference.get().setImageResource(R.drawable.default_artwork);
        }

        void setImageBitmap(Bitmap bitmap) {
            setImageBitmap(bitmap, false);
        }

        void setImageBitmap(final Bitmap bitmap, boolean fromCache) {
            if (fromCache) {
                imageViewWeakReference.get().setImageBitmap(bitmap);
            } else {
                imageViewWeakReference.get().animate().alpha(0).setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                imageViewWeakReference.get().setImageBitmap(bitmap);
                                imageViewWeakReference.get().animate().alpha(1).setDuration(100).start();
                            }
                        }).start();
            }
        }

        public void tryForCache() {
            subscription = createCacheObservable(artInfo, artworkType)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            setImageBitmap(bitmap, true);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            if (throwable instanceof CacheMissException) {
                                onCacheMiss();
                            } else {
                                setDefaultImage();
                            }
                        }
                    });
        }

        abstract void onCacheMiss();
    }

    public class ArtistArtworkRequest extends BaseArtworkRequest {

        public ArtistArtworkRequest(ImageView imageView, ArtInfo artInfo, ArtworkType artworkType) {
            super(imageView, artInfo, artworkType);
        }

        @Override
        void onCacheMiss() {
            if (unsubscribed) return;
            Timber.i("onCacheMiss %s", artInfo);
            setDefaultImage();
            if (TextUtils.isEmpty(artInfo.artistName)) return;
            boolean isOnline = ApolloUtils.isOnline(mContext);
            boolean wantArtistImages = mPreferences.getBoolean(AppPreferences.DOWNLOAD_MISSING_ARTIST_IMAGES, true);
            if (isOnline && wantArtistImages) {
                tryForNetwork();
            }
        }

        void tryForNetwork() {
            subscription = createArtistNetworkRequest(artInfo, artworkType)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            setImageBitmap(bitmap);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.e(throwable, "Unable to obtain image for %s", artInfo);
                        }
                    });
        }
    }

    public class AlbumArtworkRequest extends BaseArtworkRequest {

        public AlbumArtworkRequest(ImageView imageView, ArtInfo artInfo, ArtworkType artworkType) {
            super(imageView, artInfo, artworkType);
        }

        @Override
        public Subscription start() {
            tryForNetwork(false);
            return this;
        }

        @Override
        void onCacheMiss() {
            if (unsubscribed) return;
            Timber.v("onCacheMiss: %s", artInfo);
            setDefaultImage();
            //check if we have everything we need to download artwork
            boolean hasAlbumArtist = !TextUtils.isEmpty(artInfo.albumName) && !TextUtils.isEmpty(artInfo.artistName);
            boolean hasUri = artInfo.artworkUri != null && !artInfo.artworkUri.equals(Uri.EMPTY);
            boolean isOnline = ApolloUtils.isOnline(mContext);
            boolean wantAlbumArt = mPreferences.getBoolean(AppPreferences.DOWNLOAD_MISSING_ARTWORK, true);
            boolean preferDownload = mPreferences.getBoolean(AppPreferences.PREFER_DOWNLOAD_ARTWORK, false);
            boolean isLocalArt = isLocalArtwork(artInfo.artworkUri);
            if (hasAlbumArtist && hasUri) {
                // We have everything we may need
                if (isOnline && wantAlbumArt) {
                    // were online and want artwork
                    if (isLocalArt && !preferDownload) {
                        // try mediastore first if local and user prefers
                        tryForMediaStore(true);
                    } else {
                        // go to network, falling back to mediastore if local
                        tryForNetwork(isLocalArt);
                    }
                } else if (isOnline && !isLocalArt) {
                    // were online and have an external uri lets get it
                    // regardless of user preference
                    tryForUrl();
                } else if (!isOnline && isLocalArt && !preferDownload) {
                    // were offline, this is a local source
                    // and the user doesnt want to try network first
                    // go ahead and fetch the mediastore image
                    tryForMediaStore(false);
                } // else were offline and cant get artwork or the user wants to defer
            } else if (hasAlbumArtist) {
                if (isOnline) {
                    // try for network, we dont have a uri so dont try the mediastore on failure
                    tryForNetwork(false);
                }
            } else if (hasUri) {
                if (isLocalArt) {
                    //Wait what? this should never happen
                    tryForMediaStore(isOnline);
                } else if (isOnline) {
                    //all we have is a url so go for it
                    tryForUrl();
                }
            } //else just ignore the request
        }

        public void tryForNetwork(final boolean tryMediaStoreOnFailure) {
            subscription = createAlbumNetworkObservable(artInfo, artworkType)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            setImageBitmap(bitmap);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            onNetworkMiss(tryMediaStoreOnFailure);
                        }
                    });
        }

        void onNetworkMiss(final boolean tryMediaStore) {
            Timber.v("onNetworkMiss %s", artInfo);
            if (tryMediaStore && !unsubscribed) {
                tryForMediaStore(false);
            }
        }

        public void tryForMediaStore(boolean tryNetworkOnFailure) {

        }

        void onMediaStoreMiss(boolean tryNetwork) {
            if (tryNetwork) {
                tryForNetwork(false);
            }
        }

        public void tryForUrl() {
            subscription = createImageRequestObservable(artInfo.artworkUri.toString(), artworkType)
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap bitmap) {
                            setImageBitmap(bitmap);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.e(throwable, "tryForUrl %s", artInfo);
                        }
                    });
        }
    }

    public AlbumArtworkRequest newAlbumRequest(ImageView imageView, ArtInfo artInfo, ArtworkType artworkType) {
        return new AlbumArtworkRequest(imageView, artInfo, artworkType);
    }

    public Observable<Bitmap> createCacheObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {
                    Timber.v("Trying L1 for %s, from %s", artInfo.albumName, Thread.currentThread().getName());
                    Bitmap bitmap = mL1Cache.getBitmap(getCacheKey(artInfo, artworkType));
                    if (!subscriber.isUnsubscribed()) {
                        if (bitmap != null) {
                            subscriber.onNext(bitmap);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new CacheMissException());
                        }
                    }
                }
            })
            // We missed the l1cache try l2
            .onErrorResumeNext(new Func1<Throwable, Observable<? extends Bitmap>>() {
                @Override
                public Observable<? extends Bitmap> call(Throwable throwable) {
                    if (throwable instanceof CacheMissException) {
                        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
                            @Override
                            public void call(Subscriber<? super Bitmap> subscriber) {
                                Timber.v("Trying L2 for %s, from %s", artInfo.albumName, Thread.currentThread().getName());
                                Bitmap bitmap = mL2Cache.getBitmap(getCacheKey(artInfo, artworkType));
                                if (!subscriber.isUnsubscribed()) {
                                    if (bitmap != null) {
                                        subscriber.onNext(bitmap);
                                        subscriber.onCompleted();
                                    } else {
                                        subscriber.onError(new CacheMissException());
                                    }
                                }
                            }
                        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
                    } else {
                        return Observable.error(throwable);
                    }
                }
            });
    }

    public Observable<Bitmap> createCacheObservable(final long id, final ArtworkType artworkType) {
        // first we have to get the artinfo for the album
        return new AlbumArtInfoLoader(mContext, new long[]{id}).createObservable()
                // then we can creat the cache observable with the artinfo
            .flatMap(new Func1<ArtInfo, Observable<Bitmap>>() {
                @Override
                public Observable<Bitmap> call(final ArtInfo artInfo) {
                    return createCacheObservable(artInfo, artworkType);
                }
            });
    }

    public Observable<Bitmap> createAlbumNetworkObservable(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createAlbumLastFmApiRequestObservable(artInfo)
                // remap the album info returned by last fm into a url where we can find an image
                .flatMap(new Func1<Album, Observable<String>>() {
                    @Override
                    public Observable<String> call(final Album album) {
                        // try coverartarchive
                        if (!mPreferences.getBoolean(AppPreferences.WANT_LOW_RESOLUTION_ART, false)) {
                            Timber.v("Creating CoverArtRequest %s, from %s", album.getName(), Thread.currentThread().getName());
                            return createAlbumCoverArtRequestObservable(album.getMbid())
                                    // if coverartarchive fails fallback to lastfm
                                    // im using ResumeNext so i can propogate the error
                                    // not sure Return will do that properly TODO find out
                                    .onErrorResumeNext(new Func1<Throwable, Observable<String>>() {
                                        @Override
                                        public Observable<String> call(Throwable throwable) {
                                            Timber.v("CoverArtRequest failed %s, from %s", album.getName(), Thread.currentThread().getName());
                                            String url = getBestImage(album, true);
                                            if (!TextUtils.isEmpty(url)) {
                                                return Observable.just(url);
                                            } else {
                                                return Observable.error(new NullPointerException("No image urls for " + album.getName()));
                                            }
                                        }
                                    });
                        } else { // user wants low res go straight for lastfm
                            String url = getBestImage(album, false);
                            if (!TextUtils.isEmpty(url)) {
                                return Observable.just(url);
                            } else {
                                return Observable.error(new NullPointerException("No url for " + album.getName()));
                            }
                        }
                    }
                })
                // remap the url we found into a bitmap
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        return createImageRequestObservable(s, artworkType);
                    }
                });
    }

    public Observable<Bitmap> createArtistNetworkRequest(final ArtInfo artInfo, final ArtworkType artworkType) {
        return createArtistLastFmApiRequestObservable(artInfo)
                .flatMap(new Func1<Artist, Observable<String>>() {
                    @Override
                    public Observable<String> call(Artist artist) {
                        String url = getBestImage(artist, !mPreferences.getBoolean(AppPreferences.WANT_LOW_RESOLUTION_ART, false));
                        if (!TextUtils.isEmpty(url)) {
                            return Observable.just(url);
                        } else {
                            Timber.i("ArtistApiRequest: No image urls for %s", artist.getName());
                            return Observable.error(new NullPointerException("No image urls for " + artist.getName()));
                        }
                    }
                })
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        return createImageRequestObservable(s, artworkType);
                    }
                });
    }

    public Observable<Album> createAlbumLastFmApiRequestObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Album>() {
            @Override
            public void call(final Subscriber<? super Album> subscriber) {
                MusicEntryResponseCallback<Album> listener = new MusicEntryResponseCallback<Album>() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(Album album) {
                        if (subscriber.isUnsubscribed()) return;
                        if (!TextUtils.isEmpty(album.getMbid())) {
                            subscriber.onNext(album);
                            subscriber.onCompleted();
                        } else {
                            Timber.e("Api response does not contain mbid for %s", album.getName());
                            onErrorResponse(new VolleyError("Unknown mbid"));
                        }
                    }
                };
                mVolleyQueue.add(Fetch.albumInfo(artInfo.artistName, artInfo.albumName, listener, Request.Priority.HIGH));
            }
        });
    }

    public Observable<Artist> createArtistLastFmApiRequestObservable(final ArtInfo artInfo) {
        return Observable.create(new Observable.OnSubscribe<Artist>() {
            @Override
            public void call(final Subscriber<? super Artist> subscriber) {
                MusicEntryResponseCallback<Artist> listener = new MusicEntryResponseCallback<Artist>() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }

                    @Override
                    public void onResponse(Artist artist) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(artist);
                        subscriber.onCompleted();
                    }
                };
                mVolleyQueue.add(Fetch.artistInfo(artInfo.artistName, listener, Request.Priority.HIGH));
            }
        });
    }

    public Observable<String> createAlbumCoverArtRequestObservable(final String mbid) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                CoverArtJsonRequest.Listener listener = new CoverArtJsonRequest.Listener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Timber.i("CoverArtRequest:onErrorResponse %s", volleyError);
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(String s) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(s);
                        subscriber.onCompleted();
                    }
                };
                mVolleyQueue.add(new CoverArtJsonRequest(mbid, listener, mGson));
            }
        });
    }

    public Observable<Bitmap> createImageRequestObservable(final String url, final ArtworkType artworkType) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(final Subscriber<? super Bitmap> subscriber) {
                Timber.v("creating ImageRequest %s, from %s", url, Thread.currentThread().getName());
                ArtworkImageRequest.Listener listener = new ArtworkImageRequest.Listener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onError(volleyError);
                    }
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        if (subscriber.isUnsubscribed()) return;
                        subscriber.onNext(bitmap);
                        subscriber.onCompleted();
                    }
                };
                ArtworkImageRequest request = new ArtworkImageRequest(url, artworkType, listener);
                mVolleyQueue.add(request);
            }
        });
    }

    /**
     * Creates a cache key for use with the L1 cache.
     *
     * if both artist and album are null we must use the uri or all
     * requests will return the same cache key, to maintain backwards
     * compat with orpheus versions < 0.5 we use artist,album when we can.
     *
     * if all fields in artinfo are null we cannot fetch any art so an npe will
     * be thrown
     */
    public static String getCacheKey(ArtInfo artInfo, ArtworkType imageType) {
        int size = 0;
        if (artInfo.artistName == null && artInfo.albumName == null) {
            if (artInfo.artworkUri == null) {
                throw new NullPointerException("Cant fetch art with all null fields");
            }
            size += artInfo.artworkUri.toString().length();
            return new StringBuilder(size+12)
                    .append("#").append(imageType).append("#")
                    .append(artInfo.artworkUri.toString())
                    .toString();
        } else {
            size += artInfo.artistName != null ? artInfo.artistName.length() : 4;
            size += artInfo.albumName != null ? artInfo.albumName.length() : 4;
            return new StringBuilder(size+12)
                    .append("#").append(imageType).append("#")
                    .append(artInfo.artistName).append("#")
                    .append(artInfo.albumName)
                    .toString();
        }
    }

    /**
     * @return url string for highest quality image available or null if none
     */
    public static String getBestImage(MusicEntry e, boolean wantHigResArt) {
        for (ImageSize q : ImageSize.values()) {
            if (q.equals(ImageSize.MEGA) && !wantHigResArt) {
                continue;
            }
            String url = e.getImageURL(q);
            if (!TextUtils.isEmpty(url)) {
                Timber.i("Found " + q.toString() + " url for " + e.getName());
                return url;
            }
        }
        return null;
    }

    /**
     *
     */
    public static boolean isLocalArtwork(Uri u) {
        if (u != null) {
            if ("content".equals(u.getScheme())) {
                return true;
            }
        }
        return false;
    }

}