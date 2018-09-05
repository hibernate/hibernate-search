/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.arquillian;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.hibernate.search.util.StringHelper;

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
 * Configures WildFly so that the data source exists.
 */
class DataSourceConfigurator {

	private static Logger log = Logger.getLogger( DataSourceConfigurator.class.getName() );

	static final String DATA_SOURCE_JNDI_NAME = "java:/HibernateSearchTest";

	private static final ModelNode DATA_SOURCE_ADDRESS = PathAddress
			.pathAddress( "subsystem", "datasources" )
			.append( "data-source", "HibernateSearchTest" )
			.toModelNode();

	@Inject
	private Instance<ManagementClient> managementClient;

	public void afterStart(@Observes AfterStart event, ArquillianDescriptor descriptor) throws IOException {
		ModelNode result = managementClient.get().getControllerClient()
				.execute( createAddDataSourceOperation() );
		if ( !Operations.isSuccessfulOutcome( result ) ) {
			log.severe( "Can't create data source: " + result.toJSONString( false ) );
		}

	}

	public void beforeShutdown(@Observes BeforeStop event, ArquillianDescriptor descriptor) throws IOException {
		ModelNode result = managementClient.get().getControllerClient()
				.execute( Operations.createRemoveOperation( DATA_SOURCE_ADDRESS ) );
		if ( !Operations.isSuccessfulOutcome( result ) ) {
			log.warning( "Can't remove data source: " + result.toJSONString( false ) );
		}
	}

	private ModelNode createAddDataSourceOperation() throws IOException {
		ModelNode op = Operations.createAddOperation( DATA_SOURCE_ADDRESS );
		op.get( "jndi-name" ).set( DATA_SOURCE_JNDI_NAME );
		addPropertiesToModel( op, "/data-source.properties" );
		return op;
	}

	private void addPropertiesToModel(ModelNode op, String resourcePath) throws IOException {
		Properties properties = new Properties();
		try ( InputStream stream = DataSourceConfigurator.class.getResourceAsStream( resourcePath );
				Reader reader = new InputStreamReader( stream, StandardCharsets.UTF_8 ) ) {
			properties.load( reader );
		}
		for ( Map.Entry<Object, Object> property : properties.entrySet() ) {
			String name = (String) property.getKey();
			String value = (String) property.getValue();
			if ( StringHelper.isNotEmpty( value ) ) {
				op.get( name ).set( value );
			}
		}
	}

}
