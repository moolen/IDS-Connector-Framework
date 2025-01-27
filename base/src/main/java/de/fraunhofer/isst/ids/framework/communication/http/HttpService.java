package de.fraunhofer.isst.ids.framework.communication.http;

import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Interface for Http Communication
 */
public interface HttpService {

    /**
     * Set the timeouts for the OkHttpClient
     *
     * @param connectTimeout max timeout for connecting to target host (null = default values are used)
     * @param readTimeout max timeout for waiting for the target response (null = default values are used)
     * @param writeTimeout max timeout for sending the response to the target (null = default values are used)
     * @param callTimeout max timeout for the whole http request (null = default values are used)
     */
    void setTimeouts(Duration connectTimeout, Duration readTimeout, Duration writeTimeout, Duration callTimeout);

    /**
     * Reset client timeouts to OkHttp default values
     */
    void removeTimeouts();

    /**
     * Sends plaintext message as http(s) request to the defined target.
     *
     * @param target the target host of the request
     * @param message         IDSMessage to be sent (as JSON)
     * @return true if the message was successfully sent, else false
     * @throws IOException if the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    Response send(String message, URI target) throws IOException;

    /**
     * Sends a given requestBody as http(s) request to the defined in address.
     *
     * @param target the target host of the request
     * @param requestBody {@link RequestBody} object to be sent
     * @return the HttpResponse that comes back for the sent Message
     * @throws IOException if the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    Response send(RequestBody requestBody, URI target) throws IOException;

    /**
     * Sends a given requestBody as http(s) request to the defined in address,
     * extra fields for the header can be provided in headers map.
     *
     * @param requestBody {@link RequestBody} object to be sent
     * @param headers a Map of http headers for the header of the built request
     * @param target the target host of the request
     * @return the HttpResponse that comes back for the sent Message
     * @throws IOException if the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    Response sendWithHeaders(RequestBody requestBody, URI target, Map<String, String> headers) throws IOException;

    /**
     * Sends a http GET request to the target.
     *
     * @param target the target host of the request
     * @return the HttpResponse from the get request
     * @throws IOException if the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    Response get(URI target) throws IOException;

    /**
     * Sends a http GET request to the target,
     * extra fields for the header can be provided in headers map.
     *
     * @param target the target host of the request
     * @param headers a Map of http headers for the header of the built request
     * @return the HttpResponse from the get request
     * @throws IOException if the request could not be executed due to cancellation, a connectivity problem or timeout.
     */
    Response getWithHeaders(URI target, Map<String, String> headers) throws IOException;

}
