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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

public class LinkFeatureDecl
{
    private final String name;
    private final String roleFeature;
    private final String targetFeature;
    private final LinkFeatureMultiplicityMode compareBehavior;

    public LinkFeatureDecl(String aName, String aRoleFeature, String aTargetFeature,
            LinkFeatureMultiplicityMode aCompareBehavior)
    {
        name = aName;
        roleFeature = aRoleFeature;
        targetFeature = aTargetFeature;
        compareBehavior = aCompareBehavior;
    }

    public String getName()
    {
        return name;
    }

    public String getRoleFeature()
    {
        return roleFeature;
    }

    public String getTargetFeature()
    {
        return targetFeature;
    }

    public LinkFeatureMultiplicityMode getCompareBehavior()
    {
        return compareBehavior;
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        builder.append("LinkFeatureDecl [name=");
        builder.append(getName());
        if (getRoleFeature() != null) {
            builder.append(", roleFeature=");
            builder.append(getRoleFeature());
        }
        if (getTargetFeature() != null) {
            builder.append(", targetFeature=");
            builder.append(getTargetFeature());
        }
        builder.append("]");
        return builder.toString();
    }
}
