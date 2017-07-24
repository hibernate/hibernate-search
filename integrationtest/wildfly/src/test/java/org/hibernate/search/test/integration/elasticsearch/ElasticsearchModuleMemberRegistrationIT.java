/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.elasticsearch;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.wildfly.controller.MemberRegistration;
import org.hibernate.search.test.integration.wildfly.model.Member;
import org.hibernate.search.test.integration.wildfly.util.Resources;
import org.hibernate.search.testsupport.setup.TestDefaults;
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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing Elasticsearch integration using WildFLy
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class ElasticsearchModuleMemberRegistrationIT {

	private static final String EXPECTED_SEARCH_VERSION_RESOURCE = "expectedHibernateSearchVersion";
	private static final String ES_PROPERTY_PREFIX = "hibernate.search.default.";
	private static final String ES_HOST_PROPERTY = ES_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_URI;
	private static final String ES_USERNAME_PROPERTY = ES_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_USERNAME;
	private static final String ES_PASSWORD_PROPERTY = ES_PROPERTY_PREFIX + ElasticsearchEnvironment.SERVER_PASSWORD;
	private static final String ES_AWS_SIGNING_ENABLED_PROPERTY = ES_PROPERTY_PREFIX + "elasticsearch.aws.signing.enabled";
	private static final String ES_AWS_ACCESS_KEY_PROPERTY = ES_PROPERTY_PREFIX + "elasticsearch.aws.access_key";
	private static final String ES_AWS_SECRET_KEY_PROPERTY = ES_PROPERTY_PREFIX + "elasticsearch.aws.secret_key";
	private static final String ES_AWS_REGION_PROPERTY = ES_PROPERTY_PREFIX + "elasticsearch.aws.region";

	@Deployment
	public static Archive<?> createTestArchive() {
		return ShrinkWrap
				.create( WebArchive.class, ElasticsearchModuleMemberRegistrationIT.class.getSimpleName() + ".war" )
				.addClasses( Member.class, MemberRegistration.class, Resources.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-excludejavassist.xml", "jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsWebInfResource( webXml(), "web.xml" );
	}

	private static Asset webXml() {
		String webXml = Descriptors.create( org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor.class )
			.createEnvEntry()
				.envEntryName( EXPECTED_SEARCH_VERSION_RESOURCE )
				.envEntryValue( VersionTestHelper.getDependencyVersionHibernateSearch() )
				.envEntryType( "java.lang.String" )
				.up()
			.exportAsString();
		return new StringAsset( webXml );
	}

	private static Asset persistenceXml() {
		Properties testDefaults = TestDefaults.getProperties();
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				// The deployment Scanner is disabled as the JipiJapa integration is not available because of the custom Hibernate ORM module:
				.clazz( Member.class.getName() )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.indexmanager" ).value( "elasticsearch" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY )
						.value( IndexSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP.getExternalName() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
					.createProperty().name( "hibernate.search.default.elasticsearch.required_index_status" ).value( "yellow" ).up()
					.createProperty().name( "hibernate.search.default.elasticsearch.refresh_after_write" ).value( "true" ).up()
					.createProperty().name( ES_HOST_PROPERTY ).value( testDefaults.getProperty( ES_HOST_PROPERTY ) ).up()
					.createProperty().name( ES_USERNAME_PROPERTY ).value( testDefaults.getProperty( ES_USERNAME_PROPERTY ) ).up()
					.createProperty().name( ES_PASSWORD_PROPERTY ).value( testDefaults.getProperty( ES_PASSWORD_PROPERTY ) ).up()
					.createProperty().name( ES_AWS_SIGNING_ENABLED_PROPERTY ).value( testDefaults.getProperty( ES_AWS_SIGNING_ENABLED_PROPERTY ) ).up()
					.createProperty().name( ES_AWS_ACCESS_KEY_PROPERTY ).value( testDefaults.getProperty( ES_AWS_ACCESS_KEY_PROPERTY ) ).up()
					.createProperty().name( ES_AWS_SECRET_KEY_PROPERTY ).value( testDefaults.getProperty( ES_AWS_SECRET_KEY_PROPERTY ) ).up()
					.createProperty().name( ES_AWS_REGION_PROPERTY ).value( testDefaults.getProperty( ES_AWS_REGION_PROPERTY ) ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Inject
	MemberRegistration memberRegistration;

	@Resource(name = EXPECTED_SEARCH_VERSION_RESOURCE)
	String expectedSearchVersion;

	@Test
	public void HibernateSearchVersion() throws Exception {
		assertEquals( expectedSearchVersion, memberRegistration.getHibernateSearchVersionString() );
	}

	@Test
	public void testRegister() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Davide D'Alto" );
		newMember.setEmail( "davide@mailinator.com" );
		newMember.setPhoneNumber( "2125551234" );
		memberRegistration.register();

		assertNotNull( newMember.getId() );
	}

	@Test
	public void testNewMemberSearch() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Peter O'Tall" );
		newMember.setEmail( "peter@mailinator.com" );
		newMember.setPhoneNumber( "4643646643" );
		memberRegistration.register();

		List<Member> search = memberRegistration.search( "Peter" );

		assertFalse( "Expected at least one result after the indexing", search.isEmpty() );
		assertEquals( "Search hasn't found a new member", newMember.getName(), search.get( 0 ).getName() );
	}

	@Test
	public void testUnexistingMember() throws Exception {
		List<Member> search = memberRegistration.search( "TotallyInventedName" );

		assertNotNull( "Search should never return null", search );
		assertTrue( "Search results should be empty", search.isEmpty() );
	}
}
