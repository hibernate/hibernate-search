/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SearchQueryTimeoutIT {

	private static final String BACKEND_NAME = "backendName";
	private static final String INDEX_NAME = "indexName";
	private static final int FIELD_COUNT = 20;
	private static final List<String> FIELD_NAMES = Collections.unmodifiableList(
			IntStream.range( 0 , FIELD_COUNT )
					.mapToObj( i -> "field_" + i )
					.collect( Collectors.toList() )
	);
	private static final String EMPTY_FIELD_NAME = "emptyFieldName";

	// Taken from our current documentation (https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/):
	private static final String TEXT_1 = "Fine-grained dirty checking consists in keeping track of which properties are dirty in a given entity, so as to only reindex" +
			"\"containing\" entities that actually use at least one of the dirty properties.";
	private static final String TEXT_2 = "Whenever we create a type node in the reindexing resolver building tree, we take care to determine all the possible concrete " +
			"entity types for the considered type, and create one reindexing resolver type node builder per possible entity type.";
	private static final String TEXT_3 = "The only thing left to do is register the path that is depended on (in our example, longField). With this path registered, " +
			"we will be able to build a PojoPathFilter, so that whenever SecondLevelEmbeddedEntityClass changes, we will walk through the tree, but not all the tree: " +
			"if at some point we notice that a node is relevant only if longField changed, but the \"dirtiness state\" tells us that longField did not change, " +
			"we can skip a whole branch of the tree, avoiding useless lazy loading and reindexing.";

	private static final String BUZZ_WORDS = "tree search avoid nested reference thread concurrency scaling reindexing node track";
	private static final int ANY_INTEGER = 739;

	private static final int ROUNDS = 1000;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private StubMappingIndexManager indexManager;
	private IndexMapping indexMapping;

	@Before
	public void setup() {
		setupHelper.start( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void timeout_slowQuery_smallTimeout_raiseAnException() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		SubTest.expectException( () -> query.fetchAll() )
				.assertThrown()
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( "Search query exceeded the timeout of 0 seconds and 1 nanoseconds: " );
	}

	@Test
	public void timeout_slowCount_smallTimeout_raiseAnException() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		SubTest.expectException( () -> query.fetchTotalHitCount() )
				.assertThrown()
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( "Search query exceeded the timeout of 0 seconds and 1 nanoseconds: " );
	}

	@Test
	public void timeout_slowQuery_smallTimeout_limitFetching() {
		Assume.assumeTrue(
				"backend should have a fast timeout resolution in order to run this test correctly",
				TckConfiguration.get().getBackendFeatures().fastTimeoutResolution()
		);

		SearchResult<DocumentReference> result = startSlowQuery()
				.truncateAfter( 1, TimeUnit.NANOSECONDS )
				.fetchAll();

		assertThat( result.getTotalHitCount() ).isLessThan( ROUNDS * 3 );
		assertThat( result.getTook() ).isNotNull(); // May be 0 due to low resolution
		assertThat( result.isTimedOut() ).isTrue();
	}

	@Test
	public void timeout_fastQuery_largeTimeout() {
		SearchResult<DocumentReference> result = startFastQuery()
				.failAfter( 1, TimeUnit.DAYS )
				.fetchAll();

		SearchResultAssert.assertThat( result ).hasNoHits();

		assertThat( result.getTook() ).isLessThan( Duration.ofDays( 1L ) );
		assertThat( result.isTimedOut() ).isFalse();
	}

	@Test
	public void timeout_fastCount_largeTimeout() {
		SearchQuery<DocumentReference> query = startFastQuery()
				.failAfter( 1, TimeUnit.DAYS )
				.toQuery();

		assertThat( query.fetchTotalHitCount() ).isEqualTo( 0 );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> startSlowQuery() {
		return indexManager.createScope().query()
				.predicate( f -> f.bool( b -> {
					for ( String fieldName : FIELD_NAMES ) {
						b.must( f.match().field( fieldName ).matching( BUZZ_WORDS ) );
					}
				} ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> startFastQuery() {
		return indexManager.createScope().query()
				.predicate( f -> f.match().field( EMPTY_FIELD_NAME ).matching( ANY_INTEGER ) );
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();
		// Use a batch approach for a real application
		// Here the huge bulk is used to provoke a timeout
		for ( int i = 0; i < ROUNDS; i++ ) {
			plan.add(
					referenceProvider( i + "a" ),
					document -> {
						for ( IndexFieldReference<String> field : indexMapping.fields ) {
							document.addValue( field, TEXT_1 );
						}
					}
			);
			plan.add(
					referenceProvider( i + "b" ),
					document -> {
						for ( IndexFieldReference<String> field : indexMapping.fields ) {
							document.addValue( field, TEXT_2 );
						}
					}
			);
			plan.add(
					referenceProvider( i + "c" ),
					document -> {
						for ( IndexFieldReference<String> field : indexMapping.fields ) {
							document.addValue( field, TEXT_3 );
						}
					}
			);
		}
		plan.execute().join();
	}

	private static class IndexMapping {
		private final List<IndexFieldReference<String>> fields = new ArrayList<>();

		IndexMapping(IndexSchemaElement root) {
			for ( String fieldName : FIELD_NAMES ) {
				fields.add(
						root.field(
								fieldName,
								f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
						)
								.toReference()
				);
			}
			root.field( EMPTY_FIELD_NAME, f -> f.asInteger() ).toReference();
		}
	}
}
