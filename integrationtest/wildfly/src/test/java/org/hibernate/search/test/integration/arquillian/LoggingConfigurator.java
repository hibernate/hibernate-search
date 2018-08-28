/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.arquillian;

import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * Configures WildFly so that the org.hibernate.search logger exists for the DEBUG level.
 */
class LoggingConfigurator {

	@Inject
	private Instance<ManagementClient> managementClient;

	private Logger logger = Logger.getLogger( LoggingConfigurator.class.getName() );

	private final ModelNode ADDRESS_HIBERNATE_SEARCH_LOGGER = PathAddress
			.pathAddress( "subsystem", "logging" )
			.append( "logger", "org.hibernate.search" )
			.toModelNode();

	private final ModelNode ADDRESS_WELD_CLASS_LOADING_LOGGER = PathAddress
			.pathAddress( "subsystem", "logging" )
			.append( "logger", "org.jboss.weld.ClassLoading" )
			.toModelNode();

	public void afterStart(@Observes AfterStart event, ArquillianDescriptor descriptor) throws IOException {
		addLogger( ADDRESS_HIBERNATE_SEARCH_LOGGER, "DEBUG" );
		addLogger( ADDRESS_WELD_CLASS_LOADING_LOGGER, "DEBUG" );
	}

	public void beforeShutdown(@Observes BeforeStop event, ArquillianDescriptor descriptor) throws IOException {
		removeLogger( ADDRESS_WELD_CLASS_LOADING_LOGGER );
		removeLogger( ADDRESS_HIBERNATE_SEARCH_LOGGER );
	}

	private void addLogger(ModelNode node, String loggerLevel) throws IOException {
		ModelNode op = Operations.createAddOperation( node );
		op.get( "level" ).set( loggerLevel );
		final ModelNode result = managementClient.get().getControllerClient()
				.execute( op );
		if ( !Operations.isSuccessfulOutcome( result ) ) {
			logger.warning( "Can't create " + node + " logger: " + result.toJSONString( false ) );
		}
	}

	private void removeLogger(ModelNode node) throws IOException {
		final ModelNode result = managementClient.get().getControllerClient()
				.execute( Operations.createRemoveOperation( node ) );
		if ( !Operations.isSuccessfulOutcome( result ) ) {
			logger.warning( "Can't remove " + node + " logger: " + result.toJSONString( false ) );
		}
	}

}
