import au.com.bytecode.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import static java.util.Collections.sort;

/**
 * Created with IntelliJ IDEA.
 * User: ecil
 * Date: 10/2/13
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class NonPersonalizedRecommender {

    private static final String MOVIE_TITLES_FILE = "data/recsys_data_movie-titles.csv";
    private static final String USERS_FILE = "data/recsys_data_users.csv";
    private static final String RATINGS_FILE = "data/recsys_data_ratings.csv";

    private class MovieScore implements Comparable<MovieScore> {

        public int movieId;
        public float score;

        public MovieScore(int movie, float score) {
            this.movieId = movie;
            this.score = score;
        }

        public int compareTo(MovieScore o) {
            // returned values are inverse to sort in reverse order
            if (score > o.score)
                return -1;
            else if (score < o.score)
                return 1;
            else
                return 0;
        }
    }

    /*
    Returns a HashMap in this format:
    {title_code: title_name,
     title_code: title_name,
     ...}
     */
    private Map<String, String> loadMovies() {
        Map<String, String> movies = new HashMap<String, String>();
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(MOVIE_TITLES_FILE));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                movies.put(nextLine[0], nextLine[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
        return movies;
    }

    /*
    Returns a HashMap in this format:
    {user_code: user_name,
     user_code: user_name,
     ...}
     */
    private Map<String, String> loadUsers() {
        Map<String, String> users = new HashMap<String, String>();
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(USERS_FILE));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                users.put(nextLine[0], nextLine[1]);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        return users;
    }


    /*
    Returns a HashMap in this format:
    {title_code: {user_code: rating,
                  user_code: rating,
                  user_code: rating,
                  ...},
     title_code: {user_code: rating,
                  user_code: rating,
                  user_code: rating,
                  ...},
     ...}
     */
    private Map<String, Map<String, Float>> loadRatings() {
        Map<String, Map<String, Float>> ratings = new HashMap<String, Map<String, Float>>();
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(RATINGS_FILE));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                Map<String, Float> ratingsForAnItem;
                String movie = nextLine[1];
                String user = nextLine[0];
                Float rating = Float.valueOf(nextLine[2]);
                if (ratings.get(movie) == null) { //movie is not on the list yet
                    ratingsForAnItem = new HashMap<String, Float>();
                    ratingsForAnItem.put(user, rating);
                    ratings.put(movie, ratingsForAnItem);
                } else {
                    ratingsForAnItem = ratings.get(movie);
                    ratingsForAnItem.put(user, rating);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        return ratings;
    }

    private int commonElementCount(Set<String> set1, Set<String> set2) {
        int count = 0;
        for (String element : set1) {
            if (set2.contains(element))
                count++;
        }
        return count;
    }

    private MovieScore[] topMoviesSimpleAssociation(int movie, Map<String, Map<String, Float>> ratings, int count) {
        MovieScore[] topMovies = new MovieScore[count];
        int referenceMovieCount;
        referenceMovieCount = ratings.get(Integer.toString(movie)).keySet().size();

        Set<String> referenceUserSet = ratings.get(String.valueOf(movie)).keySet();

        List<MovieScore> movieScoreList = new ArrayList<MovieScore>();
        for (String associatedMovie : ratings.keySet()) {
            int associatedMovieCode = Integer.parseInt(associatedMovie);
            if (associatedMovieCode == movie)
                continue;
            Set<String> associatedUserSet = ratings.get(associatedMovie).keySet();

            float simpleScore = commonElementCount(referenceUserSet, associatedUserSet) / (float) referenceMovieCount;
            MovieScore ms = new MovieScore(associatedMovieCode, simpleScore);
            movieScoreList.add(ms);

        }
        sort(movieScoreList);
        for (int i = 0; i < count; i++) {
            topMovies[i] = movieScoreList.get(i);//movieScoreList.get(movieScoreList.size() - i - 1);
        }
        return topMovies;
    }

    private Set<String> differenceSet(Set<String> set1, Set<String> set2) {
        Set<String> differenceSet = new HashSet<String>();
        for (String key : set1) {
            if (!set2.contains(key)) {
                differenceSet.add(key);
            }
        }
        return differenceSet;
    }

    private MovieScore[] topMoviesAdvancedAssociation(int movie, Map<String, Map<String, Float>> ratings, int count, Map<String, String> users) {
        MovieScore[] topMovies = new MovieScore[count];
        int referenceMovieCount;
        referenceMovieCount = ratings.get(Integer.toString(movie)).keySet().size();

        Set<String> referenceUserSet = ratings.get(String.valueOf(movie)).keySet();

        System.out.println("Ref: " + referenceUserSet.size());

        Set<String> usersThatDidNotRateTheMovieSet = differenceSet(users.keySet(), referenceUserSet);
        System.out.println("Diff: " + usersThatDidNotRateTheMovieSet.size());

        System.out.println("Total: " + (referenceUserSet.size() + usersThatDidNotRateTheMovieSet.size()) );
        List<MovieScore> movieScoreList = new ArrayList<MovieScore>();
        for (String associatedMovie : ratings.keySet()) {
            int associatedMovieCode = Integer.parseInt(associatedMovie);
            if (associatedMovieCode == movie)
                continue;
            Set<String> associatedUserSet = ratings.get(associatedMovie).keySet();

            float usersThatDidNotRateTheMovieCount = (float)usersThatDidNotRateTheMovieSet.size();
            float simpleScore = commonElementCount(referenceUserSet, associatedUserSet) / (float) referenceMovieCount;

            float advancedScore = simpleScore / (commonElementCount(usersThatDidNotRateTheMovieSet, associatedUserSet) / usersThatDidNotRateTheMovieCount);

            MovieScore ms = new MovieScore(associatedMovieCode, advancedScore);
            movieScoreList.add(ms);

        }
        sort(movieScoreList);
        for (int i = 0; i < count; i++) {
            topMovies[i] = movieScoreList.get(i);//movieScoreList.get(movieScoreList.size() - i - 1);
        }
        return topMovies;
    }

    private String formatCSV(int movieId, MovieScore[] topMovies) {
        DecimalFormat df = new DecimalFormat("0.00");
        String csv = "";
        csv += movieId;
        for (MovieScore ms : topMovies) {
            csv += ",";
            csv += ms.movieId;
            csv += ",";
            csv += df.format(ms.score);
        }
        csv += "\n";
        return csv;
    }

    public static void main(String[] args) {
        // Movies array contains the movie IDs of the top 5 movies.
        MovieScore[] topMovies;

        NonPersonalizedRecommender npr = new NonPersonalizedRecommender();
        Map<String, String> movies = npr.loadMovies();
        /*
        for (String key : movies.keySet()) {
            System.out.println(key + " - " + movies.get(key));
        }
        */

        Map<String, String> users = npr.loadUsers();
        /*
        for (String key : users.keySet()) {
            System.out.println(key + " - " + users.get(key));
        }
        */

        Map<String, Map<String, Float>> ratings = npr.loadRatings();
        /*
        for (String movie : ratings.keySet()) {
            System.out.println(movie);
            Map<String, Float> ratingsForAMovie = ratings.get(movie);
            for (String user : ratingsForAMovie.keySet()) {
                Float rating = ratingsForAMovie.get(user);
                System.out.println("=> " + user + '\t' + rating);
            }
        }
        */

        int[] assignedMovieId = {11, 121, 8587};

        // Simple formula
        String csv = "";
        for (int movieId : assignedMovieId) {
            topMovies = npr.topMoviesSimpleAssociation(movieId, ratings, 5);
            csv += npr.formatCSV(movieId, topMovies);
        }
        System.out.println(csv);

        try {
            PrintWriter writer = new PrintWriter("pa1-result-simple.txt", "UTF-8");
            writer.print(csv);
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Advanced formula
        csv = "";
        for (int movieId : assignedMovieId) {
            topMovies = npr.topMoviesAdvancedAssociation(movieId, ratings, 5, users);
            csv += npr.formatCSV(movieId, topMovies);
        }
        System.out.println(csv);

        try {
            PrintWriter writer = new PrintWriter("pa1-result-advanced.txt", "UTF-8");
            writer.print(csv);
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}
