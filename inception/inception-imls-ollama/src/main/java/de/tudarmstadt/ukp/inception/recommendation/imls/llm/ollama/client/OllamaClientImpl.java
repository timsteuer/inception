/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class OllamaClientImpl
    implements OllamaClient
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public static final int HTTP_BAD_REQUEST = 400;

    private final OllamaMetrics metrics;

    protected final HttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaClientImpl()
    {
        client = HttpClient.newBuilder().build();
        metrics = new OllamaMetricsImpl();
    }

    public OllamaClientImpl(HttpClient aClient, OllamaMetrics aMetrics)
    {
        client = aClient;
        metrics = aMetrics;
    }

    protected HttpResponse<InputStream> sendRequest(HttpRequest aRequest) throws IOException
    {
        try {
            var response = client.send(aRequest, HttpResponse.BodyHandlers.ofInputStream());

            handleError(response);

            return response;
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Error while sending request: " + e.getMessage(), e);
        }
    }

    protected String getResponseBody(HttpResponse<InputStream> response) throws IOException
    {
        if (response.body() != null) {
            return IOUtils.toString(response.body(), UTF_8);
        }

        return "";
    }

    @Override
    public String generate(String aUrl, OllamaGenerateRequest aRequest) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "api/generate")) //
                .header(CONTENT_TYPE, "application/json")
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var rawResponse = sendRequest(request);

        handleError(rawResponse);

        var result = new StringBuilder();
        try (var is = rawResponse.body()) {
            var iter = objectMapper.readerFor(OllamaGenerateResponse.class).readValues(is);
            while (iter.hasNext()) {
                var response = (OllamaGenerateResponse) iter.nextValue();

                if (LOG.isDebugEnabled()) {
                    var loadDuration = response.getLoadDuration() / 1_000_000_000;
                    var promptEvalDuration = response.getPromptEvalDuration() / 1_000_000_000d;
                    var promptEvalTokenPerSecond = response.getPromptEvalCount()
                            / promptEvalDuration;
                    var evalDuration = response.getEvalDuration() / 1_000_000_000d;
                    var evalTokenPerSecond = response.getEvalCount() / evalDuration;
                    var totalDuration = response.getTotalDuration() / 1_000_000_000;
                    LOG.debug("Tokens  - prompt: {} ({} per sec) response: {} ({} per sec)", //
                            response.getPromptEvalCount(), //
                            promptEvalTokenPerSecond, //
                            response.getEvalCount(), //
                            evalTokenPerSecond);
                    LOG.debug("Timings - load: {}sec  prompt: {}sec  response: {}s  total: {}sec", //
                            loadDuration, promptEvalDuration, evalDuration, totalDuration);
                }

                if (metrics != null) {
                    metrics.handleResponse(response);
                }

                result.append(response.getResponse());
            }
        }

        return result.toString().trim();
    }

    @Override
    public List<OllamaModel> listModels(String aUrl) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "api/tags")) //
                .header(CONTENT_TYPE, "application/json").GET() //
                .timeout(TIMEOUT) //
                .build();

        var response = sendRequest(request);

        handleError(response);

        try (var is = response.body()) {
            return objectMapper.readValue(is, OllamaTagsResponse.class).getModels();
        }
    }

    private void handleError(HttpResponse<InputStream> response) throws IOException
    {
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            var responseBody = getResponseBody(response);
            var msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new IOException(msg);
        }
    }
}