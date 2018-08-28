/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.integration.arquillian.WildFlyConfigurationExtension;
import org.hibernate.search.test.integration.wildfly.model.Member;
import org.hibernate.search.test.integration.wildfly.util.ManagementClientHelper;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;

/**
 * Test that a failure to bootstrap Hibernate Search leads to the deployment
 * being marked as failed and the failure appearing in the logs.
 *
 * @author Yoann Rodiere
 */
@RunWith(Arquillian.class)
@RunAsClient
@TestForIssue( jiraKey = "HSEARCH-3163" )
public class BootstrapFailureIT {

	private static final Logger log = Logger.getLogger( BootstrapFailureIT.class );

	private static final String FAILING_DEPLOYMENT = "failing_deployment";

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Deployment(name = FAILING_DEPLOYMENT, managed = false, testable = false)
	public static Archive<?> createFailingArchive() {
		return ShrinkWrap
				.create( WebArchive.class, BootstrapFailureIT.class.getSimpleName() + ".war" )
				// No need to add the test class, we just want a failing deployment
				// ... but we still need an indexed entity
				.addClass( Member.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" );
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( WildFlyConfigurationExtension.DATA_SOURCE_JNDI_NAME )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					// FAILURE HERE: This configuration property should make bootstrapping fail
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "SOME_INVALID_VALUE" )
					.up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() )
					.up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@ArquillianResource
	private Deployer deployer;

	@ArquillianResource
	private ManagementClient managementClient;

	@Test
	public void bootstrapFailure() {
		ManagementClientHelper helper = new ManagementClientHelper( managementClient );
		deployFailingDeployment();
		assertThat( helper.getAllLogs() ).contains(
				"Caused by: " + SearchException.class.getName()
				+ ": HSEARCH000103: Unable to initialize IndexManager named '"
				+ Member.class.getName() + "'"
		);
	}

	private void deployFailingDeployment() {
		Exception caughtException = null;
		try {
			deployer.deploy( FAILING_DEPLOYMENT );
		}
		catch (Exception e) {
			caughtException = e;
		}
		if ( caughtException == null ) {
			try {
				deployer.undeploy( FAILING_DEPLOYMENT );
			}
			catch (RuntimeException e) {
				log.warn( "Error undeploying a deployment", e );
			}
			Assert.fail(
					"Deployment " + FAILING_DEPLOYMENT + " should have failed, but it didn't."
							+ " There is something wrong with this test."
			);
		}
	}

}
