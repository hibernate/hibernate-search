/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.typehandling;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.impl.IndexedTypesSets;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.junit.Assert;
import org.junit.Test;

public class BasicTypeCollectionsTest {

	private static final IndexedTypeIdentifier TYPE_A = new PojoIndexedTypeIdentifier( BasicTypeCollectionsTest.class );
	private static final IndexedTypeIdentifier TYPE_B = new PojoIndexedTypeIdentifier( String.class );

	@Test
	public void emptyStream() {
		assertIsEmpty( Collections.<IndexedTypeIdentifier>emptyList().stream().collect( IndexedTypesSets.streamCollector() ) );
	}

	@Test
	public void nullVararg() {
		assertIsEmpty( IndexedTypesSets.fromClasses( null ) );
	}

	@Test
	public void emptyArray() {
		assertIsEmpty( IndexedTypesSets.fromIdentifiers( new IndexedTypeIdentifier[0] ) );
	}

	@Test
	public void emptyIterable() {
		assertIsEmpty( IndexedTypesSets.fromIdentifiers( Collections.<IndexedTypeIdentifier>emptyList() ) );
	}

	@Test
	public void singleElementClass() {
		assertIsSingletonSet( IndexedTypesSets.fromClass( BasicTypeCollectionsTest.class ), BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void singleElementIterable() {
		assertIsSingletonSet( IndexedTypesSets.fromIdentifiers( TYPE_A.asTypeSet() ), BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void singleElementStream() {
		assertIsSingletonSet(
				Collections.<IndexedTypeIdentifier>singleton( TYPE_A ).stream().collect( IndexedTypesSets.streamCollector() ),
				BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void singleElementArray() {
		assertIsSingletonSet(
				IndexedTypesSets.fromIdentifiers( new IndexedTypeIdentifier[] { TYPE_A } ),
				BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void compositeEmpty() {
		assertIsEmpty( IndexedTypesSets.composite( IndexedTypesSets.empty(), IndexedTypesSets.empty() ) );
	}

	@Test
	public void compositeSingle() {
		assertIsSingletonSet( IndexedTypesSets.composite( TYPE_A.asTypeSet(), TYPE_A ), BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void compositeSingleAndEmpty() {
		assertIsSingletonSet( IndexedTypesSets.composite( TYPE_A.asTypeSet(), IndexedTypesSets.empty() ), BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void subtractionEmpty() {
		assertIsEmpty( IndexedTypesSets.subtraction( IndexedTypesSets.empty(), IndexedTypesSets.empty() ) );
	}

	@Test
	public void subtractionOfSingletons() {
		assertIsEmpty( IndexedTypesSets.subtraction( TYPE_A.asTypeSet(), TYPE_A.asTypeSet() ) );
	}

	@Test
	public void subtractionOfSingletonFromEmpty() {
		assertIsEmpty( IndexedTypesSets.subtraction( IndexedTypesSets.empty(), TYPE_A.asTypeSet() ) );
	}

	@Test
	public void subtractionOfEmptyFromSingleton() {
		assertIsSingletonSet( IndexedTypesSets.subtraction( TYPE_A.asTypeSet(), IndexedTypesSets.empty() ), BasicTypeCollectionsTest.class, true );
	}

	@Test
	public void buildCoupleSet() {
		assertIsDoubleSet( IndexedTypesSets.composite( TYPE_A.asTypeSet(), TYPE_B ) );
	}

	@Test
	public void subtractionOfOneFromCouple() {
		assertIsSingletonSet( IndexedTypesSets.subtraction( IndexedTypesSets.composite( TYPE_A.asTypeSet(), TYPE_B ), TYPE_B.asTypeSet() ), TYPE_A.getPojoType(), true );
		assertIsSingletonSet( IndexedTypesSets.subtraction( IndexedTypesSets.composite( TYPE_A.asTypeSet(), TYPE_B ), TYPE_A.asTypeSet() ), TYPE_B.getPojoType(), true );
	}

	private void assertIsDoubleSet(IndexedTypeSet typeSet) {
		Assert.assertFalse( "Verify it's not a singleton", typeSet == IndexedTypesSets.empty() );
		Assert.assertFalse( typeSet.isEmpty() );
		Assert.assertEquals( 2, typeSet.size() );
		Iterator<IndexedTypeIdentifier> iterator = typeSet.iterator();
		Assert.assertTrue( iterator.hasNext() );
		IndexedTypeIdentifier firstElement = iterator.next(); // increment once
		IndexedTypeIdentifier secondElement = iterator.next(); // increment twice
		Assert.assertFalse( iterator.hasNext() );
		iterator.forEachRemaining( l -> Assert.fail( "should never happen" ) ); //no more elements
		Set<Class<?>> pojosSet = typeSet.toPojosSet();
		Assert.assertTrue( pojosSet.contains( TYPE_A.getPojoType() ) );
		Assert.assertTrue( pojosSet.contains( TYPE_B.getPojoType() ) );
		Assert.assertEquals( 2, pojosSet.size() );
	}

	private void assertIsEmpty(IndexedTypeSet typeSet) {
		Assert.assertTrue( "Verify the singleton optimisation applies", typeSet == IndexedTypesSets.empty() );
		Assert.assertTrue( typeSet.isEmpty() );
		Assert.assertEquals( 0, typeSet.size() );
		typeSet.iterator().forEachRemaining( l -> Assert.fail( "should never happen" ) );
		Assert.assertEquals( 0, typeSet.toPojosSet().size() );
	}

	private void assertIsSingletonSet(IndexedTypeSet typeSet, Class<?> someType, boolean recursive) {
		Assert.assertFalse( "Verify it's not a singleton", typeSet == IndexedTypesSets.empty() );
		Assert.assertFalse( typeSet.isEmpty() );
		Assert.assertEquals( 1, typeSet.size() );
		Iterator<IndexedTypeIdentifier> iterator = typeSet.iterator();
		Assert.assertTrue( iterator.hasNext() );
		IndexedTypeIdentifier firstElement = iterator.next(); // increment once
		Assert.assertFalse( iterator.hasNext() );
		iterator.forEachRemaining( l -> Assert.fail( "should never happen" ) ); //no more elements
		Set<Class<?>> pojosSet = typeSet.toPojosSet();
		Assert.assertTrue( pojosSet.contains( someType ) );
		Assert.assertEquals( 1, pojosSet.size() );
		IndexedTypeSet typeSet2 = firstElement.asTypeSet();
		if ( recursive ) {
			assertIsSingletonSet( typeSet2, someType, false );
		}
		Assert.assertEquals( typeSet2, typeSet2 );
		Assert.assertEquals( firstElement.getPojoType(), someType );
	}


}
