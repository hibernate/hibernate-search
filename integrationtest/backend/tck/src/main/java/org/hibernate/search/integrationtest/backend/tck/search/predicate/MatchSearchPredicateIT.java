/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private StubMappingIndexManager compatibleIndexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						"CompatibleMappedType", COMPATIBLE_INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void match() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.predicateParameterValue;

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void match_withDslConverter() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = new ValueWrapper<>( fieldModel.predicateParameterValue );

			SearchQuery<DocumentReference> query = searchTarget.query()
					.asReference()
					.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ) )
					.build();

			assertThat( query )
					.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void match_emptyStringBeforeAnalysis() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		MainFieldModel fieldModel = indexMapping.analyzedStringField;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( fieldModel.relativeFieldName ).matching( "" ) )
				.build();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void match_noTokenAfterAnalysis() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		MainFieldModel fieldModel = indexMapping.analyzedStringField;

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				// Use a stopword, which should be removed by the analysis
				.predicate( f -> f.match().onField( fieldModel.relativeFieldName ).matching( "a" ) )
				.build();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void unsupported_field_types() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.document1Value.indexedValue;

			SubTest.expectException(
					"match() predicate with unsupported type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().match().onField( absoluteFieldPath ).matching( valueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Match predicates are not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void match_error_null() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectException(
					"matching() predicate with null value to match on field " + fieldModel.relativeFieldName,
					() -> searchTarget.predicate().match().onField( fieldModel.relativeFieldName ).matching( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "value to match" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldModel.relativeFieldName );
		}
	}

	@Test
	public void boost() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( root -> root.bool()
						.should( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
								.matching( indexMapping.string1Field.document1Value.indexedValue )
						)
						.should( f -> f.match().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.matching( indexMapping.string1Field.document3Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( root -> root.bool()
						.should( f -> f.match().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.matching( indexMapping.string1Field.document1Value.indexedValue )
						)
						.should( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
								.matching( indexMapping.string1Field.document3Value.indexedValue )
						)
				)
				.sort( c -> c.byScore() )
				.build();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void multi_fields() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.matching( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.matching( indexMapping.string2Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// onField().orFields(...)

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string2Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string3Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// onFields(...)

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query()
				.asReference()
				.predicate( f -> f.match().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string2Field.relativeFieldName )
						.matching( indexMapping.string2Field.document1Value.indexedValue )
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void unknown_field() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onFields( indexMapping.string1Field.relativeFieldName, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onField( indexMapping.string1Field.relativeFieldName ).orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"match() predicate with unknown field",
				() -> searchTarget.predicate().match().onField( indexMapping.string1Field.relativeFieldName ).orFields( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void error_invalidType() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		List<ByTypeFieldModel<?>> fieldModels = new ArrayList<>();
		fieldModels.addAll( indexMapping.supportedFieldModels );
		fieldModels.addAll( indexMapping.supportedFieldWithDslConverterModels );

		for ( ByTypeFieldModel<?> fieldModel : fieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object invalidValueToMatch = new InvalidType();

			SubTest.expectException(
					"match() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().match().onField( absoluteFieldPath ).matching( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void multiIndex() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget( compatibleIndexManager );

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;
				Object valueToMatch = model.predicateParameterValue;

				SearchQuery<DocumentReference> query = searchTarget.query()
						.asReference()
						.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ) )
						.build();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( INDEX_NAME, DOCUMENT_1 );
					b.doc( COMPATIBLE_INDEX_NAME, COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.string1Field.document1Value.write( document );
			indexMapping.string2Field.document1Value.write( document );
			indexMapping.string3Field.document1Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.string1Field.document2Value.write( document );
			indexMapping.string2Field.document2Value.write( document );
			indexMapping.string3Field.document2Value.write( document );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.string1Field.document3Value.write( document );
			indexMapping.string2Field.document3Value.write( document );
			indexMapping.string3Field.document3Value.write( document );
		} );
		workPlan.execute().join();

		workPlan = compatibleIndexManager.createWorkPlan();
		workPlan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
		} );
		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY,
				DOCUMENT_3
		);
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		final MainFieldModel string1Field;
		final MainFieldModel string2Field;
		final MainFieldModel string3Field;
		final MainFieldModel analyzedStringField;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapByTypeFields(
					root, "supported_", ignored -> { },
					MatchPredicateExpectations::isMatchPredicateSupported
			);
			supportedFieldWithDslConverterModels = mapByTypeFields(
					root, "supported_converted_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() ),
					MatchPredicateExpectations::isMatchPredicateSupported
			);
			unsupportedFieldModels = mapByTypeFields(
					root, "supported_converted_", ignored -> { },
					e -> !e.isMatchPredicateSupported()
			);
			string1Field = MainFieldModel.mapper(
					"Irving", "Auster", "Coe"
			)
					.map( root, "string1" );
			string2Field = MainFieldModel.mapper(
					"Avenue of mysteries", "Oracle Night", "4 3 2 1"
			)
					.map( root, "string2" );
			string3Field = MainFieldModel.mapper(
					"Avenue of mysteries", "Oracle Night", "4 3 2 1"
			)
					.map( root, "string3" );
			analyzedStringField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ),
					"a word", "another word", "a"
			)
					.map( root, "analyzedString" );
		}
	}

	private static List<ByTypeFieldModel<?>> mapByTypeFields(IndexSchemaElement root, String prefix,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration,
			Predicate<MatchPredicateExpectations<?>> predicate) {
		return FieldTypeDescriptor.getAll().stream()
				.filter(
						typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent()
						&& predicate.test( typeDescriptor.getMatchPredicateExpectations().get() )
				)
				.map( typeDescriptor -> mapByTypeField( root, prefix, typeDescriptor, additionalConfiguration ) )
				.collect( Collectors.toList() );
	}

	private static <F> ByTypeFieldModel<F> mapByTypeField(IndexSchemaElement parent, String prefix,
			FieldTypeDescriptor<F> typeDescriptor,
			Consumer<StandardIndexFieldTypeContext<?, ?>> additionalConfiguration) {
		MatchPredicateExpectations<F> expectations = typeDescriptor.getMatchPredicateExpectations().get(); // Safe, see caller
		return StandardFieldMapper.of(
				typeDescriptor::configure,
				additionalConfiguration,
				(accessor, name) -> new ByTypeFieldModel<>( accessor, name, expectations )
		)
				.map( parent, prefix + typeDescriptor.getUniqueName() );
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

	private static class MainFieldModel {
		static StandardFieldMapper<String, MainFieldModel> mapper(
				String document1Value, String document2Value, String document3Value) {
			return mapper( c -> c.asString(), document1Value, document2Value, document3Value );
		}

		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, String>> configuration,
				String document1Value, String document2Value, String document3Value) {
			return StandardFieldMapper.of(
					configuration,
					(accessor, name) -> new MainFieldModel( accessor, name, document1Value, document2Value, document3Value )
			);
		}

		final String relativeFieldName;
		final ValueModel<String> document1Value;
		final ValueModel<String> document2Value;
		final ValueModel<String> document3Value;

		private MainFieldModel(IndexFieldAccessor<String> accessor, String relativeFieldName,
				String document1Value, String document2Value, String document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
		}
	}

	private static class ByTypeFieldModel<F> {
		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;

		final F predicateParameterValue;

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName,
				MatchPredicateExpectations<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( accessor, expectations.getDocument2Value() );
			this.predicateParameterValue = expectations.getMatchingDocument1Value();
		}
	}
}
