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
package de.tudarmstadt.ukp.inception.security.client.auth.oauth;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class OAuthSessionImpl
    implements Serializable, OAuthSession
{
    private static final long serialVersionUID = 5378118174761262214L;

    private Date lastUpdate;

    private long accessTokenExpiresIn;

    private long refreshTokenExpiresIn;

    private String accessToken;

    private Date accessTokenValidUntil;

    private String refreshToken;

    private Date refreshTokenValidUntil;

    public OAuthSessionImpl(OAuthAccessTokenResponse aResponse)
    {
        update(aResponse);
    }

    @Override
    public String getAccessToken()
    {
        return accessToken;
    }

    @Override
    public void setAccessToken(String aAccessToken)
    {
        accessToken = aAccessToken;
    }

    @Override
    public Date getAccessTokenValidUntil()
    {
        return accessTokenValidUntil;
    }

    @Override
    public void setAccessTokenValidUntil(Date aAccessTokenValidUntil)
    {
        accessTokenValidUntil = aAccessTokenValidUntil;
    }

    @Override
    public String getRefreshToken()
    {
        return refreshToken;
    }

    @Override
    public void setRefreshToken(String aRefreshToken)
    {
        refreshToken = aRefreshToken;
    }

    @Override
    public Date getRefreshTokenValidUntil()
    {
        return refreshTokenValidUntil;
    }

    @Override
    public void setRefreshTokenValidUntil(Date aRefreshTokenValidUntil)
    {
        refreshTokenValidUntil = aRefreshTokenValidUntil;
    }

    @Override
    public Date getLastUpdate()
    {
        return lastUpdate;
    }

    @Override
    public void setLastUpdate(Date aLastUpdate)
    {
        lastUpdate = aLastUpdate;
    }

    @Override
    public long getAccessTokenExpiresIn()
    {
        return accessTokenExpiresIn;
    }

    @Override
    public void setAccessTokenExpiresIn(long aAccessTokenExpiresIn)
    {
        accessTokenExpiresIn = aAccessTokenExpiresIn;
    }

    @Override
    public long getRefreshTokenExpiresIn()
    {
        return refreshTokenExpiresIn;
    }

    @Override
    public void setRefreshTokenExpiresIn(long aRefreshTokenExpiresIn)
    {
        refreshTokenExpiresIn = aRefreshTokenExpiresIn;
    }
}
