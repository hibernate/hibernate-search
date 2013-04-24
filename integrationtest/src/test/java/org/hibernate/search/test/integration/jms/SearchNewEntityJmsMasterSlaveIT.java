/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.integration.jms;

import java.io.File;
import java.util.List;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * In a JMS Master/Slave configuration, every node should be able to find
 * entities created by some other nodes after the synchronization succeed.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class SearchNewEntityJmsMasterSlaveIT {
	/**
	 * Affects how often the Master and Slave directories should start the refresh copy work
	 */
	private static final int REFRESH_PERIOD_IN_SEC = 2;

	/**
	 * Idle loop to wait for results to be transmitted
	 */
	private static final int SLEEP_TIME_FOR_SYNCHRONIZATION = 50;

	/**
	 * Multiplier on top of REFRESH_PERIOD_IN_SEC we can wait before considering the test failed.
	 */
	private static final int MAX_PERIOD_RETRIES = 5;

	private static final int MAX_SEARCH_ATTEMPTS = ( MAX_PERIOD_RETRIES * REFRESH_PERIOD_IN_SEC * 1000 / SLEEP_TIME_FOR_SYNCHRONIZATION );

	private static final File tmpDir = RegistrationConfiguration.createTempDir();

	@Deployment(name = "master", order = 1)
	public static Archive<?> createDeploymentMaster() throws Exception {
		return DeploymentJmsMasterSlave.createMaster( "master", REFRESH_PERIOD_IN_SEC, tmpDir );
	}

	@Deployment(name = "slave-1", order = 2)
	public static Archive<?> createDeploymentSlave1() throws Exception {
		return DeploymentJmsMasterSlave.createSlave( "slave-1", REFRESH_PERIOD_IN_SEC, tmpDir );
	}

	@Deployment(name = "slave-2", order = 3)
	public static Archive<?> createDeploymentSlave2() throws Exception {
		return DeploymentJmsMasterSlave.createSlave( "slave-2", REFRESH_PERIOD_IN_SEC, tmpDir );
	}

	@Inject
	RegistrationController memberRegistration;

	@Test
	@InSequence(0)
	@OperateOnDeployment("master")
	public void deleteExistingMembers() throws Exception {
		int deletedMembers = memberRegistration.deleteAllMembers();
		assertEquals( "At the start of the test there should be no members", 0, deletedMembers );
	}

	@Test
	@InSequence(1)
	@OperateOnDeployment("slave-1")
	public void registerNewMemberOnSlave1() throws Exception {
		RegisteredMember newMember = memberRegistration.getNewMember();
		assertNull( "A non registered member should have null ID", newMember.getId() );

		newMember.setName( "Davide D'Alto" );
		newMember.setEmail( "dd@slave1.fake.email" );
		memberRegistration.register();

		assertNotNull( "A registered member should have an ID", newMember.getId() );
	}

	@Test
	@InSequence(2)
	@OperateOnDeployment("slave-2")
	public void registerNewMemberOnSlave2() throws Exception {
		RegisteredMember newMember = memberRegistration.getNewMember();
		assertNull( "A non registered member should have null ID", newMember.getId() );

		newMember.setName( "Peter O'Tall" );
		newMember.setEmail( "po@slave2.fake.email" );
		memberRegistration.register();

		assertNotNull( "A registered member should have an ID", newMember.getId() );
	}

	@Test
	@InSequence(3)
	@OperateOnDeployment("master")
	public void registerNewMemberOnMaster() throws Exception {
		RegisteredMember newMember = memberRegistration.getNewMember();
		assertNull( "A non registered member should have null ID", newMember.getId() );

		newMember.setName( "Richard Mayhew" );
		newMember.setEmail( "rm@master.fake.email" );
		memberRegistration.register();

		assertNotNull( "A registered member should have an ID", newMember.getId() );
	}

	@Test
	@InSequence(4)
	@OperateOnDeployment("slave-1")
	public void searchNewMembersAfterSynchronizationOnSlave1() throws Exception {
		assertSearchResult( "Davide D'Alto", search( "Davide" ) );
		assertSearchResult( "Peter O'Tall", search( "Peter" ) );
		assertSearchResult( "Richard Mayhew", search( "Richard" ) );
	}

	@Test
	@InSequence(5)
	@OperateOnDeployment("slave-2")
	public void searchNewMembersAfterSynchronizationOnSlave2() throws Exception {
		assertSearchResult( "Davide D'Alto", search( "Davide" ) );
		assertSearchResult( "Peter O'Tall", search( "Peter" ) );
		assertSearchResult( "Richard Mayhew", search( "Richard" ) );
	}

	@Test
	@InSequence(6)
	@OperateOnDeployment("master")
	public void searchNewMembersAfterSynchronizationOnMaster() throws Exception {
		assertSearchResult( "Davide D'Alto", search( "Davide" ) );
		assertSearchResult( "Peter O'Tall", search( "Peter" ) );
		assertSearchResult( "Richard Mayhew", search( "Richard" ) );
	}

	private void assertSearchResult(String expectedResult, List<RegisteredMember> results) {
		assertEquals( "Unexpected number of results from search", 1, results.size() );
		assertEquals( "Unexpected result from search", expectedResult, results.get( 0 ).getName() );
	}

	private void waitForIndexSynchronization() throws InterruptedException {
		Thread.sleep( SLEEP_TIME_FOR_SYNCHRONIZATION );
	}

	private List<RegisteredMember> search(String name) throws InterruptedException {
		List<RegisteredMember> results = memberRegistration.search( name );

		int attempts = 0;
		while ( results.size() == 0 && attempts < MAX_SEARCH_ATTEMPTS ) {
			attempts++;
			waitForIndexSynchronization();
			results = memberRegistration.search( name );
		}
		return results;
	}
}
