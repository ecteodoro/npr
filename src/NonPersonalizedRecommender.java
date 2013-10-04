import au.com.bytecode.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import static java.util.Collections.sort;

/**
 * A Simple Non Personalized Recommender
 *
 * User: ecil
 * Date: 10/2/13
 * Time: 9:49 AM
 *
 */
public class NonPersonalizedRecommender {

    private static final String MOVIE_TITLES_FILE = "data/recsys_data_movie-titles.csv";
    private static final String USERS_FILE = "data/recsys_data_users.csv";
    private static final String RATINGS_FILE = "data/recsys_data_ratings.csv";

    /**
     * This inner class is a simple data structure to store
     * a movieId and its associated score.
     * Most often used as a collection of these objects.
     * Its Comparable feature allows a collection of these objects
     * to be sorted (in descending order, to get the top scored movies).
     */
    private class MovieScore implements Comparable<MovieScore> {

        public int movieId;
        public float score;

        public MovieScore(int movie, float score) {
            this.movieId = movie;
            this.score = score;
        }

        public int compareTo(MovieScore o) {
            // returned values are inverse to sort in descending order
            if (score > o.score)
                return -1;
            else if (score < o.score)
                return 1;
            else
                return 0;
        }
    }

    /**
     * Loads movies from a CSV data file.
     *
     * @return a HashMap in this format: {title_code: title_name, title_code: title_name, ...}
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

    /**
     * Loads users from a CSV data file.
     *
     * @return a HashMap in this format: {user_code: user_name, user_code: user_name, ...}
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


    /**
     * Loads ratings from a CSV data file and returns a HashMap in this format:
     * {movieID: {userID: rating,
     *            userID: rating,
     *            userID: rating,
     *            ...},
     *  movieID: {userID: rating,
     *            userID: rating,
     *            userID: rating,
     *            ...},
     *  ...}
     *
     *  @return a HashMap containing ratings
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

    /**
     * Returns the number of elements that exist in both sets (intersection).
     * Given set A and set B, returns the cardinality of (A & B).
     *
     * @param set1 the first set
     * @param set2 the second set
     * @return the number of common elements
     */
    private int commonElementCount(Set<String> set1, Set<String> set2) {
        int count = 0;
        for (String element : set1) {
            if (set2.contains(element))
                count++;
        }
        return count;
    }

    /**
     * Given two sets, returns a third set that contains elements
     * that are members of the first set and are not members of the second set.
     * Given sets A and B, return C = (A \ B).
     *
     * @param set1 first set
     * @param set2 second set
     * @return the relative complement of the second set in the first set
     */
    private Set<String> differenceSet(Set<String> set1, Set<String> set2) {
        Set<String> differenceSet = new HashSet<String>();
        for (String key : set1) {
            if (!set2.contains(key)) {
                differenceSet.add(key);
            }
        }
        return differenceSet;
    }

    /**
     * Returns the top N movies associated with another movie, using a simple formula.
     *
     * @param movie the reference movie
     * @param ratings the ratings matrix
     * @param count how many top N movies must be associated and returned
     * @return the top N movies
     */
    private MovieScore[] topMoviesSimpleAssociation(int movie, Map<String, Map<String, Float>> ratings, int count) {
        return calculateTopMovies(movie, ratings, count, null, false);
    }

    /**
     * Returns the top N movies associated with another movie, using an advanced formula.
     *
     * @param movie the reference movie
     * @param ratings the ratings matrix
     * @param count how many top N movies must be associated and returned
     * @param users the list of all users who rated movies
     * @return
     */
    private MovieScore[] topMoviesAdvancedAssociation(int movie, Map<String, Map<String, Float>> ratings, int count, Map<String, String> users) {
        return calculateTopMovies(movie, ratings, count, users, true);
    }

    /**
     * Return a list of the top 5 movies that occur with a certain Movie A (reference movie).
     *
     * @param movie the reference movie
     * @param ratings the ratings matrix
     * @param count how many top movies will be recommended (in this case it is 5)
     * @param users the list of users who rated movies
     * @param isAdvancedMode calculate using advanced formula (true) or using simple formula (false)
     * @return an array of {@link MovieScore}
     */
    private MovieScore[] calculateTopMovies(int movie, Map<String, Map<String, Float>> ratings, int count, Map<String, String> users, boolean isAdvancedMode) {
        MovieScore[] topMovies = new MovieScore[count];
        // how many users rated the reference movie
        int referenceMovieCount = ratings.get(Integer.toString(movie)).keySet().size();
        // the list of users/rating that rated the reference movie
        Set<String> referenceUserSet = ratings.get(String.valueOf(movie)).keySet();
        Set<String> usersThatDidNotRateTheMovieSet = null;
        if (isAdvancedMode)
            usersThatDidNotRateTheMovieSet = differenceSet(users.keySet(), referenceUserSet);
        List<MovieScore> movieScoreList = new ArrayList<MovieScore>();
        // reads the users ratings associated with other movies and calculate an association score
        for (String associatedMovie : ratings.keySet()) {
            int associatedMovieCode = Integer.parseInt(associatedMovie);
            if (associatedMovieCode == movie)
                continue; // skip when movie is the reference movie
            Set<String> associatedUserSet = ratings.get(associatedMovie).keySet();
            float score = commonElementCount(referenceUserSet, associatedUserSet) / (float) referenceMovieCount;
            if (isAdvancedMode) {
                float usersThatDidNotRateTheMovieCount = (float)usersThatDidNotRateTheMovieSet.size();
                score = score / (commonElementCount(usersThatDidNotRateTheMovieSet, associatedUserSet) / usersThatDidNotRateTheMovieCount);
            }
            MovieScore ms = new MovieScore(associatedMovieCode, score);
            movieScoreList.add(ms);
        }
        // sort the score list in descending order and pick the top N movies
        sort(movieScoreList);
        for (int i = 0; i < count; i++) {
            topMovies[i] = movieScoreList.get(i);
        }
        return topMovies;
    }

    /**
     * Format a row for CSV file with recommendations.
     * Each row will have the movie ID of the movie assigned to you,
     * followed by five pairs of "movie ID,predicted-score", from first to last,
     * showing the top-five associated movies using that formula.
     * Scores must have at least 2 decimal places.
     *
     * @param movieId assigned movie
     * @param topMovies the top movies and associated scores
     */
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
        Map<String, String> users = npr.loadUsers();
        Map<String, Map<String, Float>> ratings = npr.loadRatings();

        // movied IDs used as reference for recommendations
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
