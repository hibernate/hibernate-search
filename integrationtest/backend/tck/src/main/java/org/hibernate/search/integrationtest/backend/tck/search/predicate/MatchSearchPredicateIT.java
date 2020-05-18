/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class MatchSearchPredicateIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String EMPTY = "empty";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";
	private static final String INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 = "incompatible_analyzer_1";
	private static final String COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1 = "compatible_search_analyzer_1";
	private static final String INCOMPATIBLE_DECIMAL_SCALE_INDEX_DOCUMENT_1 = "incompatible_decimal_scale_1";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "rawFieldCompatible" );
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );
	private final SimpleMappedIndex<IncompatibleAnalyzerIndexBinding> incompatibleAnalyzerIndex =
			SimpleMappedIndex.of( IncompatibleAnalyzerIndexBinding::new ).name( "incompatibleAnalyzer" );
	private final SimpleMappedIndex<CompatibleSearchAnalyzerIndexBinding> compatibleSearchAnalyzerIndex =
			SimpleMappedIndex.of( CompatibleSearchAnalyzerIndexBinding::new ).name( "compatibleSearchAnalyzer" );
	private final SimpleMappedIndex<IncompatibleDecimalScaleIndexBinding> incompatibleDecimalScaleIndex =
			SimpleMappedIndex.of( IncompatibleDecimalScaleIndexBinding::new ).name( "incompatibleDecimalScale" );
	private final SimpleMappedIndex<UnsearchableFieldsIndexBinding> unsearchableFieldsIndex =
			SimpleMappedIndex.of( UnsearchableFieldsIndexBinding::new ).name( "unsearchableFields" );

	@Before
	public void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex, compatibleIndex, rawFieldCompatibleIndex, compatibleSearchAnalyzerIndex,
						incompatibleIndex, incompatibleAnalyzerIndex, incompatibleDecimalScaleIndex,
						unsearchableFieldsIndex
				)
				.setup();

		initData();
	}

	@Test
	public void match() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.predicateParameterValue;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.match().field( absoluteFieldPath ).matching( valueToMatch ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		}
	}

	@Test
	public void match_unsearchable() {
		StubMappingScope scope = unsearchableFieldsIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy( () ->
					scope.predicate().match().field( absoluteFieldPath )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "is not searchable" )
					.hasMessageContaining( "Make sure the field is marked as searchable" )
					.hasMessageContaining( absoluteFieldPath );
		}
	}

	@Test
	public void withDslConverter_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = new ValueWrapper<>( fieldModel.predicateParameterValue );

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.match().field( absoluteFieldPath ).matching( valueToMatch ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		}
	}

	@Test
	public void withDslConverter_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SearchQuery<DocumentReference> query = scope.query()
					.where( f -> f.match().field( absoluteFieldPath ).matching( fieldModel.predicateParameterValue, ValueConvert.NO ) )
					.toQuery();

			assertThat( query )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		}
	}

	@Test
	public void emptyStringBeforeAnalysis() {
		StubMappingScope scope = mainIndex.createScope();

		MainFieldModel fieldModel = mainIndex.binding().analyzedStringField;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( fieldModel.relativeFieldName ).matching( "" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void noTokenAfterAnalysis() {
		StubMappingScope scope = mainIndex.createScope();

		MainFieldModel fieldModel = mainIndex.binding().analyzedStringField;

		SearchQuery<DocumentReference> query = scope.query()
				// Use a stopword, which should be removed by the analysis
				.where( f -> f.match().field( fieldModel.relativeFieldName ).matching( "a" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void error_unsupportedFieldTypes() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.document1Value.indexedValue;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match().field( absoluteFieldPath ).matching( valueToMatch ),
					"match() predicate with unsupported type on field " + absoluteFieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "match predicates" )
					.hasMessageContaining( "are not supported by this field's type" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void error_null() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			Assertions.assertThatThrownBy(
					() -> scope.predicate().match().field( fieldModel.relativeFieldName ).matching( null ),
					"matching() predicate with null value to match on field " + fieldModel.relativeFieldName
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "value to match" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldModel.relativeFieldName );
		}
	}

	@Test
	public void perFieldBoostWithConstantScore_error() {
		StubMappingScope scope = mainIndex.createScope();

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 2.1f )
						.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
						.constantScore()
						.toPredicate()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "per-field boosts together with withConstantScore option" );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 42 )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 42 )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.boost( 39 )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void predicateLevelBoost_andFieldLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						// 4 * 2 => boost x8
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 4 )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.boost( 2 )
						)
						// 3 * 3 => boost x9
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 3 )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.boost( 3 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						// 1 * 3 => boost x3
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 3 )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
						)
						// 0.1 * 3 => boost x0.3
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName ).boost( 3 )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.boost( 0.1f )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void constantScore() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						// 0.287682
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
						)
						// withConstantScore 0.287682 => 1
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.constantScore()
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						// withConstantScore 0.287682 => 1
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.constantScore()
						)
						// 0.287682
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.constantScore().boost( 7 )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.constantScore().boost( 39 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.constantScore().boost( 39 )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.constantScore().boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void predicateLevelBoost_multiFields() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.field( mainIndex.binding().string2Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.boost( 7 )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.field( mainIndex.binding().string2Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.boost( 39 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.field( mainIndex.binding().string2Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
								.boost( 39 )
						)
						.should( f.match().field( mainIndex.binding().string1Field.relativeFieldName )
								.field( mainIndex.binding().string2Field.relativeFieldName )
								.matching( mainIndex.binding().string1Field.document3Value.indexedValue )
								.boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQuery")
	public void fuzzy() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery =
				text -> scope.query()
						.where( f -> f.match()
								.field( absoluteFieldPath )
								.matching( text )
								.fuzzy() )
						.toQuery();

		// max edit distance = default (2), ignored prefix length = default (0)
		assertThat( createQuery.apply( "another word" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther ord" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "ather wd" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "ater w" ) )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQuery")
	public void fuzzy_maxEditDistance() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;
		BiFunction<String, Integer, SearchQuery<DocumentReference>> createQuery =
				(text, maxEditDistance) -> scope.query()
						.where( f -> f.match()
								.field( absoluteFieldPath )
								.matching( text )
								.fuzzy( maxEditDistance ) )
						.toQuery();

		// max edit distance = 2
		assertThat( createQuery.apply( "another word", 2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther ord", 2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "ather wd", 2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "ater w", 2 ) )
				.hasNoHits();

		// max edit distance = 1
		assertThat( createQuery.apply( "another word", 1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther ord", 1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "ather wd", 1 ) )
				.hasNoHits();
		assertThat( createQuery.apply( "ater w", 1 ) )
				.hasNoHits();

		// max edit distance = 0
		assertThat( createQuery.apply( "another word", 0 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther ord", 0 ) )
				.hasNoHits();
		assertThat( createQuery.apply( "ather wd", 0 ) )
				.hasNoHits();
		assertThat( createQuery.apply( "ater w", 0 ) )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQuery")
	public void fuzzy_exactPrefixLength() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;
		BiFunction<String, Integer, SearchQuery<DocumentReference>> createQuery =
				(text, exactPrefixLength) -> scope.query()
						.where( f -> f.match()
								.field( absoluteFieldPath )
								.matching( text )
								.fuzzy( 1, exactPrefixLength ) )
						.toQuery();

		// exact prefix length = 0
		assertThat( createQuery.apply( "another word", 0 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther wod", 0 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "aother wrd", 0 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "nother ord", 0 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );

		// exact prefix length = 1
		assertThat( createQuery.apply( "another word", 1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther wod", 1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "aother wrd", 1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "nother ord", 1 ) )
				.hasNoHits();

		// exact prefix length = 2
		assertThat( createQuery.apply( "another word", 2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "anther wod", 2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "aother wrd", 2 ) )
				.hasNoHits();
		assertThat( createQuery.apply( "nother ord", 2 ) )
				.hasNoHits();
	}

	@Test
	public void fuzzy_normalizedStringField() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().normalizedStringField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		createQuery = param -> scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( param ).fuzzy() )
				.toQuery();
		assertThat( createQuery.apply( "Irving" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "Irvin" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "rvin" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "rin" ) )
				.hasNoHits();

		createQuery = param -> scope.query()
				.where( f -> f.match().field( absoluteFieldPath )
						.matching( param ).fuzzy( 2, 1 ) )
				.toQuery();
		assertThat( createQuery.apply( "Irving" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "irving" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "Irvin" ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "rving" ) )
				.hasNoHits();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testFuzzyQueryOnMultipleFields")
	public void fuzzy_multipleFields() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath1 = mainIndex.binding().analyzedStringField.relativeFieldName;
		String absoluteFieldPath2 = mainIndex.binding().analyzedStringField2.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		createQuery = param -> scope.query()
				.where( f -> f.match().fields( absoluteFieldPath1, absoluteFieldPath2 ).matching( param ).fuzzy() )
				.toQuery();
		assertThat( createQuery.apply( "word" ) ) // distance 1 from doc1:field2, 0 from doc2:field1
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2 );
		assertThat( createQuery.apply( "wd" ) ) // distance 3 from doc1:field2, 2 from doc2:field1
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
		assertThat( createQuery.apply( "worldss" ) ) // distance 2 from doc1:field2, 3 from doc2:field1
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( "wl" ) ) // distance 3 from doc1:field2, 3 from doc2:field1
				.hasNoHits();
	}

	@Test
	public void error_fuzzy_unsupportedFieldType() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().fuzzyUnsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.document1Value.indexedValue;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match()
							.field( absoluteFieldPath ).matching( valueToMatch ).fuzzy(),
					"match() predicate with fuzzy() and unsupported type on field " + absoluteFieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Text predicates" )
					.hasMessageContaining( "not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match()
							.field( absoluteFieldPath ).matching( valueToMatch ).fuzzy( 1 ),
					"match() predicate with fuzzy(int) and unsupported type on field " + absoluteFieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Text predicates" )
					.hasMessageContaining( "not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match()
							.field( absoluteFieldPath ).matching( valueToMatch ).fuzzy( 1, 1 ),
					"match() predicate with fuzzy(int, int) and unsupported type on field " + absoluteFieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Text predicates" )
					.hasMessageContaining( "not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void error_fuzzy_invalidMaxEditDistance() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( absoluteFieldPath )
						.matching( "foo" ).fuzzy( 3 )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid maximum edit distance" )
				.hasMessageContaining( "0, 1 or 2" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( absoluteFieldPath )
						.matching( "foo" ).fuzzy( -1 )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid maximum edit distance" )
				.hasMessageContaining( "0, 1 or 2" );
	}

	@Test
	public void error_fuzzy_invalidPrefixLength() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( absoluteFieldPath )
						.matching( "foo" ).fuzzy( 1, -1 )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid exact prefix length" )
				.hasMessageContaining( "positive or zero" );
	}

	@Test
	public void analyzerOverride() {
		StubMappingScope scope = mainIndex.createScope();

		String whitespaceAnalyzedField = mainIndex.binding().whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String whitespaceLowercaseSearchAnalyzedField = mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.relativeFieldName;

		// Terms are never lower-cased, neither at write nor at query time.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "NEW WORLD" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );

		// Terms are always lower-cased, both at write and at query time.
		query = scope.query()
				.where( f -> f.match().field( whitespaceLowercaseAnalyzedField ).matching( "NEW WORLD" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Terms are lower-cased only at query time. Because we are overriding the analyzer in the predicate.
		query = scope.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "NEW WORLD" )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// Same here. Terms are lower-cased only at query time. Because we've defined a search analyzer.
		query = scope.query()
				.where( f -> f.match().field( whitespaceLowercaseSearchAnalyzedField ).matching( "NEW WORLD" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// As for the first query, terms are never lower-cased, neither at write nor at query time.
		// Because even if we've defined a search analyzer, we are overriding it with an analyzer in the predicate,
		// since the overriding takes precedence over the search analyzer.
		query = scope.query()
				.where( f -> f.match().field( whitespaceLowercaseSearchAnalyzedField ).matching( "NEW WORLD" )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );
	}

	@Test
	public void analyzerOverride_fuzzy() {
		StubMappingScope scope = mainIndex.createScope();

		String whitespaceAnalyzedField = mainIndex.binding().whitespaceAnalyzedField.relativeFieldName;
		String whitespaceLowercaseAnalyzedField = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String whitespaceLowercaseSearchAnalyzedField = mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.relativeFieldName;

		// Terms are never lower-cased, neither at write nor at query time.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "WORD" ).fuzzy() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_3 );

		// Terms are always lower-cased, both at write and at query time.
		query = scope.query()
				.where( f -> f.match().field( whitespaceLowercaseAnalyzedField ).matching( "WORD" ).fuzzy() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Terms are lower-cased only at query time. Because we are overriding the analyzer in the predicate.
		query = scope.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "WORD" ).fuzzy()
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// Same here. Terms are lower-cased only at query time. Because we've defined a search analyzer.
		query = scope.query()
				.where( f -> f.match().field( whitespaceLowercaseSearchAnalyzedField ).matching( "WORD" ).fuzzy() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// As for the first query, terms are never lower-cased, neither at write nor at query time.
		// Because even if we've defined a search analyzer, we are overriding it with an analyzer in the predicate,
		// since the overriding takes precedence over the search analyzer.
		query = scope.query()
				.where( f -> f.match().field( whitespaceLowercaseSearchAnalyzedField ).matching( "WORD" ).fuzzy()
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void analyzerOverride_withNormalizer() {
		StubMappingScope scope = mainIndex.createScope();
		String whitespaceAnalyzedField = mainIndex.binding().whitespaceAnalyzedField.relativeFieldName;

		Assertions.assertThatThrownBy( () -> scope.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "WORLD" )
						// we have a normalizer with that name, but not an analyzer
						.analyzer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) )
				.toQuery().fetchAll()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "DefaultAnalysisDefinitions_lowercase" );
	}

	@Test
	public void analyzerOverride_notExistingName() {
		StubMappingScope scope = mainIndex.createScope();
		String whitespaceAnalyzedField = mainIndex.binding().whitespaceAnalyzedField.relativeFieldName;

		Assertions.assertThatThrownBy( () -> scope.query()
				.where( f -> f.match().field( whitespaceAnalyzedField ).matching( "WORLD" )
						// we don't have any analyzer with that name
						.analyzer( "this_name_does_actually_not_exist" ) )
				.toQuery().fetchAll()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "this_name_does_actually_not_exist" );
	}

	@Test
	public void skipAnalysis() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "world another world" ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// ignoring the analyzer means that the parameter of match predicate will not be tokenized
		// so it will not match any token
		query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "world another world" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to pass the parameter as a token is
		query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "world" ).skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void skipAnalysis_fuzzy() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "word another word" ).fuzzy() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// ignoring the analyzer means that the parameter of match predicate will not be tokenized
		// so it will not match any token
		query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "word another word" ).fuzzy().skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// to have a match with the skipAnalysis option enabled, we have to pass the parameter as a token is
		query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "word" ).fuzzy().skipAnalysis() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void analyzerOverride_normalizedStringField() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().normalizedStringField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				// the matching parameter will be tokenized even if the field has a normalizer
				.where( f -> f.match().field( absoluteFieldPath ).matching( "Auster Coe" )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	@TestForIssue( jiraKey = { "HSEARCH-2534", "HSEARCH-3042" } )
	public void analyzerOverride_queryOnlyAnalyzer() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().whitespaceLowercaseAnalyzedField.relativeFieldName;
		String ngramFieldPath = mainIndex.binding().ngramSearchAnalyzedField.relativeFieldName;

		// Using the white space lower case analyzer, we don't have any matching.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "worldofwordcraft" ) )
				.toQuery();
		assertThat( query ).hasNoHits();

		// Overriding the analyzer with a n-gram analyzer for the specific query,
		// the same query matches all values.
		query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "worldofwordcraft" )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		// Defining a ngram search analyzer to override the analyzer,
		// we expect the same result.
		query = scope.query()
				.where( f -> f.match().field( ngramFieldPath ).matching( "worldofwordcraft" ) )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void multiFields() {
		StubMappingScope scope = mainIndex.createScope();

		// field(...).field(...)

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( mainIndex.binding().string1Field.relativeFieldName )
						.field( mainIndex.binding().string2Field.relativeFieldName )
						.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.match().field( mainIndex.binding().string1Field.relativeFieldName )
						.field( mainIndex.binding().string2Field.relativeFieldName )
						.matching( mainIndex.binding().string2Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// field().fields(...)

		query = scope.query()
				.where( f -> f.match().field( mainIndex.binding().string1Field.relativeFieldName )
						.fields( mainIndex.binding().string2Field.relativeFieldName, mainIndex.binding().string3Field.relativeFieldName )
						.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.match().field( mainIndex.binding().string1Field.relativeFieldName )
						.fields( mainIndex.binding().string2Field.relativeFieldName, mainIndex.binding().string3Field.relativeFieldName )
						.matching( mainIndex.binding().string2Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.match().field( mainIndex.binding().string1Field.relativeFieldName )
						.fields( mainIndex.binding().string2Field.relativeFieldName, mainIndex.binding().string3Field.relativeFieldName )
						.matching( mainIndex.binding().string3Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		// fields(...)

		query = scope.query()
				.where( f -> f.match().fields( mainIndex.binding().string1Field.relativeFieldName, mainIndex.binding().string3Field.relativeFieldName )
						.matching( mainIndex.binding().string1Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.match().fields( mainIndex.binding().string1Field.relativeFieldName, mainIndex.binding().string2Field.relativeFieldName )
						.matching( mainIndex.binding().string2Field.document1Value.indexedValue )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void multiFields_withDslConverter_dslConverterEnabled() {
		SearchQuery<DocumentReference> query = mainIndex.createScope().query()
				.where( f -> f.match()
						.field( mainIndex.binding().string1FieldWithDslConverter.relativeFieldName )
						.field( mainIndex.binding().string2FieldWithDslConverter.relativeFieldName )
						.matching( new ValueWrapper<>( mainIndex.binding().string1FieldWithDslConverter.document3Value.indexedValue ) )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void multiFields_withDslConverter_dslConverterDisabled() {
		SearchQuery<DocumentReference> query = mainIndex.createScope().query()
				.where( f -> f.match()
						.field( mainIndex.binding().string1FieldWithDslConverter.relativeFieldName )
						.field( mainIndex.binding().string2FieldWithDslConverter.relativeFieldName )
						.matching( mainIndex.binding().string1FieldWithDslConverter.document3Value.indexedValue, ValueConvert.NO )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = mainIndex.createScope();

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( "unknown_field" ),
				"match() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().fields( mainIndex.binding().string1Field.relativeFieldName, "unknown_field" ),
				"match() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( mainIndex.binding().string1Field.relativeFieldName ).field( "unknown_field" ),
				"match() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().match().field( mainIndex.binding().string1Field.relativeFieldName ).fields( "unknown_field" ),
				"match() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void error_invalidType() {
		StubMappingScope scope = mainIndex.createScope();

		List<ByTypeFieldModel<?>> fieldModels = new ArrayList<>();
		fieldModels.addAll( mainIndex.binding().supportedFieldModels );
		fieldModels.addAll( mainIndex.binding().supportedFieldWithDslConverterModels );

		for ( ByTypeFieldModel<?> fieldModel : fieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object invalidValueToMatch = new InvalidType();

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match().field( absoluteFieldPath ).matching( invalidValueToMatch ),
					"match() predicate with invalid parameter type on field " + absoluteFieldPath
			)
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
	public void multiIndex_withCompatibleIndex_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;
				Object valueToMatch = model.predicateParameterValue;

				SearchQuery<DocumentReference> query = scope.query()
						.where( f -> f.match().field( absoluteFieldPath ).matching( valueToMatch ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), DOCUMENT_1 );
					b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_dslConverterEnabled() {
		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			Object valueToMatch = fieldModel.predicateParameterValue;

			Assertions.assertThatThrownBy(
					() -> {
						mainIndex.createScope( rawFieldCompatibleIndex )
								.predicate().match().field( fieldModel.relativeFieldName ).matching( valueToMatch );
					}
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldModel.relativeFieldName + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( mainIndex.name(), rawFieldCompatibleIndex.name() )
					) );
		}
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			SubTest.expectSuccess( fieldModel, model -> {
				String absoluteFieldPath = model.relativeFieldName;
				Object valueToMatch = model.predicateParameterValue;

				SearchQuery<DocumentReference> query = scope.query()
						.where( f -> f.match().field( absoluteFieldPath ).matching( valueToMatch, ValueConvert.NO ) )
						.toQuery();

				assertThat( query ).hasDocRefHitsAnyOrder( b -> {
					b.doc( mainIndex.typeName(), DOCUMENT_1 );
					b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
				} );
			} );
		}
	}

	@Test
	public void multiIndex_withIncompatibleIndex_dslConverterEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match().field( fieldPath )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
					) );
		}
	}

	@Test
	public void multiIndex_withIncompatibleIndex_dslConverterDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match().field( fieldPath )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
					) );
		}
	}

	@Test
	public void multiIndex_incompatibleAnalyzer() {
		StubMappingScope scope = mainIndex.createScope( incompatibleAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> {
					scope.query()
							.where( f -> f.match().field( absoluteFieldPath ).matching( "fox" ) )
							.toQuery();
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'analyzedString'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleAnalyzerIndex.name() )
				) );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_overrideAnalyzer() {
		StubMappingScope scope = mainIndex.createScope( incompatibleAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "fox" )
						.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( incompatibleAnalyzerIndex.typeName(), INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_searchAnalyzer() {
		StubMappingScope scope = mainIndex.createScope( compatibleSearchAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "fox" ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( compatibleSearchAnalyzerIndex.typeName(), COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_skipAnalysis() {
		StubMappingScope scope = mainIndex.createScope( incompatibleAnalyzerIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.match().field( absoluteFieldPath ).matching( "fox" )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( incompatibleAnalyzerIndex.typeName(), INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_incompatibleDecimalScale() {
		StubMappingScope scope = mainIndex.createScope( incompatibleDecimalScaleIndex );
		String absoluteFieldPath = mainIndex.binding().scaledBigDecimal.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> {
					scope.query().selectEntityReference()
							.where( f -> f.match().field( absoluteFieldPath ).matching( new BigDecimal( "739.739" ) ) )
							.toQuery();
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'scaledBigDecimal'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleDecimalScaleIndex.name() )
				) );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = mainIndex.createScope( unsearchableFieldsIndex );

		for ( ByTypeFieldModel<?> fieldModel : mainIndex.binding().supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().match().field( fieldPath )
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Multiple conflicting types to build a predicate" )
					.hasMessageContaining( "'" + fieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexNames( mainIndex.name(), unsearchableFieldsIndex.name() )
					) );
		}
	}

	private void initData() {
		BulkIndexer mainIndexer = mainIndex.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
					mainIndex.binding().supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
					mainIndex.binding().unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );
					mainIndex.binding().string1Field.document1Value.write( document );
					mainIndex.binding().string2Field.document1Value.write( document );
					mainIndex.binding().string3Field.document1Value.write( document );
					mainIndex.binding().string1FieldWithDslConverter.document1Value.write( document );
					mainIndex.binding().string2FieldWithDslConverter.document1Value.write( document );
					mainIndex.binding().analyzedStringField.document1Value.write( document );
					mainIndex.binding().analyzedStringField2.document1Value.write( document );
					mainIndex.binding().normalizedStringField.document1Value.write( document );
					mainIndex.binding().whitespaceAnalyzedField.document1Value.write( document );
					mainIndex.binding().whitespaceLowercaseAnalyzedField.document1Value.write( document );
					mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.document1Value.write( document );
					mainIndex.binding().ngramSearchAnalyzedField.document1Value.write( document );
					mainIndex.binding().scaledBigDecimal.document1Value.write( document );
				} )
				.add( DOCUMENT_2, document -> {
					mainIndex.binding().supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
					mainIndex.binding().supportedFieldWithDslConverterModels.forEach( f -> f.document2Value.write( document ) );
					mainIndex.binding().unsupportedFieldModels.forEach( f -> f.document2Value.write( document ) );
					mainIndex.binding().string1Field.document2Value.write( document );
					mainIndex.binding().string2Field.document2Value.write( document );
					mainIndex.binding().string3Field.document2Value.write( document );
					mainIndex.binding().string1FieldWithDslConverter.document2Value.write( document );
					mainIndex.binding().string2FieldWithDslConverter.document2Value.write( document );
					mainIndex.binding().analyzedStringField.document2Value.write( document );
					mainIndex.binding().analyzedStringField2.document2Value.write( document );
					mainIndex.binding().normalizedStringField.document2Value.write( document );
					mainIndex.binding().whitespaceAnalyzedField.document2Value.write( document );
					mainIndex.binding().whitespaceLowercaseAnalyzedField.document2Value.write( document );
					mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.document2Value.write( document );
					mainIndex.binding().ngramSearchAnalyzedField.document2Value.write( document );
					mainIndex.binding().scaledBigDecimal.document2Value.write( document );
				} )
				.add( EMPTY, document -> { } )
				.add( DOCUMENT_3, document -> {
					mainIndex.binding().string1Field.document3Value.write( document );
					mainIndex.binding().string2Field.document3Value.write( document );
					mainIndex.binding().string3Field.document3Value.write( document );
					mainIndex.binding().string1FieldWithDslConverter.document3Value.write( document );
					mainIndex.binding().string2FieldWithDslConverter.document3Value.write( document );
					mainIndex.binding().analyzedStringField.document3Value.write( document );
					mainIndex.binding().analyzedStringField2.document3Value.write( document );
					mainIndex.binding().normalizedStringField.document3Value.write( document );
					mainIndex.binding().whitespaceAnalyzedField.document3Value.write( document );
					mainIndex.binding().whitespaceLowercaseAnalyzedField.document3Value.write( document );
					mainIndex.binding().whitespaceLowercaseSearchAnalyzedField.document3Value.write( document );
					mainIndex.binding().ngramSearchAnalyzedField.document3Value.write( document );
					mainIndex.binding().scaledBigDecimal.document3Value.write( document );
				} );
		BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer()
				.add( COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					compatibleIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
					compatibleIndex.binding().supportedFieldWithDslConverterModels.forEach( f -> f.document1Value.write( document ) );
				} );
		BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer()
				.add( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1, document -> {
					rawFieldCompatibleIndex.binding().supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
				} );
		BulkIndexer incompatibleAnalyzerIndexer = incompatibleAnalyzerIndex.bulkIndexer()
				.add( INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1, document -> {
					incompatibleAnalyzerIndex.binding().analyzedStringField.document1Value.write( document );
				} );
		BulkIndexer compatibleSearchAnalyzerIndexer = compatibleSearchAnalyzerIndex.bulkIndexer()
				.add( COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1, document -> {
					compatibleSearchAnalyzerIndex.binding().analyzedStringField.document1Value.write( document );
				} );
		BulkIndexer incompatibleDecimalScaleIndexer = incompatibleDecimalScaleIndex.bulkIndexer()
				.add( INCOMPATIBLE_DECIMAL_SCALE_INDEX_DOCUMENT_1, document -> {
					incompatibleDecimalScaleIndex.binding().scaledBigDecimal.document1Value.write( document );
				} );
		mainIndexer.join(
				compatibleIndexer, rawFieldCompatibleIndexer, compatibleSearchAnalyzerIndexer,
				incompatibleAnalyzerIndexer, incompatibleDecimalScaleIndexer
		);

		// Check that all documents are searchable
		SearchQuery<DocumentReference> query = mainIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

		query = compatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );

		query = rawFieldCompatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );

		query = incompatibleAnalyzerIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( incompatibleAnalyzerIndex.typeName(), INCOMPATIBLE_ANALYZER_INDEX_DOCUMENT_1 );

		query = compatibleSearchAnalyzerIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( compatibleSearchAnalyzerIndex.typeName(), COMPATIBLE_SEARCH_ANALYZER_INDEX_DOCUMENT_1 );

		query = incompatibleDecimalScaleIndex.createScope().query()
				.selectEntityReference()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( incompatibleDecimalScaleIndex.typeName(), INCOMPATIBLE_DECIMAL_SCALE_INDEX_DOCUMENT_1 );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, ?>> additionalConfiguration,
			FieldModelConsumer<MatchPredicateExpectations<?>, ByTypeFieldModel<?>> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			// Safe, see forEachTypeDescriptor
			MatchPredicateExpectations<?> expectations = typeDescriptor.getMatchPredicateExpectations().get();
			ByTypeFieldModel<?> fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
			consumer.accept( typeDescriptor, expectations, fieldModel );
		} );
	}

	private static class IndexBinding {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> unsupportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<String>> fuzzySupportedFieldModels = new ArrayList<>();
		final List<ByTypeFieldModel<?>> fuzzyUnsupportedFieldModels = new ArrayList<>();

		final MainFieldModel<String> string1Field;
		final MainFieldModel<String> string2Field;
		final MainFieldModel<String> string3Field;
		final MainFieldModel<String> analyzedStringField;
		final MainFieldModel<String> analyzedStringField2;
		final MainFieldModel<String> normalizedStringField;

		final MainFieldModel<String> string1FieldWithDslConverter;
		final MainFieldModel<String> string2FieldWithDslConverter;

		final MainFieldModel<String> whitespaceAnalyzedField;
		final MainFieldModel<String> whitespaceLowercaseAnalyzedField;
		final MainFieldModel<String> whitespaceLowercaseSearchAnalyzedField;
		final MainFieldModel<String> ngramSearchAnalyzedField;

		final MainFieldModel<BigDecimal> scaledBigDecimal;

		@SuppressWarnings("unchecked")
		IndexBinding(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_", ignored -> { },
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isMatchPredicateSupported() ) {
							supportedFieldModels.add( model );
							if ( String.class.equals( typeDescriptor.getJavaType() ) ) {
								fuzzySupportedFieldModels.add( (ByTypeFieldModel<String>) model );
							}
							else {
								fuzzyUnsupportedFieldModels.add( model );
							}
						}
						else {
							unsupportedFieldModels.add( model );
						}
					}
			);
			mapByTypeFields(
					root, "byType_converted_", c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isMatchPredicateSupported() ) {
							supportedFieldWithDslConverterModels.add( model );
						}
					}
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
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ),
					"quick brown fox", "another word", "a"
			)
					.map( root, "analyzedString" );
			analyzedStringField2 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ),
					"another world", "blue whale", "the"
			)
					.map( root, "analyzedString2" );
			normalizedStringField = MainFieldModel.mapper(
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ),
					"Irving", "Auster", "Coe"
			)
					.map( root, "normalizedString" );
			string1FieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ),
					"thread", "local", "company"
			)
					.map( root, "string1FieldWithDslConverter" );
			string2FieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ),
					"Mapper", "ORM", "Pojo"
			)
					.map( root, "string2FieldWithDslConverter" );
			whitespaceAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ),
					"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
			)
					.map( root, "whitespaceAnalyzed" );
			whitespaceLowercaseAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ),
					"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
			)
					.map( root, "whitespaceLowercaseAnalyzed" );
			whitespaceLowercaseSearchAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ),
					"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
			)
					.map( root, "whitespaceLowercaseSearchAnalyzed" );
			ngramSearchAnalyzedField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
							.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ),
					"brave new world", "BRAVE NEW WORLD", "BRave NeW WoRlD"
			)
					.map( root, "ngramSearchAnalyzed" );
			scaledBigDecimal = MainFieldModel.mapper(
					c -> c.asBigDecimal().decimalScale( 3 ),
					new BigDecimal( "739.739" ), BigDecimal.ONE, BigDecimal.TEN
			)
					.map( root, "scaledBigDecimal" );
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexBinding,
			 * but with an incompatible DSL converter.
			 */
			mapByTypeFields(
					root, "byType_", c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() ),
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isMatchPredicateSupported() ) {
							supportedFieldModels.add( model );
						}
					}
			);
		}
	}

	private static class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the supportedFieldModels from IndexBinding,
			 * but with an incompatible type.
			 */
			forEachTypeDescriptor( typeDescriptor -> {
				StandardFieldMapper<?, IncompatibleFieldModel> mapper;
				if ( Integer.class.equals( typeDescriptor.getJavaType() ) ) {
					mapper = IncompatibleFieldModel.mapper( context -> context.asLong() );
				}
				else {
					mapper = IncompatibleFieldModel.mapper( context -> context.asInteger() );
				}
				mapper.map( root, "byType_" + typeDescriptor.getUniqueName() );
			} );
		}
	}

	private static class IncompatibleAnalyzerIndexBinding {
		final MainFieldModel<String> analyzedStringField;

		/*
		 * Unlike IndexBinding#analyzedStringField,
		 * we're using here a different analyzer for the field.
		 */
		IncompatibleAnalyzerIndexBinding(IndexSchemaElement root) {
			analyzedStringField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ),
					"quick brown fox", "another word", "a"
			)
					.map( root, "analyzedString" );
		}
	}

	private static class CompatibleSearchAnalyzerIndexBinding {
		final MainFieldModel<String> analyzedStringField;

		/*
		 * Unlike IndexBinding#analyzedStringField,
		 * we're using here a different analyzer for the field.
		 */
		CompatibleSearchAnalyzerIndexBinding(IndexSchemaElement root) {
			analyzedStringField = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name )
						// Overriding it with a compatible one
						.searchAnalyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ),
					"quick brown fox", "another word", "a"
			)
					.map( root, "analyzedString" );
		}
	}

	private static class IncompatibleDecimalScaleIndexBinding {
		final MainFieldModel<BigDecimal> scaledBigDecimal;

		/*
		 * Unlike IndexBinding#scaledBigDecimal,
		 * we're using here a different decimal scale for the field.
		 */
		IncompatibleDecimalScaleIndexBinding(IndexSchemaElement root) {
			scaledBigDecimal = MainFieldModel.mapper(
					c -> c.asBigDecimal().decimalScale( 7 ),
					new BigDecimal( "739.739" ), BigDecimal.ONE, BigDecimal.TEN
			)
					.map( root, "scaledBigDecimal" );
		}
	}

	private static class UnsearchableFieldsIndexBinding {
		final List<ByTypeFieldModel<?>> supportedFieldModels = new ArrayList<>();

		@SuppressWarnings("unchecked")
		UnsearchableFieldsIndexBinding(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_",
					// make the field not searchable
					c -> c.searchable( Searchable.NO ),
					(typeDescriptor, expectations, model) -> {
						if ( expectations.isMatchPredicateSupported() ) {
							supportedFieldModels.add( model );
						}
					}
			);
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldReference<F> reference;
		final F indexedValue;

		private ValueModel(IndexFieldReference<F> reference, F indexedValue) {
			this.reference = reference;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			target.addValue( reference, indexedValue );
		}
	}

	private static class MainFieldModel<T> {
		static StandardFieldMapper<String, MainFieldModel<String>> mapper(
				String document1Value, String document2Value, String document3Value) {
			return mapper( c -> c.asString(), document1Value, document2Value, document3Value );
		}

		static <LT> StandardFieldMapper<LT, MainFieldModel<LT>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, LT>> configuration,
				LT document1Value, LT document2Value, LT document3Value) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel<>( reference, name, document1Value, document2Value, document3Value )
			);
		}

		final String relativeFieldName;
		final ValueModel<T> document1Value;
		final ValueModel<T> document2Value;
		final ValueModel<T> document3Value;

		private MainFieldModel(IndexFieldReference<T> reference, String relativeFieldName,
				T document1Value, T document2Value, T document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, document1Value );
			this.document3Value = new ValueModel<>( reference, document3Value );
			this.document2Value = new ValueModel<>( reference, document2Value );
		}
	}

	private static class ByTypeFieldModel<F> {
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			// Safe, see caller
			MatchPredicateExpectations<F> expectations = typeDescriptor.getMatchPredicateExpectations().get();
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(reference, name) -> new ByTypeFieldModel<>( reference, name, expectations )
			);
		}

		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;

		final F predicateParameterValue;

		private ByTypeFieldModel(IndexFieldReference<F> reference, String relativeFieldName,
				MatchPredicateExpectations<F> expectations) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, expectations.getDocument1Value() );
			this.document2Value = new ValueModel<>( reference, expectations.getDocument2Value() );
			this.predicateParameterValue = expectations.getMatchingDocument1Value();
		}
	}

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<?, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration) {
			return StandardFieldMapper.of( configuration, (reference, name) -> new IncompatibleFieldModel( name ) );
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
