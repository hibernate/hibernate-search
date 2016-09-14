/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.test.integration.wildfly.PackagerHelper;
import org.hibernate.search.test.performance.scenario.TestScenario;
import org.hibernate.search.test.performance.scenario.TestScenarioFactory;
import org.hibernate.search.test.performance.util.TargetDirHelper;
import org.hibernate.search.testsupport.TestConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tomas Hradec
 */
@RunWith(Arquillian.class)
public class TestRunnerArquillian {

	private static final TestScenario scenario = TestScenarioFactory.create();

	public static final String RUNNER_PROPERTIES = "runner.properties";
	public static final String TARGET_DIR_KEY = "target";

	@Deployment
	public static Archive<?> createTestArchive() throws Exception {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, TestRunnerArquillian.class.getSimpleName() + ".war" )
				.addPackages( true, TestRunnerArquillian.class.getPackage() )
				.addClass( TestConstants.class )
				.addAsResource( createPersistenceXml(), "META-INF/persistence.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchTestingLibraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.add( manifest(), "META-INF/MANIFEST.MF" )
				.addAsWebInfResource( reportsOutputDirectory(), "classes/" + RUNNER_PROPERTIES );
		return archive;
	}

	private static StringAsset reportsOutputDirectory() throws Exception {
		Path path = TargetDirHelper.getTargetDir();
		String absolutePath = path.toAbsolutePath().toString();
		//Use Properties to make sure we encode the output correctly,
		//especially tricky to deal with escaping of paths:
		Properties runnerProperties = new Properties();
		runnerProperties.put( TARGET_DIR_KEY, absolutePath );
		StringWriter writer = new StringWriter();
		runnerProperties.store( writer, "report output path" );
		String encodedString = writer.toString();
		return new StringAsset( encodedString );
	}

	private static Asset createPersistenceXml() {
		PersistenceDescriptor pd = Descriptors.create( PersistenceDescriptor.class ).version( "2.0" );

		PersistenceUnit<PersistenceDescriptor> pu = pd.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( System.getProperty( "datasource", "java:jboss/datasources/ExampleDS" ) );

		Properties properties = scenario.getHibernateProperties();
		for ( Entry<Object, Object> property : properties.entrySet() ) {
			pu.getOrCreateProperties().
					createProperty().
					name( property.getKey().toString() ).
					value( property.getValue().toString() );
		}
		//Additional properties:
		pu.getOrCreateProperties()
			.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
			.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
			;
		return new StringAsset( pd.exportAsString() );
	}

	public static Asset manifest() {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", "org.apache.commons.lang" )
				.exportAsString();
		return new StringAsset( manifest );
	}

	@PersistenceContext
	private EntityManager em;

	@Test
	public void runPerformanceTest() throws IOException {
		SessionFactory sf = em.unwrap( Session.class ).getSessionFactory();
		scenario.run( sf );
	}

}
