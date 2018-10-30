/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.ArrayList;
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
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String DOCUMENT_3 = "document3";
	private static final String EMPTY = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

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
	public void match() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = fieldModel.predicateParameterValue;

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ).toPredicate() )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void match_withDslConverter() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object valueToMatch = new ValueWrapper<>( fieldModel.predicateParameterValue );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( f -> f.match().onField( absoluteFieldPath ).matching( valueToMatch ).toPredicate() )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void match_emptyStringBeforeAnalysis() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		MainFieldModel fieldModel = indexMapping.analyzedStringField;

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( fieldModel.relativeFieldName ).matching( "" ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasNoHits();
	}

	@Test
	public void match_noTokenAfterAnalysis() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		MainFieldModel fieldModel = indexMapping.analyzedStringField;

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				// Use a stopword, which should be removed by the analysis
				.predicate( f -> f.match().onField( fieldModel.relativeFieldName ).matching( "a" ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasNoHits();
	}

	@Test
	public void unsupported_field_types() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.match().onField( indexMapping.string1Field.relativeFieldName )
								.matching( indexMapping.string1Field.document1Value.indexedValue ).toPredicate()
						)
						.should( c -> c.match().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.matching( indexMapping.string1Field.document3Value.indexedValue ).toPredicate()
						)
						.toPredicate()
				)
				.sort( c -> c.byScore() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.match().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.matching( indexMapping.string1Field.document1Value.indexedValue ).toPredicate()
						)
						.should( c -> c.match().onField( indexMapping.string1Field.relativeFieldName )
								.matching( indexMapping.string1Field.document3Value.indexedValue ).toPredicate()
						)
						.toPredicate()
				)
				.sort( c -> c.byScore() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void multi_fields() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.matching( indexMapping.string1Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.matching( indexMapping.string2Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// onField().orFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string1Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string2Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string3Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// onFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.matching( indexMapping.string1Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.match().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string2Field.relativeFieldName )
						.matching( indexMapping.string2Field.document1Value.indexedValue )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void unknown_field() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

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

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
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

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, EMPTY,
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

	private static class MainFieldModel {
		static StandardFieldMapper<String, MainFieldModel> mapper(
				String document1Value, String document2Value, String document3Value) {
			return mapper( c -> c.asString(), document1Value, document2Value, document3Value );
		}

		static StandardFieldMapper<String, MainFieldModel> mapper(
				Function<IndexSchemaFieldContext, StandardIndexSchemaFieldTypedContext<?, String>> configuration,
				String document1Value, String document2Value, String document3Value) {
			return (parent, name, additionalConfiguration) -> {
				StandardIndexSchemaFieldTypedContext<?, String> context = configuration.apply( parent.field( name ) );
				additionalConfiguration.accept( context );
				IndexFieldAccessor<String> accessor = context.createAccessor();
				return new MainFieldModel( accessor, name, document1Value, document3Value, document2Value );
			};
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
						accessor, name, document1Value, document2Value, predicateParameterValue
				);
			};
		}

		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;

		final F predicateParameterValue;

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName,
				F document1Value, F document2Value, F predicateParameterValue) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.predicateParameterValue = predicateParameterValue;
		}
	}
}
