/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchIdSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String EMPTY = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void match_id() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.id().matching( DOCUMENT_1 ).toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_multiple_ids() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.id()
						.matching( DOCUMENT_1 )
						.matching( DOCUMENT_3 )
						.toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void match_any_and_match_single_id() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.id()
						.matching( DOCUMENT_2 )
						.matchingAny( Arrays.asList( DOCUMENT_1 ) )
						.toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void match_any_single_id() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1 ) )
						.toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void match_any_ids() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.id()
						.matchingAny( Arrays.asList( DOCUMENT_1, DOCUMENT_3 ) )
						.toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document2Value.write( document ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> { } );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );
		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY,
				DOCUMENT_3
		);
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapSupportedFields( root, "supported_", ignored -> { } );
			supportedFieldWithDslConverterModels = mapSupportedFields(
					root, "supported_converted_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() )
			);
			unsupportedFieldModels = Arrays.asList(
					ByTypeFieldModel.mapper(
							GeoPoint.class,
							GeoPoint.of( 40, 70 ),
							GeoPoint.of( 45, 98 )
					)
							.map( root, "geoPoint" )
			);
		}

		private List<ByTypeFieldModel<?>> mapSupportedFields(IndexSchemaElement root, String prefix,
				Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
			return Arrays.asList(
					ByTypeFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ),
							"irving and company", "Auster", "Irving"
					)
							.map(
									root, prefix + "analyzedString", additionalConfiguration
							),
					ByTypeFieldModel.mapper( String.class, "Irving", "Auster" )
							.map( root, prefix + "nonAnalyzedString", additionalConfiguration ),
					ByTypeFieldModel.mapper( Integer.class, 42, 67 )
							.map( root, prefix + "integer", additionalConfiguration ),
					ByTypeFieldModel.mapper(
							LocalDate.class,
							LocalDate.of( 1980, 10, 11 ),
							LocalDate.of( 1984, 10, 7 )
					)
							.map( root, prefix + "localDate", additionalConfiguration )
			);
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldAccessor<F> accessor;
		final F indexedValue;

		private ValueModel(IndexFieldAccessor<F> accessor, F indexedValue) {
			this.accessor = accessor;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			accessor.write( target, indexedValue );
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value) {
			return mapper(
					c -> (StandardIndexSchemaFieldTypedContext<?, F>) c.as( type ),
					document1Value, document2Value, document1Value
			);
		}

		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(
				Function<IndexSchemaFieldContext, StandardIndexSchemaFieldTypedContext<?, F>> configuration,
				F document1Value, F document2Value, F predicateParameterValue) {
			return (parent, name, additionalConfiguration) -> {
				IndexSchemaFieldContext untypedContext = parent.field( name );
				StandardIndexSchemaFieldTypedContext<?, F> context = configuration.apply( untypedContext );
				additionalConfiguration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new ByTypeFieldModel<>(
						accessor, document1Value, document2Value
				);
			};
		}

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor,
				F document1Value, F document2Value) {
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
		}
	}
}
