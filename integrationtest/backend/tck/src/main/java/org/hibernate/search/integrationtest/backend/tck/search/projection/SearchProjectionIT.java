/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.ProjectionsSearchResultAssert.assertThat;
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
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.engine.search.SearchQuery;
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

public class SearchProjectionIT {
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
	public void field_single() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query( sessionContext )
					.asProjections( fieldPath )
					.predicate().matchAll().end()
					.build();
			assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( fieldModel.document1Value.indexedValue );
				b.projection( fieldModel.document2Value.indexedValue );
				b.projection( fieldModel.document3Value.indexedValue );
				b.projection( null ); // Empty document
			} );
		}
	}

	@Test
	public void field_withProjectionConverters() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldWithProjectionConverterModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query( sessionContext )
					.asProjections( fieldPath )
					.predicate().matchAll().end()
					.build();
			assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( new ValueWrapper<>( fieldModel.document1Value.indexedValue ) );
				b.projection( new ValueWrapper<>( fieldModel.document2Value.indexedValue ) );
				b.projection( new ValueWrapper<>( fieldModel.document3Value.indexedValue ) );
				b.projection( new ValueWrapper<>( null ) ); // Empty document
			} );
		}
	}

	@Test
	public void field_duplicated() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( FieldModel<?> fieldModel : indexMapping.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = fieldModel.relativeFieldName;

			query = searchTarget.query( sessionContext )
					.asProjections( fieldPath )
					.predicate().matchAll().end()
					.build();
			assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( fieldModel.document1Value.indexedValue );
				b.projection( fieldModel.document2Value.indexedValue );
				b.projection( fieldModel.document3Value.indexedValue );
				b.projection( null ); // Empty document
			} );
		}
	}

	@Test
	public void projectionConstants_references() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query;
		DocumentReference document1Reference = reference( INDEX_NAME, DOCUMENT_1 );
		DocumentReference document2Reference = reference( INDEX_NAME, DOCUMENT_2 );
		DocumentReference document3Reference = reference( INDEX_NAME, DOCUMENT_3 );
		DocumentReference emptyReference = reference( INDEX_NAME, EMPTY );

		query = searchTarget.query( sessionContext )
				.asProjections( ProjectionConstants.DOCUMENT_REFERENCE, ProjectionConstants.REFERENCE, ProjectionConstants.OBJECT )
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
			b.projection( document1Reference, document1Reference, document1Reference );
			b.projection( document2Reference, document2Reference, document2Reference );
			b.projection( document3Reference, document3Reference, document3Reference );
			b.projection( emptyReference, emptyReference, emptyReference );
		} );
	}

	/**
	 * Test mixing multiple projection types (field projections, special projections, ...),
	 * and also multiple field projections.
	 */
	@Test
	public void mixed() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<List<?>> query;

		query = searchTarget.query( sessionContext )
				.asProjections(
						indexMapping.string1Field.relativeFieldName,
						ProjectionConstants.DOCUMENT_REFERENCE,
						indexMapping.string2Field.relativeFieldName
				)
				.predicate().matchAll().end()
				.build();
		assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
			b.projection(
					indexMapping.string1Field.document1Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_1 ),
					indexMapping.string2Field.document1Value.indexedValue
			);
			b.projection(
					indexMapping.string1Field.document2Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_2 ),
					indexMapping.string2Field.document2Value.indexedValue
			);
			b.projection(
					indexMapping.string1Field.document3Value.indexedValue,
					reference( INDEX_NAME, DOCUMENT_3 ),
					indexMapping.string2Field.document3Value.indexedValue
			);
			b.projection(
					null,
					reference( INDEX_NAME, EMPTY ),
					null
			);
		} );
	}

	@Test
	public void field_inFlattenedObject() {
		Assume.assumeTrue( "Projections on fields within flattened object fields are not supported yet in Elasticsearch", false );
		// TODO support projections on fields within flattened object fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( FieldModel<?> fieldModel : indexMapping.flattenedObject.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = indexMapping.flattenedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			query = searchTarget.query( sessionContext )
					.asProjections( fieldPath )
					.predicate().matchAll().end()
					.build();
			assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( fieldModel.document1Value.indexedValue );
				b.projection( fieldModel.document2Value.indexedValue );
				b.projection( fieldModel.document3Value.indexedValue );
				b.projection( null ); // Empty document
			} );
		}
	}

	@Test
	public void field_inNestedObject() {
		Assume.assumeTrue( "Projections on fields within nested object fields are not supported yet", false );
		// TODO support projections on fields within nested object fields

		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		for ( FieldModel<?> fieldModel : indexMapping.nestedObject.supportedFieldModels ) {
			SearchQuery<List<?>> query;
			String fieldPath = indexMapping.nestedObject.relativeFieldName + "." + fieldModel.relativeFieldName;

			query = searchTarget.query( sessionContext )
					.asProjections( fieldPath )
					.predicate().matchAll().end()
					.build();
			assertThat( query ).hasProjectionsHitsAnyOrder( b -> {
				b.projection( fieldModel.document1Value );
				b.projection( fieldModel.document2Value );
				b.projection( fieldModel.document3Value );
				b.projection( null ); // Empty document
			} );
		}
	}

	@Test
	public void multivalued() {
		Assume.assumeTrue( "Multi-valued projections are not supported yet", false );
		// TODO support multi-valued projections

		// TODO Project on multi-valued field

		// TODO Project on fields within a multi-valued flattened object

		// TODO Project on fields within a multi-valued nested object
	}

	@Test
	public void error_unknownField() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown projections" );
		thrown.expectMessage( "unknownField" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asProjections( "unknownField" )
				.predicate().matchAll().end()
				.build();
	}

	@Test
	public void error_objectField_nested() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown projections" );
		thrown.expectMessage( "nestedObject" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asProjections( "nestedObject" )
				.predicate().matchAll().end()
				.build();
	}

	@Test
	public void error_objectField_flattened() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown projections" );
		thrown.expectMessage( "flattenedObject" );
		thrown.expectMessage( INDEX_NAME );

		searchTarget.query( sessionContext )
				.asProjections( "flattenedObject" )
				.predicate().matchAll().end()
				.build();
	}

	@Test
	public void error_notStored() {
		Assume.assumeTrue( "Checks preventing from projecting on un-stored fields are not implemented yet", false );
		// TODO  Throw an exception when trying to project on an un-stored field
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document1Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document1Value.write( document ) );

			indexMapping.string1Field.document1Value.write( document );
			indexMapping.string2Field.document1Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document1Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document1Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document2Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document2Value.write( document ) );

			indexMapping.string1Field.document2Value.write( document );
			indexMapping.string2Field.document2Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document2Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document2Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.supportedFieldModels.forEach( f -> f.document3Value.write( document ) );
			indexMapping.supportedFieldWithProjectionConverterModels.forEach( f -> f.document3Value.write( document ) );

			indexMapping.string1Field.document3Value.write( document );
			indexMapping.string2Field.document3Value.write( document );

			// Note: this object must be single-valued for these tests
			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.supportedFieldModels.forEach( f -> f.document3Value.write( flattenedObject ) );

			// Note: this object must be single-valued for these tests
			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.supportedFieldModels.forEach( f -> f.document3Value.write( nestedObject ) );
		} );
		workPlan.add( referenceProvider( EMPTY ), document -> { } );

		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, EMPTY );
	}

	private static class IndexMapping {
		final List<FieldModel<?>> supportedFieldModels;
		final List<FieldModel<?>> supportedFieldWithProjectionConverterModels;

		final FieldModel<String> string1Field;
		final FieldModel<String> string2Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
			supportedFieldModels = mapSupportedFields( root, "", ignored -> { } );
			supportedFieldWithProjectionConverterModels = mapSupportedFields(
					root, "converted_", c -> c.projectionConverter( ValueWrapper.fromIndexFieldConverter() )
			);

			string1Field = FieldModel.mapper( String.class,"ccc", "mmm", "xxx" )
					.map( root, "string1" );
			string2Field = FieldModel.mapper( String.class,"ddd", "nnn", "yyy" )
					.map( root, "string2" );

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldAccessor self;
		final List<FieldModel<?>> supportedFieldModels;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.createAccessor();
			supportedFieldModels = mapSupportedFields( objectField, "", ignored -> { } );
		}
	}

	private static List<FieldModel<?>> mapSupportedFields(IndexSchemaElement root, String prefix,
			Consumer<StandardIndexSchemaFieldTypedContext<?, ?>> additionalConfiguration) {
		return Arrays.asList(
				FieldModel
						// Mix capitalized and non-capitalized text on purpose
						.mapper( String.class, "Aaron", "george", "Zach" )
						.map(
								root, prefix + "normalizedString",
								c -> {
									( (StringIndexSchemaFieldTypedContext<?>) c ).normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name );
									additionalConfiguration.accept( c );
								}
						),
				FieldModel.mapper( String.class, "aaron", "george", "zach" )
						.map( root, prefix + "nonAnalyzedString", additionalConfiguration ),
				FieldModel.mapper( Integer.class, 1, 3, 5 )
						.map( root, prefix + "integer", additionalConfiguration ),
				FieldModel.mapper(
						LocalDate.class,
						LocalDate.of( 2018, 2, 1 ),
						LocalDate.of( 2018, 3, 1 ),
						LocalDate.of( 2018, 4, 1 )
				)
						.map( root, prefix + "localDate", additionalConfiguration ),
				FieldModel.mapper(
						GeoPoint.class,
						new ImmutableGeoPoint( 40, 70 ),
						new ImmutableGeoPoint( 40, 75 ),
						new ImmutableGeoPoint( 40, 80 )
				)
						.map( root, prefix + "geoPoint", additionalConfiguration )
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

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return (parent, name, configuration) -> {
				StandardIndexSchemaFieldTypedContext<?, F> context = parent.field( name ).as( type );
				context.store( Store.YES );
				configuration.accept( context );
				IndexFieldAccessor<F> accessor = context.createAccessor();
				return new FieldModel<>(
						accessor, name, type,
						document1Value, document2Value, document3Value
				);
			};
		}

		final String relativeFieldName;
		final Class<F> type;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private FieldModel(IndexFieldAccessor<F> accessor, String relativeFieldName, Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.type = type;
			this.document1Value = new ValueModel<>( accessor, document1Value );
			this.document2Value = new ValueModel<>( accessor, document2Value );
			this.document3Value = new ValueModel<>( accessor, document3Value );
		}
	}
}
