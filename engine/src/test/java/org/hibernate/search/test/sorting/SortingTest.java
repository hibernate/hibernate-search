/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sorting;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test to verify we apply the right sorting strategy for non-trivial mapped entities
 *
 * @author Sanne Grinovero
 */
public class SortingTest {

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Person.class );

	@Test
	public void testSortingOnNumericInt() {
		// Index all testData:
		storeTestingData(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
			);

		// Non sorted, expect results in indexing order:
		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		assertSortedResults( query, null, 0, 1, 2, 3);

		// Sorting Age as string:
		Sort sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 1, 0, 3, 2 );

		// Sorting Age as Int (numeric):
		Sort sortAsInt = new Sort( new SortField( "ageForIntSorting", SortField.Type.INT ) );
		assertSortedResults( query, sortAsInt, 0, 3, 2, 1 );
	}

	@Test
	public void testSortingOnString() {
		// Index all testData:
		storeTestingData(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
			);

		// Sorting Name
		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		Sort sortAsString = new Sort( new SortField( "name", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 3, 2, 1, 0 );
	}

	private void storeTestingData(Person... testData) {
		Worker worker = factoryHolder.getSearchFactory().getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		for ( int i = 0; i < testData.length; i++ ) {
			Person p = testData[i];
			worker.performWork( new Work( p, p.id, WorkType.INDEX ), tc);
		}
		tc.end();
	}

	private void assertSortedResults(Query query, Sort sort, int... expectedIds) {
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

	@Indexed
	private class Person {

		@DocumentId
		final int id;

		@SortFields({
				@org.hibernate.search.annotations.SortField(forField = "ageForStringSorting"),
				@org.hibernate.search.annotations.SortField(forField = "ageForIntSorting")
		})
		@Fields({
			@Field(name = "ageForStringSorting", store = Store.YES, analyze = Analyze.NO, bridge = @FieldBridge(impl = IntegerBridge.class) ),
			@Field(name = "ageForIntSorting", store = Store.YES, analyze = Analyze.NO),
			@Field(name = "ageForNullChecks", store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
		})
		final Integer age;

		@org.hibernate.search.annotations.SortField
		@Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
		final String name;

		Person(int id, Integer age, String name) {
			this.id = id;
			this.age = age;
			this.name = name;
		}

	}

}
