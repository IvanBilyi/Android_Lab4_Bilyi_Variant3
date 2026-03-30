package com.example.lab4_bilyi_variant3;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private FrameLayout videoContainer;
    private EditText urlEditText;
    private LinearLayout audioInfoLayout;
    private ImageView albumArtImageView;
    private TextView tvTitle, tvArtist, tvAlbum;
    private MediaPlayer mediaPlayer;

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) playVideo(uri); });

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) playAudio(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        videoContainer = findViewById(R.id.videoContainer);
        urlEditText = findViewById(R.id.urlEditText);
        audioInfoLayout = findViewById(R.id.audioInfoLayout);
        albumArtImageView = findViewById(R.id.albumArtImageView);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvAlbum = findViewById(R.id.tvAlbum);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoContainer);
        videoView.setMediaController(mediaController);

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(MainActivity.this, "Помилка відтворення відео!", Toast.LENGTH_SHORT).show();
            stopAllMedia();
            return true;
        });

        findViewById(R.id.btnPickVideo).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        findViewById(R.id.btnPickAudio).setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));

        findViewById(R.id.btnPlayVideoUrl).setOnClickListener(v -> handleUrlPlay(true));
        findViewById(R.id.btnPlayAudioUrl).setOnClickListener(v -> handleUrlPlay(false));

        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) mediaPlayer.start();
        });
        findViewById(R.id.btnPause).setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        });
        findViewById(R.id.btnStop).setOnClickListener(v -> stopAllMedia());
    }
    private void handleUrlPlay(boolean isVideo) {
        String url = urlEditText.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Спочатку введіть посилання!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isVideo) {
            playVideo(Uri.parse(url));
        } else {
            playAudio(Uri.parse(url));
        }
    }
    private void stopAllMedia() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        videoView.stopPlayback();
        audioInfoLayout.setVisibility(View.GONE);
        videoContainer.setVisibility(View.GONE);
    }
    private void playVideo(Uri uri) {
        stopAllMedia();

        videoContainer.setVisibility(View.VISIBLE);

        try {
            videoView.setVideoURI(uri);
            videoView.requestFocus();
            videoView.setOnPreparedListener(mp -> videoView.start());
        } catch (Exception e) {
            Toast.makeText(this, "Критична помилка відео", Toast.LENGTH_SHORT).show();
            stopAllMedia();
        }
    }

    private void playAudio(Uri uri) {
        stopAllMedia();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(MainActivity.this, "Помилка відтворення аудіо", Toast.LENGTH_SHORT).show();
                stopAllMedia();
                return true;
            });

            mediaPlayer.prepareAsync();
            Toast.makeText(this, "Завантаження...", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                extractMetadata(uri);
            });

        } catch (Exception e) {
            Toast.makeText(this, "Помилка ініціалізації аудіо", Toast.LENGTH_SHORT).show();
            stopAllMedia();
        }
    }

    // HARD TASK витягування метаданих
    private void extractMetadata(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            String scheme = uri.getScheme();
            if (scheme != null && (scheme.startsWith("http"))) {
                retriever.setDataSource(uri.toString(), new HashMap<>());
            } else {
                retriever.setDataSource(this, uri);
            }

            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            tvTitle.setText("Назва: " + (title != null ? title : "Невідомий трек"));
            tvArtist.setText("Виконавець: " + (artist != null ? artist : "Невідомий артист"));
            tvAlbum.setText("Альбом: " + (album != null ? album : "Невідомо"));

            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                albumArtImageView.setImageBitmap(bitmap);
            } else {
                albumArtImageView.setImageResource(android.R.drawable.ic_media_play);
            }

            audioInfoLayout.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            audioInfoLayout.setVisibility(View.GONE);
        } finally {
            try { retriever.release(); } catch (Exception ignore) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllMedia();
    }
}