/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sorting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test to verify we apply the right sorting strategy for non-trivial mapped entities
 *
 * @author Sanne Grinovero
 */
public class SortingTest {

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Person.class );

	Person[] testData = new Person[]{
			new Person( 3, "Three" ), // Id=0
			new Person( 10, "Ten" ), // Id=1
			new Person( 9, "Nine" ), // Id=2
			new Person( 5, "Five" ), // Id=3
	};

	@Test
	public void testSortingOnNumericInt() {
		// Index all testData:
		Worker worker = factoryHolder.getSearchFactory().getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		for ( int i = 0; i < testData.length; i++ ) {
			worker.performWork( new Work( testData[i], Long.valueOf( i ), WorkType.INDEX ), tc);
		}
		tc.end();

		// Non sorted, expect results in indexing order:
		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		assertSortedResults( query, null, 0l, 1l, 2l, 3l);

		// Sorting Age as string:
		Sort sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 1l, 0l, 3l, 2l );

		// Sorting Age as Int (numeric):
		Sort sortAsInt = new Sort( new SortField( "ageForIntSorting", SortField.Type.INT ) );
		assertSortedResults( query, sortAsInt, 0l, 3l, 2l, 1l );
	}

	private void assertSortedResults(Query query, Sort sort, Long... expectedIds) {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( Person.class ) );
		if ( sort != null ) {
			hsQuery.sort( sort );
		}
		assertEquals( expectedIds.length, hsQuery.queryResultSize() );
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		assertEquals( expectedIds.length, queryEntityInfos.size() );
		for ( int i = 0; i < expectedIds.length; i++ ) {
			EntityInfo entityInfo = queryEntityInfos.get( i );
			assertNotNull( entityInfo );
			assertEquals( expectedIds[i], entityInfo.getId() );
		}
	}

	@ProvidedId(bridge = @FieldBridge(impl = LongBridge.class))
	@Indexed
	private class Person {

		Person(int age, String name) {
			this.age = age;
			this.name = name;
		}

		@Fields({
			@Field(store = Store.YES, analyze = Analyze.NO, bridge = @FieldBridge(impl = IntegerBridge.class), name = "ageForStringSorting" ),
			@Field(store = Store.YES, analyze = Analyze.NO, name = "ageForIntSorting")
		})
		final int age;

		@Field
		final String name;

	}

}
