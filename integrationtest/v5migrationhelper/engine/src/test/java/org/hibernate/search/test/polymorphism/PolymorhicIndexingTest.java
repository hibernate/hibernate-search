/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.polymorphism;

import static org.junit.Assert.assertEquals;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.impl.IndexedTypeSets;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test to verify the contract of {@link org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator#getIndexedTypesPolymorphic(IndexedTypeSet)}
 *
 * @author Sanne Grinovero
 */
public class PolymorhicIndexingTest {

	private Class[] knownTypes = new Class[]{ RootIndexedA.class, RootIndexedB.class,
			ChildOfBIndexed.class, ChildOfBNotIndexed.class, RootNotIndexed.class, IndexedGrandChildOfB.class };

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( knownTypes );

	@Test
	public void testSortingOnNumericInt() {
		expectingForArgument( new Class[]{} ); // expecting an empty set
		expectingForArgument( null ); // expecting an empty set
		expectingForArgument( new Class[]{ Object.class }, RootIndexedA.class, RootIndexedB.class, ChildOfBIndexed.class, IndexedGrandChildOfB.class );
		expectingForArgument( new Class[]{ RootIndexedA.class }, RootIndexedA.class );

		// finds any subtype which is indexed too:
		expectingForArgument( new Class[]{ RootIndexedB.class }, RootIndexedB.class, ChildOfBIndexed.class, IndexedGrandChildOfB.class );

		// does not "walk up" to find the first indexed type, but does walk down:
		expectingForArgument( new Class[]{ ChildOfBNotIndexed.class }, IndexedGrandChildOfB.class );

		// does not include the parent of an indexed target entity:
		expectingForArgument( new Class[]{ ChildOfBIndexed.class }, ChildOfBIndexed.class );

		// does not "walk up" to find the first indexed type:
		expectingForArgument( new Class[]{ IndexedGrandChildOfB.class }, IndexedGrandChildOfB.class );
	}

	private void expectingForArgument(Class[] argument, Class... expectedInResult) {
		ExtendedSearchIntegrator searchIntegrator = factoryHolder.getSearchFactory();
		IndexedTypeSet set = searchIntegrator.getIndexedTypesPolymorphic( IndexedTypeSets.fromClasses( argument ) );
		IndexedTypeSet expectation = IndexedTypeSets.fromClasses( expectedInResult );
		assertEquals( expectation, set );
	}

	@Indexed
	class RootIndexedA {
		@DocumentId int id;
		@Field int age;
	}

	@Indexed
	class RootIndexedB {
		@DocumentId int someOtherId;
		@Field String name;
	}

	@Indexed
	class ChildOfBIndexed extends RootIndexedB {
	}

	class ChildOfBNotIndexed extends RootIndexedB {
	}

	@Indexed
	class IndexedGrandChildOfB extends ChildOfBNotIndexed {
	}

	class RootNotIndexed {
	}

}
