// Prints the folder name the Gradle wrapper uses for a distribution URL
// (~/.gradle/wrapper/dists/<name>/<this hash>/), so the build script can place a Gradle zip it
// downloaded itself where the wrapper looks first. Same algorithm as Gradle's PathAssembler.
// Run with: java tools/WrapperHash.java <distributionUrl>
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class WrapperHash {
    public static void main(String[] args) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(args[0].getBytes(StandardCharsets.UTF_8));
        System.out.println(new BigInteger(1, md5.digest()).toString(36));
    }
}
