/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.integration.jms.transaction;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.search.backend.impl.LocalBackendQueueProcessor;
import org.hibernate.search.backend.jms.impl.JndiJMSBackendQueueProcessor;
import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.testsupport.concurrency.Poller;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * In a JMS Master/Slave configuration, every node should be able to find
 * entities created by some other nodes after the synchronization succeed.
 * <p>
 * Search dependencies are not added to the archives.
 *
 * @author Davide D'Alto
 */
public abstract class TransactionalJmsMasterSlave {

	/**
	 * Affects how often the Master and Slave directories should start the refresh copy work
	 */
	static final int REFRESH_PERIOD_IN_SEC = 1;

	/**
	 * Idle loop to wait for results to be transmitted
	 */
	private static final int SLEEP_TIME_FOR_SYNCHRONIZATION_MS = 30;

	/**
	 * Multiplier on top of REFRESH_PERIOD_IN_SEC we can wait before considering the test failed.
	 */
	private static final int MAX_PERIOD_RETRIES = 5;

	private static final int MAX_SYNCHRONIZATION_TIME_MS = ( MAX_PERIOD_RETRIES * REFRESH_PERIOD_IN_SEC * 1000 ) * 2;

	private static final Poller POLLER = Poller.milliseconds( MAX_SYNCHRONIZATION_TIME_MS, SLEEP_TIME_FOR_SYNCHRONIZATION_MS );

	@Inject
	RegistrationController memberRegistration;

	@Test
	@InSequence(0)
	@OperateOnDeployment("master")
	public void deleteExistingMembers() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 0", "master", LocalBackendQueueProcessor.class.getName() );
		int deletedMembers = memberRegistration.deleteAllMembers();
		assertEquals( "At the start of the test there should be no members", 0, deletedMembers );
	}

	@Test
	@InSequence(1)
	@OperateOnDeployment("slave-1")
	public void registerNewMemberOnSlave1() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 1", "slave-1", JndiJMSBackendQueueProcessor.class.getName() );
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
		memberRegistration.assertConfiguration( "Test Sequence 2", "slave-2", JndiJMSBackendQueueProcessor.class.getName() );
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
		memberRegistration.assertConfiguration( "Test Sequence 3", "master", LocalBackendQueueProcessor.class.getName() );
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
		memberRegistration.assertConfiguration( "Test Sequence 4", "slave-1", JndiJMSBackendQueueProcessor.class.getName() );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Davide D'Alto", "Davide" ) );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Peter O'Tall", "Peter" ) );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Richard Mayhew", "Richard" ) );
	}

	@Test
	@InSequence(5)
	@OperateOnDeployment("slave-2")
	public void searchNewMembersAfterSynchronizationOnSlave2() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 5", "slave-2", JndiJMSBackendQueueProcessor.class.getName() );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Davide D'Alto", "Davide" ) );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Peter O'Tall", "Peter" ) );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Richard Mayhew", "Richard" ) );
	}

	@Test
	@InSequence(6)
	@OperateOnDeployment("master")
	public void searchNewMembersAfterSynchronizationOnMaster() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 6", "master", LocalBackendQueueProcessor.class.getName() );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Davide D'Alto", "Davide" ) );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Peter O'Tall", "Peter" ) );
		POLLER.pollAssertion( () -> assertExactlyOneEntity( "Richard Mayhew", "Richard" ) );
	}

	@Test
	@InSequence(7)
	@OperateOnDeployment("slave-2")
	public void rollbackRegisterNewMemberOnSlave2() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 7", "slave-2", JndiJMSBackendQueueProcessor.class.getName() );
		RegisteredMember newMember = memberRegistration.getNewMember();

		newMember.setName( "Emmanuel Bernard" );
		newMember.setEmail( "e@slave2.fake.email" );
		boolean gotAnException = false;
		try {
			memberRegistration.rollbackedRegister();
		}
		catch (RuntimeException e) {
			assertEquals( RuntimeException.class, e.getCause().getClass() );
			assertEquals( "Shit happens", e.getCause().getMessage() );
			gotAnException = true;
		}
		Assert.assertTrue( gotAnException );
	}

	@Test
	@InSequence(8)
	@OperateOnDeployment("slave-2")
	public void searchRollbackedMemberAfterSynchronizationOnSlave2() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 8", "slave-2", JndiJMSBackendQueueProcessor.class.getName() );
		// we need to explicitly wait because we need to detect the *effective* non operation execution (rollback)
		// so the polling-based method does not work
		// (don't allow it to break out eagerly as in the other tests)
		Thread.sleep( REFRESH_PERIOD_IN_SEC * 2 * 1000 + 200 );//Needs to be twice the refresh period because of sampling, plus some more

		// Test that this was never indexed even after the previous wait time
		assertEquals( 0, memberRegistration.search( "Emmanuel" ).size() );

		// Also test via projection, as a rollback on the RDBMs only would otherwise return an empty list anyway
		assertEquals( 0, memberRegistration.searchName( "Emmanuel" ).size() );
	}

	@Test
	@InSequence(9)
	@OperateOnDeployment("slave-2")
	public void searchNameShouldWorkOnSlave2() throws Exception {
		memberRegistration.assertConfiguration( "Test Sequence 9", "slave-2", JndiJMSBackendQueueProcessor.class.getName() );
		POLLER.pollAssertion( () -> assertAtLeastOneName( "Davide D'Alto", "davide" ) );
		POLLER.pollAssertion( () -> assertAtLeastOneName( "Peter O'Tall", "peter" ) );
		POLLER.pollAssertion( () -> assertAtLeastOneName( "Richard Mayhew", "richard" ) );
	}

	private void assertExactlyOneEntity(String expectedResult, String searchString) {
		List<RegisteredMember> results = memberRegistration.search( searchString );
		assertEquals( "Unexpected number of results from search", 1, results.size() );
		assertEquals( "Unexpected result from search", expectedResult, results.get( 0 ).getName() );
	}

	private void assertAtLeastOneName(String expectedResult, String searchString) {
		List<String> results = memberRegistration.searchName( searchString );
		assertTrue( "Got no result from searchName, expected at least one", !results.isEmpty() );
		assertEquals( "Unexpected result from searchName", expectedResult, results.get( 0 ) );
	}

}
