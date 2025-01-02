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

public interface AssistantEmbeddingProperties
{
    public static final int AUTO_DETECT_DIMENSION = -1;

    String getModel();

    double getTopP();

    int getTopK();

    double getRepeatPenalty();

    double getTemperature();

    int getSeed();

    /**
     * @return maximum context length of the embedding model
     */
    int getContextLength();

    int getBatchSize();

    String getEncoding();

    /**
     * @return embedding dimension of the embedding model
     */
    int getDimension();

    void setDimension(int aI);

    /**
     * @return maximum size in LLM tokens that a RAG chunk should have
     */
    int getChunkSize();

    /**
     * @return the minimum score a chunk must have with respect to the user query to be used by the
     *         RAG
     */
    double getChunkScoreThreshold();
}
