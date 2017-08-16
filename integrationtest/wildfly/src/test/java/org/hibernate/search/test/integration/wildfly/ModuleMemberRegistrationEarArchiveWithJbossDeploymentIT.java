/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.wildfly.controller.MemberRegistration;
import org.hibernate.search.test.integration.wildfly.model.Member;
import org.hibernate.search.test.integration.wildfly.util.Resources;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Example of an integration test using JBoss AS 7 and Arquillian.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT {

	private static final String EXPECTED_SEARCH_VERSION_RESOURCE = "expectedHibernateSearchVersion";

	@Deployment
	public static Archive<?> createTestArchive() throws IllegalArgumentException, IOException {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, "ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT.war" )
				.addAsWebInfResource( webXml(), "web.xml" );

		JavaArchive ejb = ShrinkWrap
				.create( JavaArchive.class, "ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT.jar" )
				.addClasses( ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT.class, Member.class, MemberRegistration.class, Resources.class )
				.addAsManifestResource( persistenceXml(), "persistence.xml" )
				.addAsManifestResource( EmptyAsset.INSTANCE, "beans.xml" );

		String applicationXml = Descriptors.create( ApplicationDescriptor.class )
				.createModule()
					.ejb( ejb.getName() ).up()
				.createModule()
					.getOrCreateWeb()
						.webUri( war.getName() ).up().up()
				.exportAsString();

		EnterpriseArchive ear = ShrinkWrap
				.create( EnterpriseArchive.class, ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT.class.getSimpleName() + ".ear" )
				.addAsModules( ejb )
				.addAsModule( war )
				.addAsResource( jbossDeploymentXml(), "/jboss-deployment-structure.xml" )
				.setApplicationXML( new StringAsset( applicationXml ) );
		return ear;
	}

	private static Asset webXml() {
		String webXml = Descriptors.create( org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor.class )
			.createEnvEntry()
				.envEntryName( EXPECTED_SEARCH_VERSION_RESOURCE )
				.envEntryValue(
						"main".equals( VersionTestHelper.getModuleSlotString() ) ?
								VersionTestHelper.getDependencyVersionHibernateSearchBuiltIn() :
								VersionTestHelper.getDependencyVersionHibernateSearch()
				)
				.envEntryType( "java.lang.String" )
				.up()
			.exportAsString();
		return new StringAsset( webXml );
	}

	private static Asset jbossDeploymentXml() throws IOException {
		String text;
		try ( InputStream inputStream = ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT.class.getClassLoader().getResourceAsStream( "jboss-deployment-structure-ModuleMemberRegistrationEarArchiveWithJbossDeploymentIT.xml" ) ) {
			try ( Scanner scanner = new Scanner( inputStream, "UTF-8" ) ) {
				text = scanner.useDelimiter( "\\A" ).next();
			}
		}
		String finalXml = text.replace( (CharSequence)"${project.slot}", (CharSequence)VersionTestHelper.getModuleSlotString() );
		return new StringAsset( finalXml );
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				// The deployment Scanner is disabled as the JipiJapa integration is not available because of the custom Hibernate ORM module:
				.clazz( Member.class.getName() )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Resource(name = EXPECTED_SEARCH_VERSION_RESOURCE)
	String expectedSearchVersion;

	@Inject
	MemberRegistration memberRegistration;

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
	public void testNonExistingMember() throws Exception {
		List<Member> search = memberRegistration.search( "TotallyInventedName" );

		assertNotNull( "Search should never return null", search );
		assertTrue( "Search results should be empty", search.isEmpty() );
	}
}
