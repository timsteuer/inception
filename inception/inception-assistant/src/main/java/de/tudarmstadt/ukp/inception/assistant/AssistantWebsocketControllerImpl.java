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
package de.tudarmstadt.ukp.inception.assistant;

import static de.tudarmstadt.ukp.inception.assistant.model.MAssistantChatRoles.USER;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.inception.assistant.model.MAssistantTextMessage;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;

@Controller
@RequestMapping(AssistantWebsocketController.BASE_URL)
@ConditionalOnWebApplication
@ConditionalOnExpression("${websocket.enabled:true} and ${assistant.enabled:false}")
public class AssistantWebsocketControllerImpl
    implements AssistantWebsocketController
{
    private final AssistantService assistantService;
    private final ProjectService projectService;

    @Autowired
    public AssistantWebsocketControllerImpl(ServletContext aServletContext,
            SimpMessagingTemplate aMsgTemplate, AssistantService aAssistantService, ProjectService aProjectService)
    {
        assistantService = aAssistantService;
        projectService = aProjectService;
    }

    @SubscribeMapping(PROJECT_ASSISTANT_TOPIC_TEMPLATE)
    public List<MAssistantTextMessage> onSubscribeToAssistantMessages(SimpMessageHeaderAccessor aHeaderAccessor,
            Principal aPrincipal, //
            @DestinationVariable(PARAM_PROJECT) long aProjectId)
        throws IOException
    {
        var project = projectService.getProject(aProjectId);
        return assistantService.getConversationMessages(aPrincipal.getName(), project);
    }
    
    @MessageMapping(PROJECT_ASSISTANT_TOPIC_TEMPLATE)
    public void onUserMessage(SimpMessageHeaderAccessor aHeaderAccessor,
            Principal aPrincipal, //
            @DestinationVariable(PARAM_PROJECT) long aProjectId,
            @Payload String aMessage)
        throws IOException
    {
        var project = projectService.getProject(aProjectId);
        var message = MAssistantTextMessage.builder().withRole(USER).withMessage(aMessage).build();
        assistantService.processUserMessage(aPrincipal.getName(), project, message);
    }

    @SendTo(PROJECT_ASSISTANT_TOPIC_TEMPLATE)
    public MAssistantTextMessage send(MAssistantTextMessage aUpdate)
    {
        return aUpdate;
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}
