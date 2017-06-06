/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
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
public class MemberRegistrationIT {

	@Deployment
	public static Archive<?> createTestArchive() throws Exception {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, MemberRegistrationIT.class.getSimpleName() + ".war" )
				.addClasses( Member.class, MemberRegistration.class, Resources.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-hcann.xml", "/jboss-deployment-structure.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchLibraries() )
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
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "none" ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Inject
	MemberRegistration memberRegistration;

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
