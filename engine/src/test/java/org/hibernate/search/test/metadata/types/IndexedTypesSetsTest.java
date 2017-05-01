/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata.types;

import java.util.Collections;

import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.impl.IndexedTypesSets;
import org.hibernate.search.test.metadata.Foo;
import org.junit.Assert;
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
		final IndexedTypeSet fromClasses = IndexedTypesSets.fromClasses( IndexedTypesSetsTest.class, Foo.class );
		final String tostring = fromClasses.toString();
		//being a set, the order is unspecified. The toString output is going to match A,B or B,A :
		Assert.assertTrue(
				"[org.hibernate.search.test.metadata.Foo, org.hibernate.search.test.metadata.types.IndexedTypesSetsTest]".equals( tostring ) ||
				"[org.hibernate.search.test.metadata.types.IndexedTypesSetsTest, org.hibernate.search.test.metadata.Foo]".equals( tostring ) );
	}

	@Test
	public void testCreationOfEmptySets() {
		//This is more to test about creation from special parameters than to test the isEmpty() method
		Assert.assertTrue( IndexedTypesSets.fromClasses().isEmpty() );
		Assert.assertTrue( IndexedTypesSets.empty().isEmpty() );
		Assert.assertTrue( IndexedTypesSets.fromIdentifiers( Collections.emptySet() ).isEmpty() );
	}

	@Test
	public void testSize() {
		Assert.assertEquals( 2, IndexedTypesSets.fromClasses( IndexedTypesSetsTest.class, Foo.class ).size() );
		Assert.assertEquals( 1, IndexedTypesSets.fromClasses( Foo.class, Foo.class ).size() );
		Assert.assertEquals( 0, IndexedTypesSets.fromClasses( ).size() );
	}

	@Test
	public void testEquality() {
		final IndexedTypeSet a = IndexedTypesSets.fromClasses( IndexedTypesSetsTest.class, Foo.class );
		final IndexedTypeSet b = IndexedTypesSets.fromClasses( Foo.class, IndexedTypesSetsTest.class );
		final IndexedTypeSet c = IndexedTypesSets.fromClasses( Foo.class );
		Assert.assertEquals( a, b );
		Assert.assertEquals( b, a );
		Assert.assertEquals( a, a );
		Assert.assertNotEquals( a, c );
		Assert.assertNotEquals( b, c );
	}

}

