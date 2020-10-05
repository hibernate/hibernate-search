/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.impl.IndexedTypeSets;

import org.junit.Test;

/**
 * Verify some key contracts on Class adaptor for IndexedTypesSet
 */
public class IndexedTypesSetsTest {

	/**
	 * We actually verify the toString() of IndexedTypesSets as it's being used in logging
	 * messages and diagnostic output.
	 */
	@Test
	public void testingLogFormat() {
		//take any two random classes:
		final IndexedTypeSet fromClasses = IndexedTypeSets.fromClasses( IndexedTypesSetsTest.class, Foo.class );
		final String tostring = fromClasses.toString();
		//being a set, the order is unspecified. The toString output is going to match A,B or B,A :
		assertTrue(
				"[org.hibernate.search.test.metadata.types.Foo, org.hibernate.search.test.metadata.types.IndexedTypesSetsTest]".equals( tostring ) ||
				"[org.hibernate.search.test.metadata.types.IndexedTypesSetsTest, org.hibernate.search.test.metadata.types.Foo]".equals( tostring ) );
	}

	@Test
	public void testNullTypesAmongArray() {
		//take any two random classes:
		final IndexedTypeSet fromClasses = IndexedTypeSets.fromClasses( IndexedTypesSetsTest.class, null, Foo.class );
		final String tostring = fromClasses.toString();
		//being a set, the order is unspecified. The toString output is going to match A,B or B,A :
		assertTrue(
				"[org.hibernate.search.test.metadata.types.Foo, org.hibernate.search.test.metadata.types.IndexedTypesSetsTest]".equals( tostring ) ||
				"[org.hibernate.search.test.metadata.types.IndexedTypesSetsTest, org.hibernate.search.test.metadata.types.Foo]".equals( tostring ) );
	}

	@Test
	public void testCreationOfEmptySets() {
		//This is more to test about creation from special parameters than to test the isEmpty() method
		assertTrue( IndexedTypeSets.fromClasses().isEmpty() );
		assertTrue( IndexedTypeSets.empty().isEmpty() );
		assertTrue( IndexedTypeSets.fromIdentifiers( Collections.emptySet() ).isEmpty() );
	}

	@Test
	public void testSize() {
		assertEquals( 2, IndexedTypeSets.fromClasses( IndexedTypesSetsTest.class, Foo.class ).size() );
		assertEquals( 1, IndexedTypeSets.fromClasses( Foo.class, Foo.class ).size() );
		assertEquals( 0, IndexedTypeSets.fromClasses( ).size() );
	}

	@Test
	public void testEquality() {
		final IndexedTypeSet a = IndexedTypeSets.fromClasses( IndexedTypesSetsTest.class, Foo.class );
		final IndexedTypeSet b = IndexedTypeSets.fromClasses( Foo.class, IndexedTypesSetsTest.class );
		final IndexedTypeSet c = IndexedTypeSets.fromClasses( Foo.class );
		assertEquals( a, b );
		assertEquals( b, a );
		assertEquals( a, a );
		assertNotEquals( a, c );
		assertNotEquals( b, c );
	}

}

