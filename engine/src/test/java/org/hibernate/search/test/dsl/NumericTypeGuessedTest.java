/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * The RangeQuery builder DSL needs to guess the right type of range it needs to produce, by either relying
 * on the known metadata for the target fields, or by guessing it from the types provided as arguments.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1758")
public class NumericTypeGuessedTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( CustomBridgedNumbers.class );

	@Test
	public void verifyExceptionOnNonMeaningfullQueries() {
		final ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();

		storeData( "title-one", "1" );
		storeData( "title-two", "2" );
		storeData( "title-three", "3" );

		QueryBuilder queryBuilder = searchFactory
				.buildQueryBuilder()
				.forEntity( CustomBridgedNumbers.class )
				.get();

		Query query = queryBuilder
					.range()
						.onField( "customField" )
						.from( 1 ).excludeLimit()
						.to( 3 ).excludeLimit()
						.createQuery();

		List<EntityInfo> queryEntityInfos = searchFactory.createHSQuery().luceneQuery( query )
				.targetedEntities( Arrays.asList( new Class<?>[] { CustomBridgedNumbers.class } ) )
				.projection( "title" )
				.queryEntityInfos();

		Assert.assertEquals( 1, queryEntityInfos.size() );
		EntityInfo entityInfo = queryEntityInfos.get( 0 );
		Assert.assertEquals( "title-two", entityInfo.getProjection()[0] );
	}

	private void storeData(String title, String value) {
		CustomBridgedNumbers entry = new CustomBridgedNumbers();
		entry.title = title;
		entry.textEncodedInt = value;

		Work work = new Work( entry, entry.title, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		sfHolder.getSearchFactory().getWorker().performWork( work, tc );
		tc.end();
	}

	@Indexed
	public static class CustomBridgedNumbers {
		@DocumentId
		String title;

		@Field(bridge = @FieldBridge(impl = NumericEncodingCustom.class))
		String textEncodedInt;
	}

	public static class NumericEncodingCustom implements org.hibernate.search.bridge.FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value != null ) {
				Integer i = Integer.parseInt( (String) value );
				luceneOptions.addNumericFieldToDocument( "customField", i, document );
			}

		}

	}


}
