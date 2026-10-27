import java.io.IOException; //in case the file can't be written
import java.nio.file.Files;
import java.nio.file.Path;

//builds the web page and saves it as report.html, which you can open in a browser.
//Build:  javac -d out src/*.java
//Run:    java -cp out htmlReport     (run from the hw-starter folder)
public class htmlReport {

    public static void main(String[] args) {

        String page = htmlPage.buildPage(); //get the whole page as one String

        Path file = Path.of("report.html"); //where to save it. this is the folder you run java from.

        try {
            Files.writeString(file, page); //write the String into the file (replaces it if it already exists)
            System.out.println("Saved page to " + file.toAbsolutePath());
            System.out.println("Open that file in your browser to see it.");
        }
        catch (IOException e) { //e.g. no permission to write in this folder
            System.out.println("Could not save report.html: " + e.getMessage());
        }
    }
}
