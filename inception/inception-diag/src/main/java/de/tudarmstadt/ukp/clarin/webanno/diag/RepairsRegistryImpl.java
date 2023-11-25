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
package de.tudarmstadt.ukp.clarin.webanno.diag;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CasDoctorAutoConfiguration#repairsRegistry}.
 * </p>
 */
public class RepairsRegistryImpl
    extends ExtensionPoint_ImplBase<Void, Repair>
    implements RepairsRegistry
{
    public RepairsRegistryImpl(@Lazy @Autowired(required = false) List<Repair> aExtensions)
    {
        super(aExtensions);
    }
}
