/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubTypeModel;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of index field references.
 * <p>
 * This does not check the effects of the model definition on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
public class IndexFieldReferenceIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	/**
	 * Test that field references do not throw any exception when calling write() with a non-null value.
	 */
	@Test
	public void field_write_nonNull() {
		executeAdd( "1", document -> {
			setNonNullValues( indexMapping, document );
		} );
	}

	/**
	 * Test that field references do not throw any exception when calling write() with a null value.
	 */
	@Test
	public void field_write_null() {
		executeAdd( "1", document -> {
			setNullValues( indexMapping, document );
		} );
	}

	/**
	 * Test that object field references do not throw any exception when calling add()
	 * or when using field references on added objects.
	 */
	@Test
	public void objectField_add() {
		executeAdd( "1", document -> {
			setNullValues( indexMapping, document );

			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			setNonNullValues( indexMapping.flattenedObject, flattenedObject );
			flattenedObject = indexMapping.flattenedObject.self.add( document );
			setNullValues( indexMapping.flattenedObject, flattenedObject );
			DocumentElement flattenedObjectSecondLevelObject =
					indexMapping.flattenedObject.flattenedObject.self.add( flattenedObject );
			setNonNullValues( indexMapping.flattenedObject.flattenedObject, flattenedObjectSecondLevelObject );
			flattenedObjectSecondLevelObject = indexMapping.flattenedObject.nestedObject.self.add( flattenedObject );
			setNullValues( indexMapping.flattenedObject.nestedObject, flattenedObjectSecondLevelObject );

			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			setNonNullValues( indexMapping.nestedObject, nestedObject );
			nestedObject = indexMapping.nestedObject.self.add( document );
			setNullValues( indexMapping.nestedObject, nestedObject );
			DocumentElement nestedObjectSecondLevelObject =
					indexMapping.nestedObject.flattenedObject.self.add( nestedObject );
			setNonNullValues( indexMapping.nestedObject.flattenedObject, nestedObjectSecondLevelObject );
			nestedObjectSecondLevelObject = indexMapping.nestedObject.nestedObject.self.add( nestedObject );
			setNullValues( indexMapping.nestedObject.nestedObject, nestedObjectSecondLevelObject );
		} );
	}

	/**
	 * Test that object field references do not throw any exception when calling addMissing().
	 */
	@Test
	public void objectField_addMissing() {
		executeAdd( "1", document -> {
			setNullValues( indexMapping, document );

			DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
			indexMapping.flattenedObject.self.addMissing( document );
			indexMapping.flattenedObject.flattenedObject.self.add( flattenedObject );
			indexMapping.flattenedObject.flattenedObject.self.addMissing( flattenedObject );
			indexMapping.flattenedObject.nestedObject.self.add( flattenedObject );
			indexMapping.flattenedObject.nestedObject.self.addMissing( flattenedObject );

			DocumentElement nestedObject = indexMapping.nestedObject.self.add( document );
			indexMapping.nestedObject.self.addMissing( document );
			indexMapping.nestedObject.flattenedObject.self.add( nestedObject );
			indexMapping.nestedObject.flattenedObject.self.addMissing( nestedObject );
			indexMapping.nestedObject.nestedObject.self.add( nestedObject );
			indexMapping.nestedObject.nestedObject.self.addMissing( nestedObject );
		} );
	}

	/**
	 * Test that references to excluded fields can be called without any exception being thrown.
	 */
	@Test
	public void excludedFields_write() {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = indexMapping.excludingObject.self.add( document );
			setNonNullValues( indexMapping.excludingObject, excludingObject );
			excludingObject = indexMapping.excludingObject.self.add( document );
			setNullValues( indexMapping.excludingObject, excludingObject );

			DocumentElement flattenedSecondLevelObject =
					indexMapping.excludingObject.flattenedObject.self.add( excludingObject );
			setNonNullValues( indexMapping.excludingObject.flattenedObject, flattenedSecondLevelObject );
			flattenedSecondLevelObject = indexMapping.excludingObject.flattenedObject.self.add( excludingObject );
			setNullValues( indexMapping.excludingObject.flattenedObject, flattenedSecondLevelObject );

			DocumentElement nestedSecondLevelObject = indexMapping.excludingObject.nestedObject.self.add( excludingObject );
			setNullValues( indexMapping.excludingObject.nestedObject, nestedSecondLevelObject );
			nestedSecondLevelObject = indexMapping.excludingObject.nestedObject.self.add( excludingObject );
			setNullValues( indexMapping.excludingObject.nestedObject, nestedSecondLevelObject );
		} );
	}

	@Test
	public void invalidFieldForDocumentElement_flattenedObjectChild() {
		for ( IndexFieldReference<?> reference : indexMapping.flattenedObject.getFieldReferences() ) {
			SubTest.expectException(
					"Parent mismatch with reference " + reference,
					() ->
							executeAdd( "1", document -> {
								reference.write( document, null );
							} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid field reference for this document element" )
					.hasMessageContaining( "this document element has path 'flattenedObject'" )
					.hasMessageContaining( "but the referenced field has a parent with path 'null'" );
		}
	}

	@Test
	public void invalidFieldForDocumentElement_nestedObjectChild() {
		for ( IndexFieldReference<?> reference : indexMapping.nestedObject.getFieldReferences() ) {
			SubTest.expectException(
					"Parent mismatch with reference " + reference,
					() ->
							executeAdd( "1", document -> {
								reference.write( document, null );
							} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid field reference for this document element" )
					.hasMessageContaining( "this document element has path 'nestedObject'" )
					.hasMessageContaining( "but the referenced field has a parent with path 'null'" );
		}
	}

	@Test
	public void invalidFieldForDocumentElement_rootChild() {
		for ( IndexFieldReference<?> reference : indexMapping.getFieldReferences() ) {
			SubTest.expectException(
					"Parent mismatch with reference " + reference,
					() ->
							executeAdd( "1", document -> {
								DocumentElement flattenedObject = indexMapping.flattenedObject.self.add( document );
								reference.write( flattenedObject, null );
							} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid field reference for this document element" )
					.hasMessageContaining( "this document element has path 'null'" )
					.hasMessageContaining( "but the referenced field has a parent with path 'flattenedObject'" );
		}
	}

	private void setNonNullValues(AllTypesMapping mapping, DocumentElement document) {
		mapping.string.write( document, "text 1" );
		mapping.string_analyzed.write( document, "text 1" );
		mapping.integer.write( document, 1 );
		mapping.localDate.write( document, LocalDate.of( 2018, 1, 1 ) );
		mapping.geoPoint.write( document, GeoPoint.of( 0, 1 ) );
	}

	private void setNullValues(AllTypesMapping mapping, DocumentElement document) {
		mapping.string.write( document, null );
		mapping.string_analyzed.write( document, null );
		mapping.integer.write( document, null );
		mapping.localDate.write( document, null );
		mapping.geoPoint.write( document, null );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( id ), documentContributor::accept );
		workPlan.execute().join();
	}

	/**
	 * Defines one field for each field type.
	 */
	private static class AllTypesMapping {
		final IndexFieldReference<String> string;
		final IndexFieldReference<String> string_analyzed;
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<LocalDate> localDate;
		final IndexFieldReference<GeoPoint> geoPoint;

		final List<IndexFieldReference<?>> fieldReferences = new ArrayList<>();

		AllTypesMapping(IndexSchemaElement root) {
			addFieldReferences(
				string = root.field( "string", f -> f.asString() ).toReference(),
				string_analyzed = root.field(
						"string_analyzed" ,
						f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name )
				)
						.toReference(),
				integer = root.field( "integer", f -> f.asInteger() ).toReference(),
				localDate = root.field( "localDate", f -> f.asLocalDate() ).toReference(),
				geoPoint = root.field( "geoPoint", f -> f.asGeoPoint() ).toReference()
			);
		}

		protected void addFieldReferences(IndexFieldReference<?>... references) {
			Collections.addAll( fieldReferences, references );
		}

		public List<IndexFieldReference<?>> getFieldReferences() {
			return Collections.unmodifiableList( fieldReferences );
		}
	}

	private static class IndexMapping extends AllTypesMapping {
		final FirstLevelObjectMapping flattenedObject;
		final FirstLevelObjectMapping nestedObject;
		final FirstLevelObjectMapping excludingObject;

		IndexMapping(IndexModelBindingContext ctx) {
			super( ctx.getSchemaElement() );
			IndexSchemaElement root = ctx.getSchemaElement();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new FirstLevelObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new FirstLevelObjectMapping( nestedObjectField );

			// Simulate an embedded context which excludes every subfield
			IndexModelBindingContext excludingEmbeddedContext = ctx.addIndexedEmbeddedIfIncluded(
					new StubTypeModel( "embedded" ),
					"excludingObject.", ObjectFieldStorage.FLATTENED,
					null, Collections.singleton( "pathThatDoesNotMatchAnything" )
			).get();
			excludingObject = new FirstLevelObjectMapping(
					excludingEmbeddedContext.getSchemaElement(),
					excludingEmbeddedContext.getParentIndexObjectReferences().iterator().next()
			);
		}
	}

	private static class FirstLevelObjectMapping extends AllTypesMapping {
		final IndexObjectFieldReference self;

		final SecondLevelObjectMapping flattenedObject;
		final SecondLevelObjectMapping nestedObject;

		FirstLevelObjectMapping(IndexSchemaObjectField objectField) {
			this( objectField, objectField.toReference() );
		}

		FirstLevelObjectMapping(IndexSchemaElement objectField, IndexObjectFieldReference objectFieldReference) {
			super( objectField );
			self = objectFieldReference;
			IndexSchemaObjectField flattenedObjectField = objectField.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new SecondLevelObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = objectField.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new SecondLevelObjectMapping( nestedObjectField );
		}
	}

	private static class SecondLevelObjectMapping extends AllTypesMapping {
		final IndexObjectFieldReference self;

		SecondLevelObjectMapping(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.toReference();
		}
	}
}
