/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.logging.impl;

import java.util.Properties;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.ClassFormatter;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jgroups.Address;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Hibernate Search JGroup backend log abstraction.
 *
 * @author Hardy Ferentschik
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends org.hibernate.search.util.logging.impl.Log {

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 1,
			value = "Remote JGroups peer '%1$s' is suspected to have left '")
	SuspectedException jgroupsSuspectingPeer(Address sender);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 2,
			value = "Timeout sending synchronous message to JGroups peer '%1$s''")
	TimeoutException jgroupsRpcTimeout(Address sender);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 3,
			value = "Exception reported from remote JGroups node '%1$s' : '%2$s'")
	SearchException jgroupsRemoteException(Address sender, Throwable exception, @Cause Throwable cause);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 4,
			value = "Unable to send Lucene update work via JGroups cluster")
	SearchException unableToSendWorkViaJGroups(@Cause Throwable e);

	@LogMessage(level = WARN)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 5, value = "Received null or empty Lucene works list in message.")
	void receivedEmptyLuceneWorksInMessage();

	@LogMessage(level = INFO)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 6, value = "Received new cluster view: %1$s")
	void jGroupsReceivedNewClusterView(Object view);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 7, value = "Configured JGroups channel is a Muxer! MuxId option is required: define '%s'.")
	SearchException missingJGroupsMuxId(String muxId);

	@LogMessage(level = DEBUG)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 8, value = "Starting JGroups ChannelProvider")
	void jGroupsStartingChannelProvider();

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 9, value = "MuxId '%1$d' configured on the JGroups was already taken. Can't register handler!")
	SearchException jGroupsMuxIdAlreadyTaken(short n);

	@LogMessage(level = WARN)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 10,
			value = "FLUSH is not present in your JGroups stack! FLUSH is needed to ensure messages are not dropped while new nodes join the cluster. Will proceed, but inconsistencies may arise!")
	void jGroupsFlushNotPresentInStack();

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 11, value = "Using JGroups channel having configuration '%1$s'")
	void jgroupsFullConfiguration(String printProtocolSpecAsXML);

	@LogMessage(level = ERROR)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 12, value = "Problem closing channel; setting it to null")
	void jGroupsClosingChannelError(@Cause Exception toLog);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 13, value = "Object injected for JGroups channel in %1$s is of an unexpected type %2$s (expecting org.jgroups.JChannel)")
	SearchException jGroupsChannelInjectionError(String channelInject, @Cause Exception e, @FormatWith(ClassFormatter.class) Class<?> actualType);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 14, value = "Starting JGroups channel using configuration '%1$s'")
	void startingJGroupsChannel(Object cfg);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 15, value = "Error while trying to create a channel using config file: %1$s")
	SearchException jGroupsChannelCreationUsingFileError(String configuration, @Cause Throwable e);

	@LogMessage(level = INFO)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 16,
			value = "Unable to use any JGroups configuration mechanisms provided in properties %1$s. Using default JGroups configuration file!")
	void jGroupsConfigurationNotFoundInProperties(Properties props);

	@LogMessage(level = INFO)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 17, value = "Disconnecting and closing JGroups Channel to cluster '%1$s'")
	void jGroupsDisconnectingAndClosingChannel(String clusterName);

	@LogMessage(level = WARN)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 18,
			value = "Default JGroups configuration file was not found. Attempt to start JGroups channel with default configuration!")
	void jGroupsDefaultConfigurationFileNotFound();

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 19, value = "Unable to start JGroups channel")
	SearchException unableToStartJGroupsChannel(@Cause Throwable e);

	@LogMessage(level = INFO)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 20, value = "Connected to cluster [ %1$s ]. The local Address is %2$s")
	void jGroupsConnectedToCluster(String clusterName, Object address);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 21, value = "Unable to connect to: [%1$s] JGroups channel")
	SearchException unableConnectingToJGroupsCluster(String clusterName, @Cause Throwable e);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 22, value = "JGroups backend configured for index '%1$s' using block_for_ack '%2$s'")
	void jgroupsBlockWaitingForAck(String indexName, boolean block);

	@Message(id = JGROUPS_BACKEND_MESSAGES_START_ID + 23, value = "JGroups channel configuration should be specified in the global section [hibernate.search.services.jgroups.], " +
			"not as an IndexManager property for index '%1$s'. See http://docs.jboss.org/hibernate/search/5.0/reference/en-US/html_single/#jgroups-backend")
	SearchException legacyJGroupsConfigurationDefined(String indexName);
}
