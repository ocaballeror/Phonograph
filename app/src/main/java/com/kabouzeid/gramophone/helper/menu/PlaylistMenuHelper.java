package com.kabouzeid.gramophone.helper.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog;
import com.kabouzeid.gramophone.dialogs.DeletePlaylistDialog;
import com.kabouzeid.gramophone.dialogs.RenamePlaylistDialog;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.loader.PlaylistSongLoader;
import com.kabouzeid.gramophone.model.AbsCustomPlaylist;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.PlaylistSong;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.util.PlaylistsUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistMenuHelper {
    @SuppressLint("StaticFieldLeak")
    public static boolean handleMenuClick(@NonNull AppCompatActivity activity, @NonNull final Playlist playlist, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play:
                MusicPlayerRemote.openQueue(new ArrayList<>(getPlaylistSongs(activity, playlist)), 0, true);
                return true;
            case R.id.action_play_next:
                MusicPlayerRemote.playNext(new ArrayList<>(getPlaylistSongs(activity, playlist)));
                return true;
            case R.id.action_add_to_current_playing:
                MusicPlayerRemote.enqueue(new ArrayList<>(getPlaylistSongs(activity, playlist)));
                return true;
            case R.id.action_add_to_playlist:
                AddToPlaylistDialog.create(new ArrayList<>(getPlaylistSongs(activity, playlist))).show(activity.getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
            case R.id.action_rename_playlist:
                RenamePlaylistDialog.create(playlist.id).show(activity.getSupportFragmentManager(), "RENAME_PLAYLIST");
                return true;
            case R.id.action_delete_playlist:
                DeletePlaylistDialog.create(playlist).show(activity.getSupportFragmentManager(), "DELETE_PLAYLIST");
                return true;
            case R.id.action_remove_duplicates:
                List<Song> songs = new ArrayList<>(getPlaylistSongs(activity, playlist));
                List<PlaylistSong> toRemove = new ArrayList<>();
                Collections.sort(songs,new Comparator<Song>(){
                    @Override
                    public int compare(Song song, Song t1) {
                        return song.id >= t1.id? 1:-1;
                    }
                });

                for (int i = 0; i < songs.size(); i++) {
                    if(i < songs.size() -1 && songs.get(i).id == songs.get(i+1).id){
                        toRemove.add((PlaylistSong)songs.get(i+1));
                    }
                }
                String toastTxt = "Removed " + toRemove.size() + " duplicates";
                final Toast toastDups = Toast.makeText(activity, toastTxt, Toast.LENGTH_SHORT);
                if(toRemove.size() > 0){
                    PlaylistsUtil.removeFromPlaylist(activity.getApplicationContext(), toRemove);
                }
                toastDups.show();
                break;

            case R.id.action_save_playlist:
                @SuppressLint("ShowToast")
                final Toast toast = Toast.makeText(activity, R.string.saving_to_file, LENGTH_SHORT);
                new AsyncTask<Context, Void, String>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        toast.show();
                    }

                    @Override
                    protected String doInBackground(Context... params) {
                        try {
                            return String.format(params[0].getString(R.string.saved_playlist_to), PlaylistsUtil.savePlaylist(params[0], playlist));
                        } catch (IOException e) {
                            e.printStackTrace();
                            return String.format(params[0].getString(R.string.failed_to_save_playlist), e);
                        }
                    }

                    @Override
                    protected void onPostExecute(String string) {
                        super.onPostExecute(string);
                        if (toast != null) {
                            toast.setText(string);
                            toast.show();
                        }
                    }
                }.execute(activity.getApplicationContext());
                return true;
        }
        return false;
    }

    @NonNull
    private static ArrayList<? extends Song> getPlaylistSongs(@NonNull Activity activity, Playlist playlist) {
        return playlist instanceof AbsCustomPlaylist ?
                ((AbsCustomPlaylist) playlist).getSongs(activity) :
                PlaylistSongLoader.getPlaylistSongList(activity, playlist.id);
    }
}
