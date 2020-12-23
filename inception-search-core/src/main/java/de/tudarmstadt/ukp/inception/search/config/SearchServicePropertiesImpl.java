/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("search")
public class SearchServicePropertiesImpl
    implements SearchServiceProperties
{
    private boolean enabled = false;

    private Duration indexKeepOpenTime = Duration.ofMinutes(10);

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    @Override
    public Duration getIndexKeepOpenTime()
    {
        return indexKeepOpenTime;
    }

    public void setIndexKeepOpenTime(Duration aIndexKeepOpenTime)
    {
        indexKeepOpenTime = aIndexKeepOpenTime;
    }
}
