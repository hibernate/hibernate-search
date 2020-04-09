/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

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
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubTypeModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the basic behavior of implementations of {@link DocumentElement}.
 */
public class DocumentElementBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a non-null value.
	 */
	@Test
	public void addValue_nonNull() {
		executeAdd( "1", document -> {
			setNonNullValues( indexMapping, document );
		} );
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a null value.
	 */
	@Test
	public void addValue_null() {
		executeAdd( "1", document -> {
			setNullValues( indexMapping, document );
		} );
	}

	/**
	 * Test that DocumentElement.addObject does not throw any exception,
	 * add that DocumentElement.addValue does not throw an exception for returned objects.
	 */
	@Test
	public void addObject() {
		executeAdd( "1", document -> {
			setNullValues( indexMapping, document );

			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			setNonNullValues( indexMapping.flattenedObject, flattenedObject );
			flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			setNullValues( indexMapping.flattenedObject, flattenedObject );
			DocumentElement flattenedObjectSecondLevelObject =
					flattenedObject.addObject( indexMapping.flattenedObject.flattenedObject.self );
			setNonNullValues( indexMapping.flattenedObject.flattenedObject, flattenedObjectSecondLevelObject );
			flattenedObjectSecondLevelObject = flattenedObject.addObject( indexMapping.flattenedObject.nestedObject.self );
			setNullValues( indexMapping.flattenedObject.nestedObject, flattenedObjectSecondLevelObject );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			setNonNullValues( indexMapping.nestedObject, nestedObject );
			nestedObject = document.addObject( indexMapping.nestedObject.self );
			setNullValues( indexMapping.nestedObject, nestedObject );
			DocumentElement nestedObjectSecondLevelObject =
					nestedObject.addObject( indexMapping.nestedObject.flattenedObject.self );
			setNonNullValues( indexMapping.nestedObject.flattenedObject, nestedObjectSecondLevelObject );
			nestedObjectSecondLevelObject = nestedObject.addObject( indexMapping.nestedObject.nestedObject.self );
			setNullValues( indexMapping.nestedObject.nestedObject, nestedObjectSecondLevelObject );
		} );
	}

	/**
	 * Test that DocumentElement.addNullObject does not throw any exception.
	 */
	@Test
	public void addNullObject() {
		executeAdd( "1", document -> {
			setNullValues( indexMapping, document );

			DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
			document.addNullObject( indexMapping.flattenedObject.self );
			flattenedObject.addObject( indexMapping.flattenedObject.flattenedObject.self );
			flattenedObject.addNullObject( indexMapping.flattenedObject.flattenedObject.self );
			flattenedObject.addObject( indexMapping.flattenedObject.nestedObject.self );
			flattenedObject.addNullObject( indexMapping.flattenedObject.nestedObject.self );

			DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
			document.addNullObject( indexMapping.nestedObject.self );
			nestedObject.addObject( indexMapping.nestedObject.flattenedObject.self );
			nestedObject.addNullObject( indexMapping.nestedObject.flattenedObject.self );
			nestedObject.addObject( indexMapping.nestedObject.nestedObject.self );
			nestedObject.addNullObject( indexMapping.nestedObject.nestedObject.self );
		} );
	}

	/**
	 * Test that DocumentElement.addValue does not throw any exception when passing a reference to an excluded field.
	 */
	@Test
	public void addValue_excludedFields() {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = document.addObject( indexMapping.excludingObject.self );
			setNonNullValues( indexMapping.excludingObject, excludingObject );
			excludingObject = document.addObject( indexMapping.excludingObject.self );
			setNullValues( indexMapping.excludingObject, excludingObject );

			DocumentElement flattenedSecondLevelObject =
					excludingObject.addObject( indexMapping.excludingObject.flattenedObject.self );
			setNonNullValues( indexMapping.excludingObject.flattenedObject, flattenedSecondLevelObject );
			flattenedSecondLevelObject = excludingObject.addObject( indexMapping.excludingObject.flattenedObject.self );
			setNullValues( indexMapping.excludingObject.flattenedObject, flattenedSecondLevelObject );

			DocumentElement nestedSecondLevelObject = excludingObject.addObject( indexMapping.excludingObject.nestedObject.self );
			setNullValues( indexMapping.excludingObject.nestedObject, nestedSecondLevelObject );
			nestedSecondLevelObject = excludingObject.addObject( indexMapping.excludingObject.nestedObject.self );
			setNullValues( indexMapping.excludingObject.nestedObject, nestedSecondLevelObject );
		} );
	}

	@Test
	public void invalidFieldForDocumentElement_flattenedObjectChild() {
		for ( IndexFieldReference<?> reference : indexMapping.flattenedObject.getFieldReferences() ) {
			assertThatThrownBy(
					() -> executeAdd( "1", document -> {
						document.addValue( reference, null );
					} ),
					"Parent mismatch with reference " + reference
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid field reference for this document element" )
					.hasMessageContaining( "this document element has path 'flattenedObject'" )
					.hasMessageContaining( "but the referenced field has a parent with path 'null'" );
		}
	}

	@Test
	public void invalidFieldForDocumentElement_nestedObjectChild() {
		for ( IndexFieldReference<?> reference : indexMapping.nestedObject.getFieldReferences() ) {
			assertThatThrownBy(
					() -> executeAdd( "1", document -> {
						document.addValue( reference, null );
					} ),
					"Parent mismatch with reference " + reference
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid field reference for this document element" )
					.hasMessageContaining( "this document element has path 'nestedObject'" )
					.hasMessageContaining( "but the referenced field has a parent with path 'null'" );
		}
	}

	@Test
	public void invalidFieldForDocumentElement_rootChild() {
		for ( IndexFieldReference<?> reference : indexMapping.getFieldReferences() ) {
			assertThatThrownBy(
					() -> executeAdd( "1", document -> {
						DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
						flattenedObject.addValue( reference, null );
					} ),
					"Parent mismatch with reference " + reference
			)
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid field reference for this document element" )
					.hasMessageContaining( "this document element has path 'null'" )
					.hasMessageContaining( "but the referenced field has a parent with path 'flattenedObject'" );
		}
	}

	private void setNonNullValues(AllTypesMapping mapping, DocumentElement document) {
		document.addValue( mapping.string, "text 1" );
		document.addValue( mapping.string_analyzed, "text 1" );
		document.addValue( mapping.integer, 1 );
		document.addValue( mapping.localDate, LocalDate.of( 2018, 1, 1 ) );
		document.addValue( mapping.geoPoint, GeoPoint.of( 0, 1 ) );
	}

	private void setNullValues(AllTypesMapping mapping, DocumentElement document) {
		document.addValue( mapping.string, null );
		document.addValue( mapping.string_analyzed, null );
		document.addValue( mapping.integer, null );
		document.addValue( mapping.localDate, null );
		document.addValue( mapping.geoPoint, null );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( id ), documentContributor::accept );
		plan.execute().join();
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
						f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
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

		IndexMapping(IndexBindingContext ctx) {
			super( ctx.getSchemaElement() );
			IndexSchemaElement root = ctx.getSchemaElement();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED )
					.multiValued();
			flattenedObject = new FirstLevelObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED )
					.multiValued();
			nestedObject = new FirstLevelObjectMapping( nestedObjectField );

			// Simulate an embedded context which excludes every subfield
			IndexedEmbeddedDefinition indexedEmbeddedDefinition = new IndexedEmbeddedDefinition(
					new StubTypeModel( "embedded" ),
					"excludingObject.", ObjectFieldStorage.FLATTENED,
					null, Collections.singleton( "pathThatDoesNotMatchAnything" )
			);
			IndexedEmbeddedBindingContext excludingEmbeddedContext =
					ctx.addIndexedEmbeddedIfIncluded( indexedEmbeddedDefinition, true ).get();
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
			IndexSchemaObjectField flattenedObjectField = objectField.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED )
					.multiValued();
			flattenedObject = new SecondLevelObjectMapping( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = objectField.objectField( "nestedObject", ObjectFieldStorage.NESTED )
					.multiValued();
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
