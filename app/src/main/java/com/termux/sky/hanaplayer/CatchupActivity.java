package com.termux.sky.hanaplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.sky.play_master.ExoPlayerActivityDRM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatchupActivity extends AppCompatActivity {

    private String channelId;
    private String channelName;
    private String channelLogo;
    private String channelUrl;
    private String pluginPort;

    private RecyclerView programsRecycler;
    private LinearLayout dateContainer;
    private List<TextView> dateViews = new ArrayList<>();

    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        channelId = getIntent().getStringExtra("channel_id");
        channelName = getIntent().getStringExtra("channel_name");
        channelLogo = getIntent().getStringExtra("channel_logo");
        channelUrl = getIntent().getStringExtra("channel_url");
        pluginPort = getIntent().getStringExtra("plugin_port");

        density = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0E1628"));
        root.setPadding((int) (16 * density), (int) (16 * density), (int) (16 * density), (int) (16 * density));

        
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        Glide.with(this).load(channelLogo).placeholder(R.drawable.tx_broken_image).into(logo);
        header.addView(logo, new LinearLayout.LayoutParams((int) (48 * density), (int) (48 * density)));

        TextView title = new TextView(this);
        title.setText(channelName + " (Catchup)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding((int) (12 * density), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnWatchLive = new Button(this);
        btnWatchLive.setText("▶ Live");
        btnWatchLive.setTextColor(Color.BLACK);
        btnWatchLive.setFocusable(true);
        btnWatchLive.setClickable(true);
        btnWatchLive.setBackground(createButtonFocusBackground());
        btnWatchLive.setOnClickListener(v -> launchLiveStream());
        header.addView(btnWatchLive);

        root.addView(header);

        
        HorizontalScrollView dateScroll = new HorizontalScrollView(this);
        dateScroll.setPadding(0, (int) (16 * density), 0, (int) (12 * density));
        dateScroll.setHorizontalScrollBarEnabled(false);

        dateContainer = new LinearLayout(this);
        dateContainer.setOrientation(LinearLayout.HORIZONTAL);
        dateScroll.addView(dateContainer);
        root.addView(dateScroll);

        setupDateChips();

        
        programsRecycler = new RecyclerView(this);
        programsRecycler.setLayoutManager(new LinearLayoutManager(this));

        
        programsRecycler.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        root.addView(programsRecycler, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
    }

    private StateListDrawable createTvFocusBackground(boolean isSelected) {
        StateListDrawable states = new StateListDrawable();

        
        GradientDrawable focusedState = new GradientDrawable();
        focusedState.setColor(isSelected ? Color.parseColor("#D4AF37") : Color.parseColor("#33FFFFFF"));
        focusedState.setCornerRadius(8f * density);
        focusedState.setStroke((int)(2 * density), Color.parseColor("#FFD700"));

        
        GradientDrawable defaultState = new GradientDrawable();
        defaultState.setColor(isSelected ? Color.parseColor("#FFD700") : Color.parseColor("#1C2436"));
        defaultState.setCornerRadius(8f * density);

        states.addState(new int[]{android.R.attr.state_focused}, focusedState);
        states.addState(new int[]{}, defaultState);
        return states;
    }

    private StateListDrawable createButtonFocusBackground() {
        StateListDrawable states = new StateListDrawable();

        
        GradientDrawable focusedState = new GradientDrawable();
        focusedState.setColor(Color.parseColor("#D4AF37"));
        focusedState.setCornerRadius(8f * density);
        focusedState.setStroke((int)(2 * density), Color.parseColor("#FFD700"));

        
        GradientDrawable defaultState = new GradientDrawable();
        defaultState.setColor(Color.parseColor("#FFD700"));
        defaultState.setCornerRadius(8f * density);

        states.addState(new int[]{android.R.attr.state_focused}, focusedState);
        states.addState(new int[]{}, defaultState);
        return states;
    }

    private void setupDateChips() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            final int dayOffset = -i;
            TextView chip = new TextView(this);
            chip.setText(i == 0 ? "Today" : (i == 1 ? "Yesterday" : dayFormat.format(cal.getTime())));
            chip.setTextSize(14);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding((int)(16 * density), (int)(8 * density), (int)(16 * density), (int)(8 * density));
            chip.setFocusable(true);
            chip.setClickable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, (int)(8 * density), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                updateDateSelection(dayOffset);
                loadCatchupPrograms(dayOffset);
            });

            dateViews.add(chip);
            dateContainer.addView(chip);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        
        updateDateSelection(0);
        loadCatchupPrograms(0);
    }

    private void updateDateSelection(int selectedOffset) {
        for (int i = 0; i < dateViews.size(); i++) {
            int currentOffset = -i;
            TextView chip = dateViews.get(i);
            boolean isSelected = (currentOffset == selectedOffset);
            chip.setBackground(createTvFocusBackground(isSelected));
            chip.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
        }
    }

    private void loadCatchupPrograms(int dayOffset) {
        new Thread(() -> {
            try {
                String urlString = "http://localhost:" + pluginPort + "/epg/" + channelId + "/" + dayOffset;
                Log.d("CatchupDebug", "Requesting URL: " + urlString); 

                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                
                conn.setUseCaches(false);
                conn.setRequestProperty("Cache-Control", "no-cache");

                InputStreamReader reader = new InputStreamReader(conn.getInputStream());

                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                String rawJson = sb.toString();
                Log.d("CatchupDebug", "Response for offset " + dayOffset + ": " + rawJson.substring(0, Math.min(rawJson.length(), 200)));

                EpgResponse response = new Gson().fromJson(rawJson, EpgResponse.class);
                reader.close();

                long currentTime = System.currentTimeMillis();
                List<EpgProgram> parsedEpg = new ArrayList<>();
                List<EpgProgram> finalEpg = new ArrayList<>();

                if (response != null && response.epg != null) {
                    for (EpgProgram program : response.epg) {
                        long start = program.startEpoch < 100000000000L ? program.startEpoch * 1000 : program.startEpoch;
                        long end = program.endEpoch < 100000000000L ? program.endEpoch * 1000 : program.endEpoch;
                        program.startEpoch = start;
                        program.endEpoch = end;

                        
                        if (start <= currentTime) {
                            parsedEpg.add(program);
                        }
                    }
                }

                if (dayOffset == 0) {
                    EpgProgram liveShow = null;
                    for (EpgProgram p : parsedEpg) {
                        if (currentTime >= p.startEpoch && currentTime <= p.endEpoch) {
                            liveShow = p;
                            break;
                        }
                    }

                    if (liveShow != null) {
                        finalEpg.add(liveShow);
                        for (int i = parsedEpg.size() - 1; i >= 0; i--) {
                            if (parsedEpg.get(i).srno != liveShow.srno) {
                                finalEpg.add(parsedEpg.get(i));
                            }
                        }
                    } else {
                        EpgProgram fallbackLive = createFallbackLive(currentTime);
                        finalEpg.add(fallbackLive);
                        for (int i = parsedEpg.size() - 1; i >= 0; i--) {
                            finalEpg.add(parsedEpg.get(i));
                        }
                    }
                } else {
                    for (int i = parsedEpg.size() - 1; i >= 0; i--) {
                        finalEpg.add(parsedEpg.get(i));
                    }
                }

                if (finalEpg.isEmpty()) {
                    finalEpg.add(createFallbackLive(currentTime));
                }

                runOnUiThread(() -> programsRecycler.setAdapter(new CatchupAdapter(finalEpg)));

            } catch (Exception e) {
                Log.e("CatchupActivity", "Error fetching EPG: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(CatchupActivity.this, "Error fetching EPG", Toast.LENGTH_SHORT).show();
                    List<EpgProgram> fallbackList = new ArrayList<>();
                    fallbackList.add(createFallbackLive(System.currentTimeMillis()));
                    programsRecycler.setAdapter(new CatchupAdapter(fallbackList));
                });
            }
        }).start();
    }

    private EpgProgram createFallbackLive(long currentTime) {
        EpgProgram liveTvProgram = new EpgProgram();
        liveTvProgram.srno = -1L;
        liveTvProgram.showId = "live_fallback";
        liveTvProgram.showtime = "LIVE";
        liveTvProgram.showname = "LIVE TV";
        liveTvProgram.description = "Watch Live Stream";
        liveTvProgram.duration = 0;
        liveTvProgram.endtime = "";
        liveTvProgram.channel_name = channelName;
        liveTvProgram.startEpoch = currentTime - 1000;
        liveTvProgram.endEpoch = currentTime + (3600 * 1000);
        return liveTvProgram;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void resolveAndPlayCatchup(EpgProgram program) {
        long currentTime = System.currentTimeMillis();
        boolean isLive = currentTime >= program.startEpoch && currentTime <= program.endEpoch;

        if (isLive) {
            launchLiveStream();
            return;
        }

        Toast.makeText(this, "Resolving Stream...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String cleanHtml = getString(program);

                String playUrlMatch = null;
                String licenseUrlMatch = null;

                Matcher pMatcher = Pattern.compile("player\\.load\\(\\s*[\"']([^\"']+)[\"']\\s*\\)").matcher(cleanHtml);
                if (pMatcher.find()) playUrlMatch = pMatcher.group(1);

                if (playUrlMatch == null) {
                    Matcher hlsMatcher = Pattern.compile("src:\\s*[\"']([^\"']+)[\"']").matcher(cleanHtml);
                    if (hlsMatcher.find()) playUrlMatch = hlsMatcher.group(1);
                }

                Matcher lMatcher = Pattern.compile("const\\s+licenseUrl\\s*=\\s*[\"']([^\"']+)[\"']").matcher(cleanHtml);
                if (lMatcher.find()) licenseUrlMatch = lMatcher.group(1);

                if (playUrlMatch != null) {
                    String localBase = "http://localhost:" + pluginPort;
                    final String absolutePlayUrl = playUrlMatch.startsWith("/") ? localBase + playUrlMatch : playUrlMatch;

                    String absoluteLicenseUrl = null;
                    if (licenseUrlMatch != null && !licenseUrlMatch.trim().isEmpty()) {
                        absoluteLicenseUrl = licenseUrlMatch.startsWith("/") ? localBase + licenseUrlMatch : licenseUrlMatch;
                    }

                    final String finalLicense = absoluteLicenseUrl;

                    runOnUiThread(() -> {
                        Intent intent = new Intent(CatchupActivity.this, ExoPlayerActivityDRM.class)
                            .putExtra("url", absolutePlayUrl)
                            .putExtra("name", "[Catchup] " + program.showname)
                            .putExtra("logo_url", channelLogo)
                            .putExtra("plugin_port", pluginPort);

                        
                        if (finalLicense != null && !finalLicense.isEmpty()) {
                            intent.putExtra("license_key", finalLicense);
                        } else {
                            String fallbackLicenseUrl = "http://localhost:" + pluginPort + "//live/key/" + channelId;
                            intent.putExtra("license_key", fallbackLicenseUrl);
                        }

                        String userAgent = getIntent().getStringExtra("user_agent");
                        if (userAgent != null && !userAgent.isEmpty()) {
                            intent.putExtra("user_agent", userAgent);
                        }

                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(CatchupActivity.this, "Could not resolve stream URL", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("CatchupActivity", "Error resolving stream: ", e);
                runOnUiThread(() -> Toast.makeText(CatchupActivity.this, "Error resolving catchup.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @NonNull
    private String getString(EpgProgram program) throws IOException {
        String videoUrl = "http://localhost:" + pluginPort + "/catchup/render/" + channelId + "?start=" + program.startEpoch + "&end=" + program.endEpoch + "&srno=" + program.srno;
        HttpURLConnection connection = (HttpURLConnection) new URL(videoUrl).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder htmlBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            htmlBuilder.append(line);
        }
        reader.close();

        return htmlBuilder.toString().replace("\\u0026", "&").replace("\\/", "/");
    }

    @OptIn(markerClass = UnstableApi.class)
    private void launchLiveStream() {
        Intent intent = new Intent(this, ExoPlayerActivityDRM.class)
            .putExtra("url", channelUrl)
            .putExtra("name", channelName)
            .putExtra("logo_url", channelLogo)
            .putExtra("plugin_port", pluginPort);

        
        String license = getIntent().getStringExtra("license_key");

        
        if (license != null && !license.isEmpty()) {
            intent.putExtra("license_key", license);
        } else {
            String fallbackLicenseUrl = "http://localhost:" + pluginPort + "//live/key/" + channelId;
            intent.putExtra("license_key", fallbackLicenseUrl);
        }

        String userAgent = getIntent().getStringExtra("user_agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            intent.putExtra("user_agent", userAgent);
        }

        startActivity(intent);
    }

    
    public static class EpgResponse {
        public List<EpgProgram> epg;
    }

    public static class EpgProgram {
        public long srno;
        public String showId;
        public String showtime;
        public String showname;
        public String description;
        public int duration;
        public String endtime;
        public String channel_name;
        public String episodeThumbnail;
        public String episodePoster;
        public long startEpoch;
        public long endEpoch;
    }

    
    private class CatchupAdapter extends RecyclerView.Adapter<CatchupAdapter.VH> {
        private final List<EpgProgram> programs;
        private final long currentTime = System.currentTimeMillis();

        public CatchupAdapter(List<EpgProgram> programs) {
            this.programs = programs;
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CardView card = new CardView(parent.getContext());
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins((int)(8 * density), (int)(6 * density), (int)(8 * density), (int)(6 * density));
            card.setLayoutParams(layoutParams);
            card.setRadius(12f * density);
            card.setCardElevation(2f * density);
            card.setCardBackgroundColor(Color.parseColor("#1C2436"));

            
            card.setFocusable(true);
            card.setClickable(true);

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding((int)(12 * density), (int)(12 * density), (int)(12 * density), (int)(12 * density));
            row.setGravity(Gravity.CENTER_VERTICAL);

            
            FrameLayout posterFrame = new FrameLayout(parent.getContext());
            posterFrame.setLayoutParams(new LinearLayout.LayoutParams((int)(90 * density), (int)(60 * density)));

            ImageView poster = new ImageView(parent.getContext());
            poster.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
            poster.setBackgroundColor(Color.parseColor("#0E1628"));
            posterFrame.addView(poster);

            TextView liveBadge = new TextView(parent.getContext());
            liveBadge.setText("LIVE");
            liveBadge.setTextColor(Color.WHITE);
            liveBadge.setTextSize(9);
            liveBadge.setTypeface(null, Typeface.BOLD);
            liveBadge.setBackgroundColor(Color.RED);
            liveBadge.setPadding((int)(4 * density), (int)(2 * density), (int)(4 * density), (int)(2 * density));
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2);
            badgeParams.gravity = Gravity.TOP | Gravity.START;
            badgeParams.setMargins((int)(4 * density), (int)(4 * density), 0, 0);
            liveBadge.setLayoutParams(badgeParams);
            liveBadge.setVisibility(View.GONE);
            posterFrame.addView(liveBadge);

            row.addView(posterFrame);

            
            LinearLayout textCol = new LinearLayout(parent.getContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding((int)(16 * density), 0, 0, 0);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView showName = new TextView(parent.getContext());
            showName.setTextColor(Color.WHITE);
            showName.setTextSize(15);
            showName.setTypeface(null, Typeface.BOLD);
            showName.setMaxLines(1);
            showName.setEllipsize(TextUtils.TruncateAt.END);

            TextView showDesc = new TextView(parent.getContext());
            showDesc.setTextColor(Color.parseColor("#AAAAAA"));
            showDesc.setTextSize(12);
            showDesc.setMaxLines(2);
            showDesc.setEllipsize(TextUtils.TruncateAt.END);

            TextView showTime = new TextView(parent.getContext());
            showTime.setTextColor(Color.parseColor("#FFD700"));
            showTime.setTextSize(12);
            showTime.setPadding(0, (int)(4 * density), 0, 0);

            textCol.addView(showName);
            textCol.addView(showDesc);
            textCol.addView(showTime);
            row.addView(textCol);

            
            View focusBorder = new View(parent.getContext());
            focusBorder.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            GradientDrawable borderDrawable = new GradientDrawable();
            borderDrawable.setShape(GradientDrawable.RECTANGLE);
            borderDrawable.setCornerRadius(12f * density);
            borderDrawable.setStroke((int) (3 * density), Color.parseColor("#FFD700"));
            borderDrawable.setColor(Color.TRANSPARENT);
            focusBorder.setBackground(borderDrawable);
            focusBorder.setAlpha(0f);

            FrameLayout rootFrame = new FrameLayout(parent.getContext());
            rootFrame.addView(row);
            rootFrame.addView(focusBorder);
            card.addView(rootFrame);

            return new VH(card, poster, liveBadge, showName, showDesc, showTime, focusBorder);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            EpgProgram program = programs.get(position);
            holder.showName.setText(program.showname);

            if (program.description != null && !program.description.isEmpty()) {
                holder.showDesc.setText(program.description);
                holder.showDesc.setVisibility(View.VISIBLE);
            } else {
                holder.showDesc.setVisibility(View.GONE);
            }

            holder.showTime.setText(program.showtime + " - " + program.endtime);

            boolean isLive = currentTime >= program.startEpoch && currentTime <= program.endEpoch;
            holder.liveBadge.setVisibility(isLive ? View.VISIBLE : View.GONE);

            if (program.episodePoster != null && !program.episodePoster.isEmpty()) {
                String imgUrl = "http://localhost:" + pluginPort + "/jtvposter/" + program.episodePoster;
                Glide.with(holder.itemView.getContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.tx_broken_image)
                    .into(holder.poster);
            } else {
                holder.poster.setImageResource(R.drawable.tx_play_exo);
                holder.poster.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            holder.itemView.setOnClickListener(v -> resolveAndPlayCatchup(program));

            
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float targetScale = hasFocus ? 1.03f : 1.0f;
                v.animate().scaleX(targetScale).scaleY(targetScale).setDuration(150).start();

                holder.focusBorder.animate().alpha(hasFocus ? 1f : 0f).setDuration(150).start();

                if (hasFocus) {
                    holder.card.setCardBackgroundColor(Color.parseColor("#252F47"));
                } else {
                    holder.card.setCardBackgroundColor(Color.parseColor("#1C2436"));
                }
            });
        }

        @Override
        public int getItemCount() { return programs.size(); }

        class VH extends RecyclerView.ViewHolder {
            CardView card;
            ImageView poster;
            TextView liveBadge, showName, showDesc, showTime;
            View focusBorder;

            VH(View v, ImageView poster, TextView liveBadge, TextView showName, TextView showDesc, TextView showTime, View focusBorder) {
                super(v);
                this.card = (CardView) v;
                this.poster = poster;
                this.liveBadge = liveBadge;
                this.showName = showName;
                this.showDesc = showDesc;
                this.showTime = showTime;
                this.focusBorder = focusBorder;
            }
        }
    }
}
