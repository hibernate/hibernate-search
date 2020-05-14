/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldModelConsumer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class WildcardSearchPredicateIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String DOCUMENT_4 = "document4";
	private static final String DOCUMENT_5 = "document5";
	private static final String EMPTY = "empty";

	private static final String PATTERN_1 = "local*n";
	private static final String PATTERN_2 = "inter*on";
	private static final String PATTERN_3 = "la*d";
	private static final String TEXT_MATCHING_PATTERN_1 = "Localization in English is a must-have.";
	private static final String TEXT_MATCHING_PATTERN_2 = "Internationalization allows to adapt the application to multiple locales.";
	private static final String TEXT_MATCHING_PATTERN_3 = "A had to call the landlord.";
	private static final String TEXT_MATCHING_PATTERN_2_AND_3 = "I had some interaction with that lad.";

	private static final String TERM_PATTERN_1 = "lOCAl*N";
	private static final String TERM_PATTERN_2 = "IN*oN";
	private static final String TERM_PATTERN_3 = "INteR*oN";
	private static final String TERM_PATTERN_1_EXACT_CASE = "Local*n";
	private static final String TERM_PATTERN_2_EXACT_CASE = "iN*On";
	private static final String TERM_PATTERN_3_EXACT_CASE = "Inter*on";
	private static final String TERM_MATCHING_PATTERN_1 = "Localization";
	private static final String TERM_MATCHING_PATTERN_2 = "iNTroSPEctiOn";
	private static final String TERM_MATCHING_PATTERN_2_AND_3 = "Internationalization";

	private static final String COMPATIBLE_INDEX_DOCUMENT_1 = "compatible_1";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 = "raw_field_compatible_1";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<OtherIndexBinding> compatibleIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createCompatible ).name( "compatible" );
	private final SimpleMappedIndex<OtherIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createRawFieldCompatible ).name( "rawFieldCompatible" );
	private final SimpleMappedIndex<OtherIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createIncompatible ).name( "incompatible" );
	private final SimpleMappedIndex<OtherIndexBinding> unsearchableFieldsIndex =
			SimpleMappedIndex.of( OtherIndexBinding::createUnsearchableFieldsIndexBinding ).name( "unsearchableFields" );

	@Before
	public void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex, compatibleIndex, rawFieldCompatibleIndex,
						incompatibleIndex, unsearchableFieldsIndex
				)
				.setup();

		initData();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.dsl.DSLTest.testWildcardQuery")
	public void wildcard() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_4 );

		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3612")
	public void wildcard_normalizeMatchingExpression() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().normalizedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( TERM_PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		assertThat( createQuery.apply( TERM_PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_3 );

		assertThat( createQuery.apply( TERM_PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3844") // Used to throw NPE
	public void wildcard_nonAnalyzedField() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().nonAnalyzedField.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery = queryString -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( queryString ) )
				.toQuery();

		assertThat( createQuery.apply( TERM_PATTERN_1 ) )
				.hasNoHits();
		assertThat( createQuery.apply( TERM_PATTERN_1_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );

		assertThat( createQuery.apply( TERM_PATTERN_2 ) )
				.hasNoHits();
		assertThat( createQuery.apply( TERM_PATTERN_2_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2 );

		assertThat( createQuery.apply( TERM_PATTERN_3 ) )
				.hasNoHits();
		assertThat( createQuery.apply( TERM_PATTERN_3_EXACT_CASE ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void wildcard_unsearchable() {
		StubMappingScope scope = unsearchableFieldsIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy( () ->
				scope.predicate().wildcard().field( absoluteFieldPath )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is not searchable" )
				.hasMessageContaining( "Make sure the field is marked as searchable" )
				.hasMessageContaining( absoluteFieldPath );
	}

	/**
	 * Check that a wildcard predicate can be used on a field that has a DSL converter.
	 * The DSL converter should be ignored, and there shouldn't be any exception thrown
	 * (the field should be considered as a text field).
	 */
	@Test
	public void withDslConverter() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringFieldWithDslConverter.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( PATTERN_1 ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
	}

	@Test
	public void emptyString() {
		StubMappingScope scope = mainIndex.createScope();
		MainFieldModel fieldModel = mainIndex.binding().analyzedStringField1;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( fieldModel.relativeFieldName ).matching( "" ) )
				.toQuery();

		assertThat( query )
				.hasNoHits();
	}

	@Test
	public void error_unsupportedFieldType() {
		StubMappingScope scope = mainIndex.createScope();

		for ( ByTypeFieldModel fieldModel : mainIndex.binding().unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			Assertions.assertThatThrownBy(
					() -> scope.predicate().wildcard().field( absoluteFieldPath ),
					"wildcard() predicate with unsupported type on field " + absoluteFieldPath
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Text predicates" )
					.hasMessageContaining( "are not supported by" )
					.hasMessageContaining( "'" + absoluteFieldPath + "'" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void error_null() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().wildcard().field( absoluteFieldPath ).matching( null ),
				"wildcard() predicate with null pattern"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid pattern" )
				.hasMessageContaining( "must be non-null" )
				.hasMessageContaining( absoluteFieldPath );
	}

	@Test
	public void fieldLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard()
						.field( mainIndex.binding().analyzedStringField1.relativeFieldName ).boost( 42 )
						.field( mainIndex.binding().analyzedStringField2.relativeFieldName )
						.matching( PATTERN_1 )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );

		query = scope.query()
				.where( f -> f.wildcard()
						.field( mainIndex.binding().analyzedStringField1.relativeFieldName )
						.field( mainIndex.binding().analyzedStringField2.relativeFieldName ).boost( 42 )
						.matching( PATTERN_1 )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_5, DOCUMENT_1 );
	}

	@Test
	public void predicateLevelBoost() {
		StubMappingScope scope = mainIndex.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.wildcard().field( mainIndex.binding().analyzedStringField1.relativeFieldName )
								.matching( PATTERN_1 )
						)
						.should( f.wildcard().field( mainIndex.binding().analyzedStringField2.relativeFieldName )
								.matching( PATTERN_1 )
								.boost( 7 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_5, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.wildcard().field( mainIndex.binding().analyzedStringField1.relativeFieldName )
								.matching( PATTERN_1 )
								.boost( 39 )
						)
						.should( f.wildcard().field( mainIndex.binding().analyzedStringField2.relativeFieldName )
								.matching( PATTERN_1 )
						)
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
	}

	@Test
	public void multiFields() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath1 = mainIndex.binding().analyzedStringField1.relativeFieldName;
		String absoluteFieldPath2 = mainIndex.binding().analyzedStringField2.relativeFieldName;
		String absoluteFieldPath3 = mainIndex.binding().analyzedStringField3.relativeFieldName;
		Function<String, SearchQuery<DocumentReference>> createQuery;

		// field(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath1 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );

		// field(...).field(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath1 )
						.field( absoluteFieldPath2 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );

		// field().fields(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath1 )
						.fields( absoluteFieldPath2, absoluteFieldPath3 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4, DOCUMENT_5 );

		// fields(...)

		createQuery = pattern -> scope.query()
				.where( f -> f.wildcard().fields( absoluteFieldPath1, absoluteFieldPath2 )
						.matching( pattern )
				)
				.toQuery();

		assertThat( createQuery.apply( PATTERN_1 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_5 );
		assertThat( createQuery.apply( PATTERN_2 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_2, DOCUMENT_4 );
		assertThat( createQuery.apply( PATTERN_3 ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void error_unknownField() {
		StubMappingScope scope = mainIndex.createScope();
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> scope.predicate().wildcard().field( "unknown_field" ),
				"wildcard() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().wildcard()
						.fields( absoluteFieldPath, "unknown_field" ),
				"wildcard() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().wildcard().field( absoluteFieldPath )
						.field( "unknown_field" ),
				"wildcard() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().wildcard().field( absoluteFieldPath )
						.fields( "unknown_field" ),
				"wildcard() predicate with unknown field"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope(
				compatibleIndex
		);

		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( PATTERN_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.wildcard().field( absoluteFieldPath ).matching( PATTERN_1 ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( mainIndex.typeName(), DOCUMENT_1 );
			b.doc( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
		} );
	}

	@Test
	public void multiIndex_withIncompatibleIndex() {
		// TODO HSEARCH-3307 re-enable this test once we properly take analyzer/normalizer into account when testing field compatibility for predicates in Elasticsearch
		Assume.assumeTrue( "This feature is not implemented yet", false );

		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy(
				() -> {
					mainIndex.createScope( incompatibleIndex )
							.predicate().wildcard().field( absoluteFieldPath );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'" + absoluteFieldPath + "'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
	}

	@Test
	public void multiIndex_incompatibleSearchable() {
		StubMappingScope scope = mainIndex.createScope( unsearchableFieldsIndex );
		String absoluteFieldPath = mainIndex.binding().analyzedStringField1.relativeFieldName;

		Assertions.assertThatThrownBy( () -> scope.predicate().wildcard().field( absoluteFieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( absoluteFieldPath )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), unsearchableFieldsIndex.name() )
				) )
		;
	}

	private void initData() {
		IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( mainIndex.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
			document.addValue( mainIndex.binding().analyzedStringFieldWithDslConverter.reference, TEXT_MATCHING_PATTERN_1 );
			document.addValue( mainIndex.binding().normalizedField.reference, TERM_MATCHING_PATTERN_1 );
			document.addValue( mainIndex.binding().nonAnalyzedField.reference, TERM_MATCHING_PATTERN_1 );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( mainIndex.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_2 );
			document.addValue( mainIndex.binding().normalizedField.reference, TERM_MATCHING_PATTERN_2 );
			document.addValue( mainIndex.binding().nonAnalyzedField.reference, TERM_MATCHING_PATTERN_2 );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( mainIndex.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_3 );
			document.addValue( mainIndex.binding().normalizedField.reference, TERM_MATCHING_PATTERN_2_AND_3 );
			document.addValue( mainIndex.binding().nonAnalyzedField.reference, TERM_MATCHING_PATTERN_2_AND_3 );
		} );
		plan.add( referenceProvider( DOCUMENT_4 ), document -> {
			document.addValue( mainIndex.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_2_AND_3 );
		} );
		plan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addValue( mainIndex.binding().analyzedStringField2.reference, TEXT_MATCHING_PATTERN_1 );
			document.addValue( mainIndex.binding().analyzedStringField3.reference, TEXT_MATCHING_PATTERN_3 );
		} );
		plan.add( referenceProvider( EMPTY ), document -> {
		} );
		plan.execute().join();

		plan = compatibleIndex.createIndexingPlan();
		plan.add( referenceProvider( COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( compatibleIndex.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
		} );
		plan.execute().join();

		plan = rawFieldCompatibleIndex.createIndexingPlan();
		plan.add( referenceProvider( RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 ), document -> {
			document.addValue( rawFieldCompatibleIndex.binding().analyzedStringField1.reference, TEXT_MATCHING_PATTERN_1 );
		} );
		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = mainIndex.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5, EMPTY );
		query = compatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( compatibleIndex.typeName(), COMPATIBLE_INDEX_DOCUMENT_1 );
		query = rawFieldCompatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( rawFieldCompatibleIndex.typeName(), RAW_FIELD_COMPATIBLE_INDEX_DOCUMENT_1 );
	}

	private static void forEachTypeDescriptor(Consumer<FieldTypeDescriptor<?>> action) {
		FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> typeDescriptor.getMatchPredicateExpectations().isPresent() )
				.forEach( action );
	}

	private static void mapByTypeFields(IndexSchemaElement parent, String prefix,
			FieldModelConsumer<Void, ByTypeFieldModel> consumer) {
		forEachTypeDescriptor( typeDescriptor -> {
			ByTypeFieldModel fieldModel = ByTypeFieldModel.mapper( typeDescriptor )
					.map( parent, prefix + typeDescriptor.getUniqueName() );
			consumer.accept( typeDescriptor, null, fieldModel );
		} );
	}

	private static class IndexBinding {
		final List<ByTypeFieldModel> unsupportedFieldModels = new ArrayList<>();

		final MainFieldModel analyzedStringField1;
		final MainFieldModel analyzedStringField2;
		final MainFieldModel analyzedStringField3;
		final MainFieldModel analyzedStringFieldWithDslConverter;
		final MainFieldModel normalizedField;
		final MainFieldModel nonAnalyzedField;

		IndexBinding(IndexSchemaElement root) {
			mapByTypeFields(
					root, "byType_",
					(typeDescriptor, ignored, model) -> {
						if ( !String.class.equals( typeDescriptor.getJavaType() ) ) {
							unsupportedFieldModels.add( model );
						}
					}
			);
			analyzedStringField1 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString1" );
			analyzedStringField2 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString2" );
			analyzedStringField3 = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			)
					.map( root, "analyzedString3" );
			analyzedStringFieldWithDslConverter = MainFieldModel.mapper(
					c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
			)
					.map( root, "analyzedStringWithDslConverter" );
			normalizedField = MainFieldModel.mapper(
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.map( root, "normalized" );
			nonAnalyzedField = MainFieldModel.mapper( c -> c.asString() )
					.map( root, "nonAnalyzed" );
		}
	}

	private static class OtherIndexBinding {
		static OtherIndexBinding createCompatible(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createRawFieldCompatible(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							c -> c.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
									// Using a different DSL converter
									.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createIncompatible(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							// Using a different analyzer/normalizer
							c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
					)
							.map( root, "analyzedString1" )
			);
		}

		static OtherIndexBinding createUnsearchableFieldsIndexBinding(IndexSchemaElement root) {
			return new OtherIndexBinding(
					MainFieldModel.mapper(
							// make the field not searchable
							c -> c.asString().searchable( Searchable.NO )
					)
							.map( root, "analyzedString1" )
			);
		}

		final MainFieldModel analyzedStringField1;

		private OtherIndexBinding(MainFieldModel analyzedStringField1) {
			this.analyzedStringField1 = analyzedStringField1;
		}
	}

	private static class MainFieldModel {
		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, String>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					(reference, name) -> new MainFieldModel( reference, name )
			);
		}

		final IndexFieldReference<String> reference;
		final String relativeFieldName;

		private MainFieldModel(IndexFieldReference<String> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}
	}

	private static class ByTypeFieldModel {
		static <F> StandardFieldMapper<F, ByTypeFieldModel> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					(reference, name) -> new ByTypeFieldModel( name )
			);
		}

		final String relativeFieldName;

		private ByTypeFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}

}