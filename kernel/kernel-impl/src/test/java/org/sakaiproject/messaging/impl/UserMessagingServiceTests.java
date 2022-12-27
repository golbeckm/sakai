/*
 * Copyright (c) 2003-2021 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.messaging.impl;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.messaging.api.UserMessagingService;
import org.sakaiproject.messaging.api.UserNotification;
import org.sakaiproject.messaging.api.repository.UserNotificationRepository;
import org.sakaiproject.time.api.UserTimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UserMessagingServiceTestConfiguration.class})
public class UserMessagingServiceTests extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired private ServerConfigurationService serverConfigurationService;
    @Autowired private SessionManager sessionManager;
    @Autowired private UserDirectoryService userDirectoryService;
    @Autowired private UserMessagingService userMessagingService;
    @Autowired private UserNotificationRepository userNotificationRepository;
    @Autowired private UserTimeService userTimeService;

    String student = "student";
    User studentUser = null;
    String instructor = "instructor";
    User instructorUser = null;
    UserNotification userNotification1;
    UserNotification userNotification2;

    @Before
    public void setup() {

        studentUser = mock(User.class);
        when(studentUser.getDisplayName()).thenReturn("Student User");
        try {
            when(userDirectoryService.getUser(student)).thenReturn(studentUser);
        } catch (UserNotDefinedException unde) {}

        instructorUser = mock(User.class);
        when(instructorUser.getDisplayName()).thenReturn("Instructor User");
        try {
            when(userDirectoryService.getUser(instructor)).thenReturn(instructorUser);
        } catch (UserNotDefinedException unde) {}

        when(serverConfigurationService.getBoolean("portal.bullhorns.enabled", true)).thenReturn(true);
        when(userTimeService.dateTimeFormat(any(), any(), any())).thenReturn("07 Feb 1971");;

        userNotification1 = new UserNotification();
        userNotification1.setToUser(student);
        userNotification1.setFromUser(instructor);
        userNotification1.setTitle("Notification 1");
        userNotification1.setEvent("notification1.event");
        userNotification1.setEventDate(Instant.now());
        userNotification1.setRef("/notification/one");
        userNotification1.setUrl("/portal/site/bogus/tool/xyz");

        userNotification2 = new UserNotification();
        userNotification2.setToUser(student);
        userNotification2.setFromUser(instructor);
        userNotification2.setTitle("Notification 2");
        userNotification2.setEvent("notification2.event");
        userNotification2.setEventDate(Instant.now());
        userNotification2.setRef("/notification/two");
        userNotification2.setUrl("/portal/site/bogus/tool/xyz");
    }

    @Test
    public void createAndClearNotifications() {

        assertTrue("The alerts list should be empty", userMessagingService.getNotifications().isEmpty());

        switchToStudent();

        assertTrue("The alerts list should be empty", userMessagingService.getNotifications().isEmpty());

        UserNotification updatedUserNotification1 = userNotificationRepository.save(userNotification1);
        assertEquals("There should be 1 alert", 1, userMessagingService.getNotifications().size());

        UserNotification updatedUserNotification2 = userNotificationRepository.save(userNotification2);
        assertEquals("There should be 2 alerts", 2, userMessagingService.getNotifications().size());

        userMessagingService.clearNotification(updatedUserNotification1.getId());
        assertEquals("There should be 1 alert", 1, userMessagingService.getNotifications().size());

        switchToInstructor();
        assertFalse("One user should not be able to delete another's notifications", userMessagingService.clearNotification(updatedUserNotification2.getId()));

        switchToStudent();
        assertEquals("There should be 1 alert", 1, userMessagingService.getNotifications().size());
    }

    @Test
    public void clearAllNotifications() {

        switchToStudent();

        assertTrue("The alerts list should be empty", userMessagingService.getNotifications().isEmpty());

        UserNotification updatedUserNotification1 = userNotificationRepository.save(userNotification1);
        UserNotification updatedUserNotification2 = userNotificationRepository.save(userNotification2);

        assertEquals("There should be 2 alerts", 2, userMessagingService.getNotifications().size());

        userMessagingService.clearAllNotifications();

        assertEquals("There should be 0 alerts", 0, userMessagingService.getNotifications().size());
    }

    private void switchToStudent() {
        when(sessionManager.getCurrentSessionUserId()).thenReturn(student);
    }

    private void switchToInstructor() {
        when(sessionManager.getCurrentSessionUserId()).thenReturn(instructor);
    }
}
