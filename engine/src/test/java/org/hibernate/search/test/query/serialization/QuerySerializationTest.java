/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.serialization;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.serialization.SerializationTestHelper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;


/**
 * Verifies Serialization of the LuceneHSQuery object.
 * Infinispan needs to serialize these.
 *
 * @author Sanne Grinovero
 */
@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2478 ElasticsearchHSQueryImpl is not serializable
public class QuerySerializationTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void serializeDeserializeLuceneHSQuery() throws ClassNotFoundException, IOException {
		final ExtendedSearchIntegrator integrator = sfHolder.getSearchFactory();

		Book book = new Book();
		book.title = "Java Serialization";
		book.text = "The black art of object serialization is full of pitfalls even for experienced developers";
		helper.add( book );

		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( Book.class ).get();

		Query luceneQuery = queryBuilder.keyword().onField( "text" ).matching( "art" ).createQuery();
		HSQuery hsQuery = integrator.createHSQuery( luceneQuery, Book.class );
		//Lucene Queries are not serializable: who's using LuceneHSQuery will need to
		//encode the query separately and set it again.
		hsQuery.luceneQuery( null );

		HSQuery clonedQuery = SerializationTestHelper.duplicateBySerialization( hsQuery );

		clonedQuery.afterDeserialise( integrator );
		clonedQuery.luceneQuery( luceneQuery );
		List<EntityInfo> result = clonedQuery.queryEntityInfos();
		Assert.assertEquals( 1, result.size() );
	}

	@Indexed
	static class Book {
		@DocumentId
		String title;

		@Field
		String text;
	}

}
