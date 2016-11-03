/*
 * LICENCE : CloudUnit is available under the Affero Gnu Public License GPL V3 : https://www.gnu.org/licenses/agpl-3.0.html
 * but CloudUnit is licensed too under a standard commercial license.
 * Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 * If you are not sure whether the GPL is right for you,
 * you can always test our software under the GPL and inspect the source code before you contact us
 * about purchasing a commercial license.
 *
 * LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 * or promote products derived from this project without prior written permission from Treeptik.
 * Products or services derived from this software may not be called "CloudUnit"
 * nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 * For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Class with utilities method for testing url and application deployment
 */
public class TestUtils {
    /**
     * Maximum number of iterations when pooling for content.
     * Raise the value if some tests fail.
     * 
     * @see #waitForContent(String)
     */
    public static final Integer NB_ITERATION_MAX = 30;

    /**
     * Return the content of an URL.
     *
     * @param url
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public static String getUrlContentPage(String url)
        throws ParseException, IOException {
        HttpGet request = new HttpGet(url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity);
    }
    
    /**
     * Pool URL until non-error content is given.
     * 
     * Tries at most {@link #NB_ITERATION_MAX} times.
     * 
     * @param url  a string containing a valid URL
     * @return the content received, or {@link Optional#empty()} if the URL has been pooled the maximum amount of times
     * before any content was returned.  
     */
    public static Optional<String> waitForContent(String url) {
        return Stream.generate(() -> {
                    try {
                        Thread.sleep(1000);
                        return getUrlContentPage(url);
                    } catch (ParseException | IOException | InterruptedException e) {
                        return null;
                    }
                })
            .limit(NB_ITERATION_MAX)
            .filter(content -> content != null && !content.contains("404") && !content.contains("502"))
            .findFirst();
    }

    /**
     * Download from github binaries and deploy file.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static MockMultipartFile downloadAndPrepareFileToDeploy(String filename, String path)
            throws IOException {
        return new MockMultipartFile("file", filename, "multipart/form-data", new URL(path).openStream());
    }
}
