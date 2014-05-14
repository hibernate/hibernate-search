/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import java.util.LinkedHashSet;

import org.hibernate.search.util.impl.AggregatedClassLoader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
public class AggregatedClassLoaderTest {
	private AggregatedClassLoader aggregatedClassLoader;

	@Before
	public void setUp() {
		LinkedHashSet<ClassLoader> orderedClassLoaderSet = new LinkedHashSet<ClassLoader>();
		orderedClassLoaderSet.add( AggregatedClassLoaderTest.class.getClassLoader() );
		orderedClassLoaderSet.add( Thread.currentThread().getContextClassLoader() );
		aggregatedClassLoader = new AggregatedClassLoader(
				orderedClassLoaderSet.toArray(
						new ClassLoader[orderedClassLoaderSet.size()]
				)
		);
	}

	@Test
	public void testLoadingClassMultipleTimes() throws Exception {
		Class<?> fooClass1 = aggregatedClassLoader.loadClass( "org.hibernate.search.test.util.impl.Foo" );
		Class<?> fooClass2 = aggregatedClassLoader.loadClass( "org.hibernate.search.test.util.impl.Foo" );
		assertEquals( fooClass1, fooClass2 );
	}
}


