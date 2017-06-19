/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import org.hibernate.search.test.integration.wildfly.PackagerHelper;
import org.hibernate.search.test.integration.wildfly.cdi.i18n.InternationalizedValue;
import org.hibernate.search.test.integration.wildfly.cdi.model.EntityWithCDIAwareBridges;
import org.hibernate.search.test.integration.wildfly.cdi.model.EntityWithCDIAwareBridgesDao;
import org.hibernate.search.test.integration.wildfly.model.Member;
import org.hibernate.search.testsupport.TestForIssue;
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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Yoann Rodiere
 */
@RunWith(Arquillian.class)
@TestForIssue(jiraKey = "HSEARCH-1316")
public class CDIInjectionIT {

	@Deployment
	public static Archive<?> createTestArchive() throws Exception {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, CDIInjectionIT.class.getSimpleName() + ".war" )
				.addPackages( true /* recursive */, CDIInjectionIT.class.getPackage() )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsResource( "cdi/META-INF/services", "META-INF/services" )
				.addAsWebInfResource( "jboss-deployment-structure-hcann.xml", "/jboss-deployment-structure.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchTestingLibraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return archive;
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				// The deployment Scanner is disabled as the JipiJapa integration is not available because of the custom Hibernate ORM module:
				.clazz( Member.class.getName() )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Inject
	private EntityWithCDIAwareBridgesDao dao;

	@After
	public void cleanupDatabase() {
		dao.deleteAll();
	}

	@Test
	public void injectedFieldBridge() throws InterruptedException {
		Function<String, List<EntityWithCDIAwareBridges>> search = dao::searchFieldBridge;

		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithCDIAwareBridges entity = new EntityWithCDIAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithCDIAwareBridges entity2 = new EntityWithCDIAwareBridges();
		entity2.setInternationalizedValue( InternationalizedValue.GOODBYE );
		dao.create( entity2 );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );

		dao.delete( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );
	}

	@Test
	public void injectedClassBridge() throws InterruptedException {
		Function<String, List<EntityWithCDIAwareBridges>> search = dao::searchClassBridge;

		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithCDIAwareBridges entity = new EntityWithCDIAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithCDIAwareBridges entity2 = new EntityWithCDIAwareBridges();
		entity2.setInternationalizedValue( InternationalizedValue.GOODBYE );
		dao.create( entity2 );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );

		dao.delete( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );
	}
}
