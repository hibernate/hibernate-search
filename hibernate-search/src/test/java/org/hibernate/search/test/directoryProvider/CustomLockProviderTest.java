/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.directoryProvider;

import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sanne Grinovero
 */
public class CustomLockProviderTest {
	
	@Test
	public void testUseOfCustomLockingFactory() {
		assertNull( CustomLockFactoryFactory.optionValue );
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		builder
			.addAnnotatedClass( SnowStorm.class )
			.setProperty( "hibernate.search.default.locking_option", "somethingHere" )
			.setProperty( "hibernate.search.default.locking_strategy", "org.hibernate.search.test.directoryProvider.CustomLockFactoryFactory")
			.build();
		builder.close();
		assertEquals( "somethingHere", CustomLockFactoryFactory.optionValue );
	}

	@Test
	public void testFailOnInexistentLockingFactory() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		try {
			builder
				.addAnnotatedClass( SnowStorm.class )
				.setProperty( "hibernate.search.default.locking_option", "somethingHere" )
				.setProperty( "hibernate.search.default.locking_strategy", "org.hibernate.NotExistingFactory")
				.build();
			builder.close();
			fail();
		}
		catch (org.hibernate.HibernateException e) {
			Throwable causeSearch = e.getCause();
			assertNotNull( causeSearch );
			assertTrue( causeSearch instanceof org.hibernate.search.SearchException );
			assertEquals( "Unable to find locking_strategy implementation class: org.hibernate.NotExistingFactory", causeSearch.getMessage() );
		}
	}

}
