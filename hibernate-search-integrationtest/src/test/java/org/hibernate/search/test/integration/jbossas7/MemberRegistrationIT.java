/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.search.test.integration.jbossas7;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.search.Version;
import org.hibernate.search.test.integration.jbossas7.controller.MemberRegistration;
import org.hibernate.search.test.integration.jbossas7.model.Member;
import org.hibernate.search.test.integration.jbossas7.util.Resources;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Example of an integration test using JBoss AS 7 and Arquillian.
 * 
 * @author Davide D'Alto
 */
@RunWith(Arquillian.class)
public class MemberRegistrationIT {

	@Deployment
	public static Archive<?> createTestArchive() {
		String currentVersion = Version.getVersionString();
		return ShrinkWrap
				.create( WebArchive.class, MemberRegistrationIT.class.getSimpleName() + ".war" )
				.addClasses( Member.class, MemberRegistration.class, Resources.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsLibraries(
						DependencyResolvers.use( MavenDependencyResolver.class )
								.artifact( "org.hibernate:hibernate-search-orm:" + currentVersion )
								.exclusion( "org.hibernate:hibernate-entitymanager" )
								.exclusion( "org.hibernate:hibernate-core" )
								.exclusion( "org.hibernate:hibernate-search-analyzers" )
								.exclusion( "org.hibernate.common:hibernate-commons-annotations" )
								.exclusion( "org.jboss.logging:jboss-logging" )
								.resolveAs( JavaArchive.class ) )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.persistenceUnit( "primary" )
			.jtaDataSource( "java:jboss/datasources/ExampleDS" )
			.property( "hibernate.hbm2ddl.auto", "create-drop" )
			.property( "hibernate.search.default.directory_provider", "ram" )
			.property( "_hibernate.search.lucene_version_", "_LUCENE_CURRENT_")
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
	public void testUnexistingMember() throws Exception {
		List<Member> search = memberRegistration.search( "TotallyInventedName" );

		assertNotNull( "Search should never return null", search );
		assertTrue( "Search results should be empty", search.isEmpty() );
	}
}
