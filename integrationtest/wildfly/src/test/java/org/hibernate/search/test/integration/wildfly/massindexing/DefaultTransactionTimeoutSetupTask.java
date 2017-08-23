/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.massindexing;

import java.util.logging.Logger;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * Sets the default transaction timeout to 2000 milliseconds.
 */
public class DefaultTransactionTimeoutSetupTask implements ServerSetupTask {

	private static final ModelNode ADDRESS_TRANSACTIONS_SUBSYSTEM = PathAddress.pathAddress( "subsystem", "transactions" ).toModelNode();

	private Logger logger = Logger.getLogger( DefaultTransactionTimeoutSetupTask.class.getName() );

	@Override
	public void setup(ManagementClient managementClient, String s) throws Exception {
		ModelNode op = Operations.createWriteAttributeOperation( ADDRESS_TRANSACTIONS_SUBSYSTEM, "default-timeout", 2000 );
		ModelNode result = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( result ) ) {
			logger.warning( "Can't set default transaction timeout: " + result.toJSONString( false ) );
		}
	}

	@Override
	public void tearDown(ManagementClient managementClient, String s) throws Exception {
		ModelNode op = Operations.createUndefineAttributeOperation( ADDRESS_TRANSACTIONS_SUBSYSTEM, "default-timeout" );
		ModelNode result = managementClient.getControllerClient().execute( op );
		if ( !Operations.isSuccessfulOutcome( result ) ) {
			logger.warning( "Can't reset default transaction timeout: " + result.toJSONString( false ) );
		}
	}

}
