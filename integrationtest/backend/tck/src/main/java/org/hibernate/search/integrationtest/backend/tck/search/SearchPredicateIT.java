/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.SearchException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SearchPredicateIT {

	// TODO tests related to search predicates
	// - test each type of predicate
	// - test each syntax (fluid, lambda, SearchPredicate objects)
	// - ... ?

	private static final String MATCHING_ID = "matching";
	private static final String NON_MATCHING_ID = "nonMatching";
	private static final String EMPTY_ID = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexAccessors indexAccessors;
	private IndexManager<?> indexManager;
	private String indexName;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName",
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						(indexManager, indexName) -> {
							this.indexManager = indexManager;
							this.indexName = indexName;
						}
				)
				.setup();

		initData();
	}

	@Test
	public void match_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( String fieldPath : Arrays.asList( "string", "string_analyzed", "integer", "localDate" ) ) {
			try {
				searchTarget.predicate().match().onField( fieldPath ).matching( null );
				fail( "Expected matching() predicate with null value to match to throw exception on field " + fieldPath );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( "Invalid value" )
						.contains( "value to match" )
						.contains( "must be non-null" )
						.contains( fieldPath );
			}
		}
	}

	@Test
	public void range_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		List<String> fieldPaths = Arrays.asList(
				"string", "string_analyzed", "integer", "localDate"
		);

		for ( String fieldPath : fieldPaths ) {
			try {
				searchTarget.predicate().range().onField( fieldPath ).from( null ).to( null );
				fail( "Expected range() predicate with null bounds to throw exception on field " + fieldPath );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( "Invalid value" )
						.contains( "at least one bound" )
						.contains( "must be non-null" )
						.contains( fieldPath );
			}
			try {
				searchTarget.predicate().range().onField( fieldPath ).above( null );
				fail( "Expected range() predicate with null bounds to throw exception on field " + fieldPath );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( "Invalid value" )
						.contains( "at least one bound" )
						.contains( "must be non-null" )
						.contains( fieldPath );
			}
			try {
				searchTarget.predicate().range().onField( fieldPath ).below( null );
				fail( "Expected range() predicate with null bounds to throw exception on field " + fieldPath );
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.contains( "Invalid value" )
						.contains( "at least one bound" )
						.contains( "must be non-null" )
						.contains( fieldPath );
			}
		}
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( sessionContext );
		worker.add( referenceProvider( MATCHING_ID ), document -> {
			// TODO add matching values
		} );
		worker.add( referenceProvider( NON_MATCHING_ID ), document -> {
			// TODO add non-matching values
		} );
		worker.add( referenceProvider( EMPTY_ID ), document -> { } );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().all().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( indexName, MATCHING_ID, NON_MATCHING_ID, EMPTY_ID );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> string_analyzed;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<LocalDate> localDate;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
			string_analyzed = root.field( "string_analyzed" ).asString()
					.analyzer( "default" ).createAccessor();
			integer = root.field( "integer" ).asInteger().createAccessor();
			localDate = root.field( "localDate" ).asLocalDate().createAccessor();
		}
	}
}
