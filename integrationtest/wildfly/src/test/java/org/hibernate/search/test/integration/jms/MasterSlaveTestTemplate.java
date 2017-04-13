/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;

/**
 * In a JMS or JGroups Master/Slave configuration, every node should be able to find
 * entities created by some other nodes after the synchronization succeed.
 *
 * @author Davide D'Alto
 */
public abstract class MasterSlaveTestTemplate {

	/**
	 * Affects how often the Master and Slave directories should start the refresh copy work
	 */
	public static final int REFRESH_PERIOD_IN_SEC = 2;

	/**
	 * Idle loop to wait for results to be transmitted
	 */
	private static final int SLEEP_TIME_FOR_SYNCHRONIZATION_MS = 50;

	/**
	 * Multiplier on top of REFRESH_PERIOD_IN_SEC we can wait before considering the test failed.
	 */
	private static final int MAX_PERIOD_RETRIES = 5;

	private static final int MAX_SYNCHRONIZATION_TIME_MS = MAX_PERIOD_RETRIES * REFRESH_PERIOD_IN_SEC * 1000;

	private static final Poller POLLER = Poller.milliseconds( MAX_SYNCHRONIZATION_TIME_MS, SLEEP_TIME_FOR_SYNCHRONIZATION_MS );

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
		POLLER.pollAssertion( () -> assertSearchResult( "Davide D'Alto", "Davide" ) );
		POLLER.pollAssertion( () -> assertSearchResult( "Peter O'Tall", "Peter" ) );
		POLLER.pollAssertion( () -> assertSearchResult( "Richard Mayhew", "Richard" ) );
	}

	@Test
	@InSequence(5)
	@OperateOnDeployment("slave-2")
	public void searchNewMembersAfterSynchronizationOnSlave2() throws Exception {
		POLLER.pollAssertion( () -> assertSearchResult( "Davide D'Alto", "Davide" ) );
		POLLER.pollAssertion( () -> assertSearchResult( "Peter O'Tall", "Peter" ) );
		POLLER.pollAssertion( () -> assertSearchResult( "Richard Mayhew", "Richard" ) );
	}

	@Test
	@InSequence(6)
	@OperateOnDeployment("master")
	public void searchNewMembersAfterSynchronizationOnMaster() throws Exception {
		POLLER.pollAssertion( () -> assertSearchResult( "Davide D'Alto", "Davide" ) );
		POLLER.pollAssertion( () -> assertSearchResult( "Peter O'Tall", "Peter" ) );
		POLLER.pollAssertion( () -> assertSearchResult( "Richard Mayhew", "Richard" ) );
	}

	private void assertSearchResult(String expectedResult, String searchString) {
		List<RegisteredMember> results = memberRegistration.search( searchString );
		assertEquals( "Unexpected number of results from search", 1, results.size() );
		assertEquals( "Unexpected result from search", expectedResult, results.get( 0 ).getName() );
	}

}
