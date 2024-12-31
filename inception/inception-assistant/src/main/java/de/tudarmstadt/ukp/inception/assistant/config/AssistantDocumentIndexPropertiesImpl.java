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
package de.tudarmstadt.ukp.inception.assistant.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedResource;

@ConfigurationProperties("assistant.document-index")
@ManagedResource
public class AssistantDocumentIndexPropertiesImpl
    implements AssistantDocumentIndexProperties
{
    private Duration idleEvictionDelay = Duration.ofMinutes(5);
    private Duration minIdleTime = Duration.ofMinutes(5);
    private Duration borrowWaitTimeout = Duration.ofMinutes(3);

    @Override
    public Duration getIdleEvictionDelay()
    {
        return idleEvictionDelay;
    }

    public void setIdleEvictionDelay(Duration aIdleEvictionDelay)
    {
        idleEvictionDelay = aIdleEvictionDelay;
    }

    @Override
    public Duration getMinIdleTime()
    {
        return minIdleTime;
    }

    public void setMinIdleTime(Duration aMinIdleTime)
    {
        minIdleTime = aMinIdleTime;
    }

    @Override
    public Duration getBorrowWaitTimeout()
    {
        return borrowWaitTimeout;
    }

    public void setBorrowWaitTimeout(Duration aBorrowWaitTimeout)
    {
        borrowWaitTimeout = aBorrowWaitTimeout;
    }
}
