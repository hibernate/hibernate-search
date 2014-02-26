/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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


