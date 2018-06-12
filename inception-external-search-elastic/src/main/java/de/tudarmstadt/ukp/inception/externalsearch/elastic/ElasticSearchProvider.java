/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.elastic;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;

public class ElasticSearchProvider
    implements ExternalSearchProvider
{

    private String connectionString = "";

    @Override
    public boolean connect(String aUrl, String aUser, String aPassword)
    {
        // Always return true, no connection needed
        return true;
    }

    @Override
    public void disconnect()
    {
        // Nothing to do, no connection needed in this provider
    }

    @Override
    public boolean isConnected()
    {
        // Always return true, no connection needed
        return true;
    }

    @Override
    public List<ExternalSearchResult> executeQuery(User aUser, String aQuery, String aSortOrder,
            String... sResultField)
    {
        List<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

        // URLCodec codec = new org.apache.commons.codec.net.URLCodec();
        // String query = codec.encode(String.format(parameterString, aQuery));

        RestTemplate restTemplate = new RestTemplate();

        ElasticSearchResult queryResult;

        queryResult = restTemplate.getForObject(connectionString + aQuery,
                ElasticSearchResult.class);

        for (ElasticSearchHit hit : queryResult.getHits().getHits()) {
            ExternalSearchResult result = new ExternalSearchResult();

            result.setDocumentId(hit.get_id());
            result.setText(hit.get_source().getDoc().getText());
            results.add(result);
        }

        return results;
    }

}
