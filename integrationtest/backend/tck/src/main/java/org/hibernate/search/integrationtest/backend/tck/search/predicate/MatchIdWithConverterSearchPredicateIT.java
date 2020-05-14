/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.Arrays;

import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the DSL when the id of the entity is stored as a different type.
 * <p>
 * In this test the id of the entity is an integer but it's stored in the index, using a converter,
 * as a string with the prefix `document`. In the DSL the user will still use the integer type when
 * looking for entities matching an id.
 */
public class MatchIdWithConverterSearchPredicateIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";

	private static final ToDocumentIdentifierValueConverter<Integer> ID_CONVERTER = new ToDocumentIdentifierValueConverter<Integer>() {
		@Override
		public String convert(Integer value, ToDocumentIdentifierValueConvertContext context) {
			return "document" + value;
		}

		@Override
		public String convertUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
			return convert( (Integer) value, context );
		}
	};

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final StubMappedIndex index = StubMappedIndex.ofAdvancedNonRetrievable( ctx -> ctx.idDslConverter( ID_CONVERTER ) );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void match_id() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id().matching( 1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void match_multiple_ids() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matching( 1 )
						.matching( 3 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void match_any_and_match_single_id() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matching( 2 )
						.matchingAny( Arrays.asList( 1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void match_any_single_id() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( 1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void match_any_ids() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.id()
						.matchingAny( Arrays.asList( 1, 3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = index.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> { } );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> { } );
		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = index.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}
}
