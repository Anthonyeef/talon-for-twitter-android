package com.klinker.android.twitter_l.activities.media_viewer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.TalonPhotoViewAttacher;
import com.klinker.android.twitter_l.utils.TimeoutThread;

import uk.co.senab.photoview.PhotoViewAttacher;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class PhotoFragment extends Fragment {

    public PhotoPagerActivity activity;

    public static PhotoFragment getInstance(String s) {
        Bundle b = new Bundle();
        b.putString("url", s);

        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(b);

        return fragment;
    }
    
    String url;
    ImageView picture;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        activity = (PhotoPagerActivity) getActivity();

        Bundle args = getArguments();
        url = args.getString("url");

        final View root = inflater.inflate(R.layout.photo_dialog_layout, container, false);

        picture = (ImageView) root.findViewById(R.id.picture);
        picture.setPadding(0,0,0,0);

        root.findViewById(R.id.share_button).setVisibility(View.GONE);
        root.findViewById(R.id.save_button).setVisibility(View.GONE);
        root.findViewById(R.id.info_button).setVisibility(View.GONE);
        root.findViewById(R.id.buttons_layout).setVisibility(View.INVISIBLE);

        final TalonPhotoViewAttacher mAttacher = new TalonPhotoViewAttacher(picture);
        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                if (activity.sysUiShown) {
                    activity.hideSystemUI();
                } else {
                    activity.showSystemUI();
                }
            }
        });

        Glide.with(getActivity()).load(url).dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new SimpleTarget<GlideDrawable>() {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                picture.setImageDrawable(resource);

                LinearLayout spinner = (LinearLayout) root.findViewById(R.id.list_progress);
                spinner.setVisibility(View.GONE);

                mAttacher.update();
            }
        });

        return root;
    }

    public void saveImage() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(activity)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(getResources().getString(R.string.downloading) + "...")
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText(getResources().getString(R.string.saving_picture) + "...")
                                    .setProgress(100, 100, true);

                    NotificationManager mNotificationManager =
                            (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());

                    String urlString = url;

                    if (urlString.contains("pbs.twimg") && !(urlString.contains("profile_image") || urlString.contains("profile_banner"))) {
                        urlString += ":orig";
                    }

                    HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                    InputStream is = new BufferedInputStream(conn.getInputStream());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;

                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    Random generator = new Random();
                    int n = 1000000;
                    n = generator.nextInt(n);
                    String fname = "Image-" + n;

                    Uri uri = IOUtils.saveImage(bitmap, fname, getActivity());
                    String root = Environment.getExternalStorageDirectory().toString();
                    File myDir = new File(root + "/Talon");
                    File file = new File(myDir, fname + ".jpg");

                    try {
                        uri = IOUtils.getImageContentUri(getActivity(), file);
                    } catch (Exception e) {

                    }

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "image/*");

                    bitmap.recycle();

                    PendingIntent pending = PendingIntent.getActivity(activity, 91, intent, 0);

                    mBuilder =
                            new NotificationCompat.Builder(activity)
                                    .setContentIntent(pending)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(getResources().getString(R.string.saved_picture) + "...")
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText(getResources().getString(R.string.saved_picture) + "!");

                    mNotificationManager.notify(6, mBuilder.build());
                } catch (final Exception e) {
                    e.printStackTrace();

                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    new PermissionModelUtils(activity).showStorageIssue(e);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    try {
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(activity)
                                        .setSmallIcon(R.drawable.ic_stat_icon)
                                        .setTicker(getResources().getString(R.string.error) + "...")
                                        .setContentTitle(getResources().getString(R.string.app_name))
                                        .setContentText(getResources().getString(R.string.error) + "...")
                                        .setProgress(0, 100, true);

                        NotificationManager mNotificationManager =
                                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(6, mBuilder.build());
                    } catch (Exception x) {
                        // not attached
                    }

                }
            }
        }).start();
    }

    public void shareImage() {
        if (picture == null) {
            return;
        }

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = Glide.with(getActivity())
                            .load(url)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // create the intent
                                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                                sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                sharingIntent.setType("image/*");

                                // add the bitmap uri to the intent
                                Uri uri = getImageUri(activity, bitmap);
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

                                // start the chooser
                                startActivity(Intent.createChooser(sharingIntent, getString(R.string.menu_share) + ": "));
                            }
                        });
                    }
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory() + "/Talon/image_to_share.jpg");
        File dir = new File(Environment.getExternalStorageDirectory(), "Talon");
        try {
            if (!dir.exists())
                dir.mkdirs();
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());

            return IOUtils.getImageContentUri(inContext, f);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}