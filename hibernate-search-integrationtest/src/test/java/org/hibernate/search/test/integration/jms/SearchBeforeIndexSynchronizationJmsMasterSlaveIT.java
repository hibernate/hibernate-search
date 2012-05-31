package org.hibernate.search.test.integration.jms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * In a JMS Master/Slave configuration, it's not possible for a node to find any
 * entity before the synchronization of the indexes.
 *
 * The refresh period is set to a very high value ({@link Integer#MAX_VALUE}) so
 * that we can be confident it's executed before the
 * synchronization of the indexes occurs.
 *
 * @author "Davide D'Alto"
 */
@RunWith(Arquillian.class)
public class SearchBeforeIndexSynchronizationJmsMasterSlaveIT {

	private static final int INFINITE_REFRESH_PERIOD = Integer.MAX_VALUE;

	@Deployment(name = "master", order = 1 )
	public static Archive<?> createDeploymentMaster() throws Exception {
		return DeploymentJmsMasterSlave.createMaster("master", INFINITE_REFRESH_PERIOD);
	}

	@Deployment(name = "slave-1", order = 2)
	public static Archive<?> createDeploymentSlave1() throws Exception {
		return DeploymentJmsMasterSlave.createSlave("slave-1", INFINITE_REFRESH_PERIOD);
	}

	@Deployment(name = "slave-2", order = 3)
	public static Archive<?> createDeploymentSlave2() throws Exception {
		return DeploymentJmsMasterSlave.createSlave("slave-2", INFINITE_REFRESH_PERIOD);
	}

	@Inject
	RegistrationController memberRegistration;

	@Test
	@InSequence(0)
	@OperateOnDeployment("master")
	public void deleteExistingMembers() throws Exception {
		memberRegistration.deleteAllMembers();
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
	@OperateOnDeployment("master")
	public void searchNewMembersBeforeSynchronizationOnMaster() throws Exception {
		{
			List<RegisteredMember> result = search("Davide");
			Assert.assertEquals("Found user created by Slave 1: " + result, 0, result.size());
		}
		{
			List<RegisteredMember> result = search("Peter");
			Assert.assertEquals("Found user created by Slave 2: " + result, 0, result.size());
		}
		{
			List<RegisteredMember> result = search("Richard");
			Assert.assertEquals("Found user created by Master: " + result, 0, result.size());
		}
	}

	@Test
	@InSequence(5)
	@OperateOnDeployment("slave-1")
	public void searchNewMemberBeforeSynchronizationOnSlave1() throws Exception {
		{
			List<RegisteredMember> result = search("Davide");
			Assert.assertEquals("Found user created by Slave 1: " + result, 0, result.size());
		}
		{
			List<RegisteredMember> result = search("Peter");
			Assert.assertEquals("Found user created by Slave 2: " + result, 0, result.size());
		}
		{
			List<RegisteredMember> result = search("Richard");
			Assert.assertEquals("Found user created by Master: " + result, 0, result.size());
		}
	}

	@Test
	@InSequence(6)
	@OperateOnDeployment("slave-2")
	public void searchNewMemberBeforeSynchronizationOnSlave2() throws Exception {
		{
			List<RegisteredMember> result = search("Davide");
			Assert.assertEquals("Found user created by Slave 1: " + result, 0, result.size());
		}
		{
			List<RegisteredMember> result = search("Peter");
			Assert.assertEquals("Found user created by Slave 2: " + result, 0, result.size());
		}
		{
			List<RegisteredMember> result = search("Richard");
			Assert.assertEquals("Found user created by Master: " + result, 0, result.size());
		}
	}

	private List<RegisteredMember> search(String name) throws InterruptedException {
		return memberRegistration.search( name );
	}
}
