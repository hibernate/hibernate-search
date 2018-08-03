/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.sort;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SearchSortByFieldIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";
	private static final String EMPTY = "empty";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private IndexManager<?> indexManager;
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

	private SearchQuery<DocumentReference> simpleQuery(Consumer<? super SearchSortContainerContext<SearchSort>> sortContributor) {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		return searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort( sortContributor )
				.build();
	}

	@Test
	public void byField() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
		}
	}

	@Test
	public void byField_inFlattenedObject() {
		for ( ByTypeFieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SearchQuery<DocumentReference> query;
			String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			query = simpleQuery( b -> b.byField( fieldPath ).onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortLast() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1, EMPTY );

			query = simpleQuery( b -> b.byField( fieldPath ).asc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

			query = simpleQuery( b -> b.byField( fieldPath ).desc().onMissingValue().sortFirst() );
			DocumentReferencesSearchResultAssert.assertThat( query )
					.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );
		}
	}

	@Test
	public void multipleFields() {
		SearchQuery<DocumentReference> query;

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).desc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_2, DOCUMENT_1 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).asc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).desc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_2, DOCUMENT_1, DOCUMENT_3 );

		query = simpleQuery( b -> b
				.byField( indexMapping.identicalForFirstTwo.relativeFieldName ).desc().onMissingValue().sortFirst()
				.then().byField( indexMapping.identicalForLastTwo.relativeFieldName ).asc()
		);
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsExactOrder( INDEX_NAME, EMPTY, DOCUMENT_3, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void error_unsortable() {
		Assume.assumeTrue( "Errors on attempt to sort on unsortable fields are not supported yet", false );
		// TODO throw an error on attempts to sort on unsortable fields
	}

	@Test
	public void error_unknownField() {
		Assume.assumeTrue( "Errors on attempt to sort on unknown fields are not supported yet", false );
		// TODO throw an error on attempts to sort on unknown fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		String absoluteFieldPath = "unknownField";

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( absoluteFieldPath ).end()
				.build();
	}

	@Test
	public void error_objectField_nested() {
		Assume.assumeTrue( "Errors on attempt to sort on object fields are not supported yet", false );
		// TODO throw an error on attempts to sort on object fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		String absoluteFieldPath = indexMapping.nestedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( absoluteFieldPath ).end()
				.build();
	}

	@Test
	public void error_objectField_flattened() {
		Assume.assumeTrue( "Errors on attempt to sort on object fields are not supported yet", false );
		// TODO throw an error on attempts to sort on object fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		String absoluteFieldPath = indexMapping.flattenedObject.relativeFieldName;

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown field" );
		thrown.expectMessage( absoluteFieldPath );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( absoluteFieldPath ).end()
				.build();
	}

	private void initData() {
		ChangesetIndexWorker<? extends DocumentElement> worker = indexManager.createWorker( sessionContext );
		// Important: do not index the documents in the expected order after sorts (1, 2, 3)
		worker.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document2Value.write( document ) );

			indexMapping.identicalForFirstTwo.document2Value.write( document );
			indexMapping.identicalForLastTwo.document2Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
		} );
		worker.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document1Value.write( document ) );

			indexMapping.identicalForFirstTwo.document1Value.write( document );
			indexMapping.identicalForLastTwo.document1Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
		} );
		worker.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.unsupportedFieldModels.forEach( f -> f.document3Value.write( document ) );

			indexMapping.identicalForFirstTwo.document3Value.write( document );
			indexMapping.identicalForLastTwo.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );
			indexMapping.flattenedObject.unsupportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
			indexMapping.nestedObject.unsupportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
		} );
		worker.add( referenceProvider( EMPTY ), document -> { } );

		worker.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		final MainFieldModel identicalForFirstTwo;
		final MainFieldModel identicalForLastTwo;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapSupportedFields( root );
			unsupportedFieldModels = mapUnsupportedFields( root );

			identicalForFirstTwo = MainFieldModel.mapper(
					"aaron", "aaron", "zach"
			)
					.map( root, "identicalForFirstTwo", c -> c.sortable( Sortable.YES ) );
			identicalForLastTwo = MainFieldModel.mapper(
					"aaron", "zach", "zach"
			)
					.map( root, "identicalForLastTwo", c -> c.sortable( Sortable.YES ) );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldAccessor self;
		final List<ByTypeFieldModel<?>> supportedFieldModels;
		final List<ByTypeFieldModel<?>> unsupportedFieldModels;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.createAccessor();
			supportedFieldModels = mapSupportedFields( objectField );
			unsupportedFieldModels = mapUnsupportedFields( objectField );
		}
	}

	private static List<ByTypeFieldModel<?>> mapSupportedFields(IndexSchemaElement root) {
		return Arrays.asList(
				ByTypeFieldModel
						// Mix capitalized and non-capitalized text on purpose
						.mapper( String.class, "Aaron", "george", "Zach" )
						.map(
								// TODO use a normalizer instead of an analyzer (needs support for normalizer definitions)
								root, "analyzedString", c -> c.analyzer( "default" )
						),
				ByTypeFieldModel.mapper( String.class, "aaron", "george", "zach" )
						.map( root, "nonAnalyzedString" ),
				ByTypeFieldModel.mapper( Integer.class, 1, 2, 3 )
						.map( root, "integer" ),
				ByTypeFieldModel.mapper(
						LocalDate.class,
						LocalDate.of( 2018, 2, 1 ),
						LocalDate.of( 2018, 2, 2 ),
						LocalDate.of( 2018, 2, 3 )
				)
						.map( root, "localDate" )
		);
	}

	private static List<ByTypeFieldModel<?>> mapUnsupportedFields(IndexSchemaElement root) {
		return Arrays.asList(
				ByTypeFieldModel.mapper(
						GeoPoint.class,
						new ImmutableGeoPoint( 40, 70 ),
						new ImmutableGeoPoint( 40, 71 ),
						new ImmutableGeoPoint( 40, 72 )
				)
						.map( root, "geoPoint" )
		);
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
			return (parent, name, configuration) -> {
				StandardIndexSchemaFieldTypedContext<String> context = parent.field( name ).asString();
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
		static <F> StandardFieldMapper<F, ByTypeFieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return (parent, name, configuration) -> {
				StandardIndexSchemaFieldTypedContext<F> context = parent.field( name ).as( type );
				context.sortable( Sortable.YES );
				configuration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new ByTypeFieldModel<>(
						accessor, name, document1Value, document2Value, document3Value
				);
			};
		}

		final String relativeFieldName;
		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private ByTypeFieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
		}
	}
}
