package com.termux.tv_ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MovieList {

    public static final String[] MOVIE_CATEGORY = {"Entertainment", "Movies", "Sports", "News", "Kids"};

    public static List<Movie> getList(Context context,String cat) {
        return setupMovies(context,cat);
    }

    private static List<Movie> setupMovies(Context context, String cat) {
        List<Movie> list;

        try {
            list = grok_Movies(context,cat);
            if (list.isEmpty()) {
                throw new Exception("No data in SharedPreferences");
            }
        } catch (Exception e) {
            Log.e("MovieList", "Error fetching movies from SharedPreferences: " + e.getMessage());
            list = setupStaticMovies();
        }

        return list;
    }



    private static List<Movie> grok_Movies(Context context, String cat) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences("ChannelsData", Context.MODE_PRIVATE);
//        PlaylistParser parser = new PlaylistParser(sharedPreferences);
//
//        Map<String, List<String>> channelDetails = parser.getChannelDetailsByGroup(cat);
//
//        List<String> names = channelDetails.get("names");
//        List<String> logos = channelDetails.get("logos");
//        List<String> urls = channelDetails.get("urls");
//
//        List<Movie> movies = new ArrayList<>();
//
//        //ADD SKYPREF GETTING FOR VISUAL BLISS
//
//        for (int i = 0; i < names.size(); i++) {
//            movies.add(buildMovieInfo(names.get(i), "Description for " + names.get(i), "",
//                    urls.get(i), logos.get(i), "")); // Adjust background URL if needed
//            System.out.println("names: " + names.get(i));
//            System.out.println("zzzLogo: " + logos.get(i));
//            System.out.println("zzzURL: " + urls.get(i));
//        }
//
//        return movies;
        return java.util.Collections.emptyList();
    }



    private static List<Movie> fetchMoviesFromSharedPreferences(SharedPreferences sharedPreferences) {
        List<Movie> movieList = new ArrayList<>();
        Gson gson = new Gson();
        String json = sharedPreferences.getString("movie_list", null);

        if (json != null) {
            Type movieListType = new TypeToken<List<Movie>>(){}.getType();
            movieList = gson.fromJson(json, movieListType);
        }

        return movieList;
    }

    private static List<Movie> setupStaticMovies() {
        List<Movie> list = new ArrayList<>();

        // Entertainment Category
        list.add(buildMovieInfo("Entertainment 1", "Description for Entertainment 1", "Studio One",
                "https://example.com/entertainment1.mp4", "https://example.com/entertainment1_card.jpg",
                "https://example.com/entertainment1_bg.jpg"));
        list.add(buildMovieInfo("Entertainment 2", "Description for Entertainment 2", "Studio Two",
                "https://example.com/entertainment2.mp4", "https://example.com/entertainment2_card.jpg",
                "https://example.com/entertainment2_bg.jpg"));
        list.add(buildMovieInfo("Entertainment 3", "Description for Entertainment 3", "Studio Three",
                "https://example.com/entertainment3.mp4", "https://example.com/entertainment3_card.jpg",
                "https://example.com/entertainment3_bg.jpg"));
        list.add(buildMovieInfo("Entertainment 4", "Description for Entertainment 4", "Studio Four",
                "https://example.com/entertainment4.mp4", "https://example.com/entertainment4_card.jpg",
                "https://example.com/entertainment4_bg.jpg"));
        list.add(buildMovieInfo("Entertainment 5", "Description for Entertainment 5", "Studio Five",
                "https://example.com/entertainment5.mp4", "https://example.com/entertainment5_card.jpg",
                "https://example.com/entertainment5_bg.jpg"));

        // Movies Category
        list.add(buildMovieInfo("Movie 1", "Description for Movie 1", "Studio Six",
                "https://example.com/movie1.mp4", "https://example.com/movie1_card.jpg",
                "https://example.com/movie1_bg.jpg"));
        list.add(buildMovieInfo("Movie 2", "Description for Movie 2", "Studio Seven",
                "https://example.com/movie2.mp4", "https://example.com/movie2_card.jpg",
                "https://example.com/movie2_bg.jpg"));
        list.add(buildMovieInfo("Movie 3", "Description for Movie 3", "Studio Eight",
                "https://example.com/movie3.mp4", "https://example.com/movie3_card.jpg",
                "https://example.com/movie3_bg.jpg"));
        list.add(buildMovieInfo("Movie 4", "Description for Movie 4", "Studio Nine",
                "https://example.com/movie4.mp4", "https://example.com/movie4_card.jpg",
                "https://example.com/movie4_bg.jpg"));
        list.add(buildMovieInfo("Movie 5", "Description for Movie 5", "Studio Ten",
                "https://example.com/movie5.mp4", "https://example.com/movie5_card.jpg",
                "https://example.com/movie5_bg.jpg"));

        // Sports Category
        list.add(buildMovieInfo("Sports 1", "Description for Sports 1", "Studio Eleven",
                "https://example.com/sports1.mp4", "https://example.com/sports1_card.jpg",
                "https://example.com/sports1_bg.jpg"));
        list.add(buildMovieInfo("Sports 2", "Description for Sports 2", "Studio Twelve",
                "https://example.com/sports2.mp4", "https://example.com/sports2_card.jpg",
                "https://example.com/sports2_bg.jpg"));
        list.add(buildMovieInfo("Sports 3", "Description for Sports 3", "Studio Thirteen",
                "https://example.com/sports3.mp4", "https://example.com/sports3_card.jpg",
                "https://example.com/sports3_bg.jpg"));
        list.add(buildMovieInfo("Sports 4", "Description for Sports 4", "Studio Fourteen",
                "https://example.com/sports4.mp4", "https://example.com/sports4_card.jpg",
                "https://example.com/sports4_bg.jpg"));
        list.add(buildMovieInfo("Sports 5", "Description for Sports 5", "Studio Fifteen",
                "https://example.com/sports5.mp4", "https://example.com/sports5_card.jpg",
                "https://example.com/sports5_bg.jpg"));

        // News Category
        list.add(buildMovieInfo("News 1", "Description for News 1", "Studio Sixteen",
                "https://example.com/news1.mp4", "https://example.com/news1_card.jpg",
                "https://example.com/news1_bg.jpg"));
        list.add(buildMovieInfo("News 2", "Description for News 2", "Studio Seventeen",
                "https://example.com/news2.mp4", "https://example.com/news2_card.jpg",
                "https://example.com/news2_bg.jpg"));
        list.add(buildMovieInfo("News 3", "Description for News 3", "Studio Eighteen",
                "https://example.com/news3.mp4", "https://example.com/news3_card.jpg",
                "https://example.com/news3_bg.jpg"));
        list.add(buildMovieInfo("News 4", "Description for News 4", "Studio Nineteen",
                "https://example.com/news4.mp4", "https://example.com/news4_card.jpg",
                "https://example.com/news4_bg.jpg"));
        list.add(buildMovieInfo("News 5", "Description for News 5", "Studio Twenty",
                "https://example.com/news5.mp4", "https://example.com/news5_card.jpg",
                "https://example.com/news5_bg.jpg"));

        // Kids Category
        list.add(buildMovieInfo("Kids 1", "Description for Kids 1", "Studio Twenty-One",
                "https://example.com/kids1.mp4", "https://example.com/kids1_card.jpg",
                "https://example.com/kids1_bg.jpg"));
        list.add(buildMovieInfo("Kids 2", "Description for Kids 2", "Studio Twenty-Two",
                "https://example.com/kids2.mp4", "https://example.com/kids2_card.jpg",
                "https://example.com/kids2_bg.jpg"));
        list.add(buildMovieInfo("Kids 3", "Description for Kids 3", "Studio Twenty-Three",
                "https://example.com/kids3.mp4", "https://example.com/kids3_card.jpg",
                "https://example.com/kids3_bg.jpg"));
        list.add(buildMovieInfo("Kids 4", "Description for Kids 4", "Studio Twenty-Four",
                "https://example.com/kids4.mp4", "https://example.com/kids4_card.jpg",
                "https://example.com/kids4_bg.jpg"));
        list.add(buildMovieInfo("Kids 5", "Description for Kids 5", "Studio Twenty-Five",
                "https://example.com/kids5.mp4", "https://example.com/kids5_card.jpg",
                "https://example.com/kids5_bg.jpg"));

        return list;
    }

    private static Movie buildMovieInfo(
            String title,
            String description,
            String studio,
            String videoUrl,
            String cardImageUrl,
            String backgroundImageUrl) {
        Movie movie = new Movie(); // No-argument constructor
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setStudio(studio);
        movie.setVideoUrl(videoUrl);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(backgroundImageUrl);
        return movie;
    }
}
