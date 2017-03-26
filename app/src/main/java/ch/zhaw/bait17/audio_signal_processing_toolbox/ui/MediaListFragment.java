package ch.zhaw.bait17.audio_signal_processing_toolbox.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import ch.zhaw.bait17.audio_signal_processing_toolbox.R;
import ch.zhaw.bait17.audio_signal_processing_toolbox.TrackAdapter;
import ch.zhaw.bait17.audio_signal_processing_toolbox.model.SupportedAudioFormat;
import ch.zhaw.bait17.audio_signal_processing_toolbox.model.Track;

/**
 * @author georgrem, stockan1
 */

public class MediaListFragment extends Fragment {

    private static final String TAG = MediaListFragment.class.getSimpleName();
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    public final static String KEY_TRACK = "ch.zhaw.bait17.audio_signal_processing_toolbox.TRACK";
    public final static String KEY_TRACKS = "ch.zhaw.bait17.audio_signal_processing_toolbox.TRACKS";

    private  View rootView;
    private Context context;
    private OnTrackSelectedListener listener;

    public interface OnTrackSelectedListener {
        void onTrackSelected(List<Track> tracks, int trackPos);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView =  inflater.inflate(R.layout.media_list_view, container, false);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        Activity activity;
        if (context instanceof Activity) {
            activity = (Activity) context;
            listener = (OnTrackSelectedListener) activity;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement MediaListFragment.OnItemSelectedListener");
        }
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        loadTrackList();
    }

    // During startup, check if there are arguments passed to the fragment.
    // onStart is a good place to do this because the layout has already been
    // applied to the fragment at this point
    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if (args != null) {

        }
    }

    /*
        This method is not being called by requestReadExternalStoragePermission().
        It requires API level 21 and above.
        Needs a fix to work around.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadTrackList();
        } else {
            Toast.makeText(context, "You don't have permission to read from external storage.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTrackList() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            final List<Track> tracks = getAllTracks();
            Collections.sort(tracks, new Comparator<Track>() {
                public int compare(Track a, Track b) {
                    return a.getTitle().compareTo(b.getTitle());
                }
            });

            final ListView listView = (ListView) rootView.findViewById(R.id.media_list);
            TrackAdapter trackAdapter = new TrackAdapter((ArrayList) tracks);
            listView.setAdapter(trackAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (listener != null) {
                        Track track = (Track) adapterView.getItemAtPosition(i);
                        int trackPosNr = tracks.indexOf(track);
                        listener.onTrackSelected(tracks, trackPosNr);
                    }
                }
            });
        } else {
            requestReadExternalStoragePermission();
        }
    }

    private List<Track> getAllTracks() {
        List<Track> allTracks = new ArrayList<>();
        allTracks.addAll(getTracksFromRawFolder());
        allTracks.addAll(getTracksFromDevice());
        return allTracks;
    }

    private List<Track> getTracksFromRawFolder() {
        List<Track> tracksInRaw = new ArrayList<>();
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            int rawId = getResources().getIdentifier(field.getName(), "raw", context.getPackageName());
            if (rawId != 0) {
                TypedValue value = new TypedValue();
                getResources().getValue(rawId, value, true);
                String[] s = value.string.toString().split("/");
                String filename = s[s.length - 1];
                if (filename.endsWith(SupportedAudioFormat.WAVE.toString())
                        || filename.endsWith(SupportedAudioFormat.MP3.toString())) {
                    Log.i("filename", filename);
                    filename = filename.split("\\.")[0];
                    tracksInRaw.add(getTrack(rawId, filename));
                }
            }
        }
        return tracksInRaw;
    }

    private Track getTrack(int resId, String filename) {
        String title, artist, album, duration, mimeType;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String uri = "android.resource://" + context.getPackageName()
                + File.separator + resId;
        try {
            mmr.setDataSource(context, Uri.parse(uri));
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        title = title == null ? filename : title;
        artist = artist == null ? "<unknown>" : artist;
        album = album == null ? "<unknown>" : album;
        SupportedAudioFormat audioFormat = SupportedAudioFormat.getSupportedAudioFormat(mimeType);
        return new Track(title, artist, album, duration, uri, audioFormat);
    }

    private List<Track> getTracksFromDevice() {
        List<Track> tracksOnDevice = new ArrayList<>();
        Cursor musicCursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int nameColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int mimeTypeColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            do {
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);
                String album = musicCursor.getString(albumColumn);
                String duration = musicCursor.getString(durationColumn);
                String file = "file:///" + musicCursor.getString(nameColumn);
                String mimeType = musicCursor.getString(mimeTypeColumn);
                SupportedAudioFormat audioFormat = SupportedAudioFormat.getSupportedAudioFormat(mimeType);
                if (audioFormat != SupportedAudioFormat.unknown)
                    tracksOnDevice.add(new Track(title, artist, album, duration, file, audioFormat));
            }
            while (musicCursor.moveToNext());
        }
        if (musicCursor != null) musicCursor.close();
        return tracksOnDevice;
    }

    private void requestReadExternalStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar.make(rootView.findViewById(R.id.media_list),
                    "Read permission to external storage is required in order to access your audio files.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_EXTERNAL_STORAGE);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

}