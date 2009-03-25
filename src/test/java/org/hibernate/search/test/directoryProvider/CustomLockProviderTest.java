// $Id$
package org.hibernate.search.test.directoryProvider;

import junit.framework.TestCase;

import org.hibernate.search.test.util.FullTextSessionBuilder;

/**
 * @author Sanne Grinovero
 */
public class CustomLockProviderTest extends TestCase {
	
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
			Throwable causeLockin = causeSearch.getCause();
			assertNotNull( causeLockin );
			assertTrue( causeLockin.getMessage().startsWith("Unable to find LockFactory") );
		}
	}

}
