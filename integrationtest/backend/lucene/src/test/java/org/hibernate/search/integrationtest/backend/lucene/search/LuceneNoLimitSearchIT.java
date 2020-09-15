/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-3947")
public class LuceneNoLimitSearchIT {

	public static final int INDEX_SIZE = 100_000;

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();
		initData();
	}

	@Test
	public void fetchAll() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "field" ).matching( "739" ) )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThat( documentReferences )
				.hasDocRefHitsAnyOrder( index.typeName(), "739" )
				.hasTotalHitCount( 1L );
	}

	@Test
	public void fetchAll_totalHitCountThreshold() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "field" ).matching( "739" ) )
				.totalHitCountThreshold( 5 )
				.toQuery();

		SearchResult<DocumentReference> documentReferences = query.fetchAll();

		assertThat( documentReferences )
				.hasDocRefHitsAnyOrder( index.typeName(), "739" )
				.hasTotalHitCount( 1L );
	}

	@Test
	public void fetchAllHits() {
		SearchQuery<DocumentReference> query = index.createScope().query()
				.where( f -> f.match().field( "field" ).matching( "739" ) )
				.toQuery();

		List<DocumentReference> documentReferences = query.fetchAllHits();

		assertThat( documentReferences ).hasDocRefHitsAnyOrder( index.typeName(), "739" );
	}

	private static void initData() {
		index.bulkIndexer()
				.add( INDEX_SIZE, i -> documentProvider(
						String.valueOf( i ),
						document -> document.addValue( index.binding().field, String.valueOf( i ) )
				) )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "field", c -> c.asString() ).toReference();
		}
	}
}
