/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
@Category(SkipOnElasticsearch.class) // This test is Lucene-specific. The generic equivalent is ProgrammaticMappingTest.
public class LuceneProgrammaticMappingTest extends SearchTestBase {

	@Test
	public void testNumeric() throws Exception {
		Item item = new Item();
		item.setId( 1 );
		item.setPrice( (short) 3454 );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( item );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();

		Query q = s.getSearchFactory().buildQueryBuilder().forEntity( Item.class ).get().all().createQuery();
		FullTextQuery query = s.createFullTextQuery( q, Item.class );

		@SuppressWarnings("unchecked")
		List<Object[]> result = query.setProjection( ProjectionConstants.DOCUMENT, ProjectionConstants.THIS )
				.list();

		assertEquals( "Numeric field via programmatic config", 1, query.getResultSize() );

		Object[] row = result.iterator().next();
		Document document = (Document) row[0];

		IndexableField priceNumeric = document.getField( "price" );
		assertThat( priceNumeric.numericValue() ).isEqualTo( 3454 );

		IndexableField priceString = document.getField( "price_string" );
		assertThat( priceString.numericValue() ).isNull();
		assertThat( priceString.stringValue() ).isEqualTo( "3454" );

		s.delete( row[1] );

		tx.commit();
		s.close();
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.MODEL_MAPPING, ProgrammaticSearchMappingFactory.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ProductCatalog.class,
				Item.class,
				BlogEntry.class
		};
	}
}
