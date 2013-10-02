import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: ecil
 * Date: 10/2/13
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class NonPersonalizedRecommender {

    public static void main(String[] args) {
        // Movies array contains the movie IDs of the top 5 movies.
        int movies[] = new int[5];


        // Write the top 5 movies, one per line, to a text file.
        try {
            PrintWriter writer = new PrintWriter("pa1-result.txt", "UTF-8");

            for (int movieId : movies) {
                writer.println(movieId);
            }

            writer.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
