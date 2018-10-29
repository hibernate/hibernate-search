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

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.RangeBoundInclusion;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.util.InvalidType;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RangeSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY_ID = "empty";

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
	public void above() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = fieldModel.predicateLowerBound;

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).above( lowerValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void above_withDslConverter() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = new ValueWrapper<>( fieldModel.predicateLowerBound );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).above( lowerValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
		}
	}

	@Test
	public void above_include_exclude() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = fieldModel.document2Value.indexedValue;

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).above( lowerValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

			// explicit inclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).above( lowerValueToMatch, RangeBoundInclusion.INCLUDED ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

			// explicit exclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).above( lowerValueToMatch, RangeBoundInclusion.EXCLUDED ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
		}
	}

	@Test
	public void below() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = fieldModel.predicateUpperBound;

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void below_withDslConverter() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = new ValueWrapper<>( fieldModel.predicateUpperBound );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
	}

	@Test
	public void below_include_exclude() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object upperValueToMatch = fieldModel.document2Value.indexedValue;

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).below( upperValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit inclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).below( upperValueToMatch, RangeBoundInclusion.INCLUDED ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit exclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).below( upperValueToMatch, RangeBoundInclusion.EXCLUDED ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	@Test
	public void fromTo() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = fieldModel.predicateLowerBound;
			Object upperValueToMatch = fieldModel.predicateUpperBound;

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).from( lowerValueToMatch ).to( upperValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void fromTo_withDslConverter() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldWithDslConverterModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object lowerValueToMatch = new ValueWrapper<>( fieldModel.predicateLowerBound );
			Object upperValueToMatch = new ValueWrapper<>( fieldModel.predicateUpperBound );

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).from( lowerValueToMatch ).to( upperValueToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void fromTo_include_exclude() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;
			Object value1ToMatch = fieldModel.document1Value.indexedValue;
			Object value2ToMatch = fieldModel.document2Value.indexedValue;
			Object value3ToMatch = fieldModel.document3Value.indexedValue;

			// Default is inclusion

			SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath ).from( value1ToMatch ).to( value2ToMatch ) )
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit inclusion

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.INCLUDED )
							.to( value2ToMatch, RangeBoundInclusion.INCLUDED )
					)
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

			// explicit exclusion for the from clause

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.EXCLUDED )
							.to( value2ToMatch, RangeBoundInclusion.INCLUDED )
					)
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

			// explicit exclusion for the to clause

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.INCLUDED )
							.to( value2ToMatch, RangeBoundInclusion.EXCLUDED )
					)
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

			// explicit exclusion for both clauses

			query = searchTarget.query( sessionContext )
					.asReferences()
					.predicate( root -> root.range().onField( absoluteFieldPath )
							.from( value1ToMatch, RangeBoundInclusion.EXCLUDED )
							.to( value3ToMatch, RangeBoundInclusion.EXCLUDED )
					)
					.build();

			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
		}
	}

	@Test
	public void unsupported_field_types() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.unsupportedFieldModels ) {
			String absoluteFieldPath = fieldModel.relativeFieldName;

			SubTest.expectException(
					"range() predicate with unsupported type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Range predicates are not supported by" )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );
		}
	}

	@Test
	public void boost() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.range().onField( indexMapping.string1Field.relativeFieldName )
								.above( indexMapping.string1Field.document3Value.indexedValue ).end()
						)
						.should( c -> c.range().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.below( indexMapping.string1Field.document1Value.indexedValue ).end()
						)
				)
				.sort().byScore().end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.bool()
						.should( c -> c.range().onField( indexMapping.string1Field.relativeFieldName ).boostedTo( 42 )
								.above( indexMapping.string1Field.document3Value.indexedValue ).end()
						)
						.should( c -> c.range().onField( indexMapping.string1Field.relativeFieldName )
								.below( indexMapping.string1Field.document1Value.indexedValue ).end()
						)
				)
				.sort().byScore().end()
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );
	}

	@Test
	public void multi_fields() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// onField(...).orField(...)

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.below( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onField( indexMapping.string1Field.relativeFieldName )
						.orField( indexMapping.string2Field.relativeFieldName )
						.above( indexMapping.string2Field.document3Value.indexedValue )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onField().orFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.below( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.from( "d" ).to( "e" )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onField( indexMapping.string1Field.relativeFieldName )
						.orFields( indexMapping.string2Field.relativeFieldName, indexMapping.string3Field.relativeFieldName )
						.above( indexMapping.string3Field.document3Value.indexedValue )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		// onFields(...)

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string2Field.relativeFieldName )
						.below( indexMapping.string1Field.document1Value.indexedValue )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.range().onFields( indexMapping.string1Field.relativeFieldName, indexMapping.string2Field.relativeFieldName )
						.above( indexMapping.string2Field.document3Value.indexedValue )
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void range_error_null() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			String fieldPath = fieldModel.relativeFieldName;
			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).from( null ).to( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );

			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).above( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );


			SubTest.expectException(
					"range() predicate with null bounds on field " + fieldPath,
					() -> searchTarget.predicate().range().onField( fieldPath ).below( null )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid value" )
					.hasMessageContaining( "at least one bound" )
					.hasMessageContaining( "must be non-null" )
					.hasMessageContaining( fieldPath );
		}
	}

	@Test
	public void unknown_field() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onFields( indexMapping.string1Field.relativeFieldName, "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( indexMapping.string1Field.relativeFieldName ).orField( "unknown_field" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'unknown_field'" );

		SubTest.expectException(
				"range() predicate with unknown field",
				() -> searchTarget.predicate().range().onField( indexMapping.string1Field.relativeFieldName ).orFields( "unknown_field" )
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
					"range().above() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).above( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			SubTest.expectException(
					"range().below() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).below( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			SubTest.expectException(
					"range().from() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath ).from( invalidValueToMatch )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Unable to convert DSL parameter: " )
					.hasMessageContaining( InvalidType.class.getName() )
					.hasCauseInstanceOf( ClassCastException.class )
					.satisfies( FailureReportUtils.hasContext(
							EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
					) );

			SubTest.expectException(
					"range().from().to() predicate with invalid parameter type on field " + absoluteFieldPath,
					() -> searchTarget.predicate().range().onField( absoluteFieldPath )
							.from( null ).to( invalidValueToMatch )
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
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithDslConverterModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.string1Field.document3Value.write( document );
			indexMapping.string2Field.document3Value.write( document );
			indexMapping.string3Field.document3Value.write( document );
		} );
		workPlan.add( referenceProvider( EMPTY_ID ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( root -> root.matchAll() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY_ID );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> supportedFieldWithDslConverterModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		final MainFieldModel string1Field;
		final MainFieldModel string2Field;
		final MainFieldModel string3Field;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapSupportedFields( root, "", ignored -> { } );
			supportedFieldWithDslConverterModels = mapSupportedFields(
					root, "converted_", c -> c.dslConverter( ValueWrapper.toIndexFieldConverter() )
			);
			unsupportedFieldModels = Arrays.asList(
					ByTypeFieldModel.mapper(
							GeoPoint.class,
							GeoPoint.of( 40, 70 ),
							GeoPoint.of( 40, 71 ),
							GeoPoint.of( 40, 72 ),
							GeoPoint.of( 30, 60 ), GeoPoint.of( 50, 80 )
					)
							.map( root, "geoPoint" )
			);
			string1Field = MainFieldModel.mapper( "ccc", "mmm", "xxx" )
					.map( root, "string1" );
			string2Field = MainFieldModel.mapper( "ddd", "nnn", "yyy" )
					.map( root, "string2" );
			string3Field = MainFieldModel.mapper( "eee", "ooo", "zzz" )
					.map( root, "string3" );
		}

		private List<ByTypeFieldModel<?>> mapSupportedFields(IndexSchemaElement root, String prefix,
				Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
			return Arrays.asList(
					// TODO also test analyzed strings
					ByTypeFieldModel.mapper( String.class, "ccc", "mmm", "xxx",
							"ggg", "rrr"
					)
							.map( root, prefix + "nonAnalyzedString", additionalConfiguration ),
					ByTypeFieldModel.mapper( Integer.class, 3, 13, 25,
							10, 19
					)
							.map( root, prefix + "integer", additionalConfiguration ),
					ByTypeFieldModel.mapper(
							LocalDate.class,
							LocalDate.of( 2003, 6, 3 ),
							LocalDate.of( 2013, 6, 3 ),
							LocalDate.of( 2025, 6, 3 ),
							LocalDate.of( 2010, 6, 8 ), LocalDate.of( 2019, 4, 18 )
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
		public static StandardFieldMapper<String, MainFieldModel> mapper(
				String document1Value, String document2Value, String document3Value) {
			return (parent, name, configuration) -> {
				StandardIndexSchemaFieldTypedContext<?, String> context = parent.field( name ).asString();
				configuration.accept( context );
				IndexFieldAccessor<String> accessor = context.createAccessor();
				return new MainFieldModel( accessor, name, document1Value, document2Value, document3Value );
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
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
		}
	}

	private static class ByTypeFieldModel<F> {
		public static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value,
				F predicateLowerBound, F predicateUpperBound) {
			return (parent, name, configuration) -> {
				StandardIndexSchemaFieldTypedContext<?, F> context = parent.field( name ).as( type );
				configuration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new ByTypeFieldModel<>(
						accessor, name,
						document1Value, document2Value, document3Value,
						predicateLowerBound, predicateUpperBound
				);
			};
		}

		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		final F predicateLowerBound;
		final F predicateUpperBound;

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName,
				F document1Value, F document2Value, F document3Value,
				F predicateLowerBound, F predicateUpperBound) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
			this.predicateLowerBound = predicateLowerBound;
			this.predicateUpperBound = predicateUpperBound;
		}
	}
}
