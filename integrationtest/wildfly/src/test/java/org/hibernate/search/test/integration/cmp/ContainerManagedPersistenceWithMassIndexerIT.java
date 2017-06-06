/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.cmp;

import javax.inject.Inject;

import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.application6.ApplicationDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-640")
@RunWith(Arquillian.class)
public class ContainerManagedPersistenceWithMassIndexerIT {
	private static final String NAME = ContainerManagedPersistenceWithMassIndexerIT.class.getSimpleName();
	private static final String EAR_ARCHIVE_NAME = NAME + ".ear";
	private static final String WAR_ARCHIVE_NAME = NAME + ".war";
	private static final String EJB_ARCHIVE_NAME = NAME + ".jar";

	// as in the original test case for HSEARCH-640 we are using a full ear archive for testing
	@Deployment
	public static EnterpriseArchive createTestEAR() {
		JavaArchive ejb = ShrinkWrap
				.create( JavaArchive.class, EJB_ARCHIVE_NAME )
				.addClasses( Singer.class, SingersSingleton.class );

		WebArchive war = ShrinkWrap
				.create( WebArchive.class, WAR_ARCHIVE_NAME )
				.addClasses( ContainerManagedPersistenceWithMassIndexerIT.class )
				.addAsResource( warManifest(), "META-INF/MANIFEST.MF" )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );

		return ShrinkWrap.create( EnterpriseArchive.class, EAR_ARCHIVE_NAME )
				.addAsModules( ejb )
				.addAsModule( war )
				.setApplicationXML( applicationXml() );
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "cmt-test" )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.clazz( Singer.class.getName() )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "hibernate.search.indexing_strategy" ).value( "manual" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
				.exportAsString();
		return new StringAsset( persistenceXml );
	}

	private static Asset applicationXml() {
		String applicationXml = Descriptors.create( ApplicationDescriptor.class )
				.applicationName( NAME )
				.createModule()
				.ejb( EJB_ARCHIVE_NAME )
				.getOrCreateWeb()
				.webUri( WAR_ARCHIVE_NAME )
				.contextRoot( "test" )
				.up().up()
				.exportAsString();
		return new StringAsset( applicationXml );
	}

	private static Asset warManifest() {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.addToClassPath( EJB_ARCHIVE_NAME )
				.exportAsString();
		return new StringAsset( manifest );
	}

	@Inject
	private SingersSingleton singersEjb;

	@Test
	public void testMassIndexerWorksInCMP() throws Exception {
		assertNotNull( singersEjb );
		singersEjb.insertContact( "John", "Lennon" );
		singersEjb.insertContact( "Paul", "McCartney" );
		singersEjb.insertContact( "George", "Harrison" );
		singersEjb.insertContact( "Ringo", "Starr" );

		assertEquals( "Don't you know the Beatles?", 4, singersEjb.listAllContacts().size() );
		assertEquals( "Beatles should not yet be indexed", 0, singersEjb.searchAllContacts().size() );
		assertTrue( "Indexing the Beatles failed.", singersEjb.rebuildIndex() );
		assertEquals( "Now the Beatles should be indexed", 4, singersEjb.searchAllContacts().size() );
	}
}


