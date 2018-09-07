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
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubTypeModel;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of index field accessors.
 * <p>
 * This does not check the effects of the model definition on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
public class IndexFieldAccessorIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexAccessors indexAccessors;
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	/**
	 * Test that field accessors do not throw any exception when calling write() with a non-null value.
	 */
	@Test
	public void field_write_nonNull() {
		executeAdd( "1", document -> {
			setNonNullValues( indexAccessors, document );
		} );
	}

	/**
	 * Test that field accessors do not throw any exception when calling write() with a null value.
	 */
	@Test
	public void field_write_null() {
		executeAdd( "1", document -> {
			setNullValues( indexAccessors, document );
		} );
	}

	/**
	 * Test that object field accessors do not throw any exception when calling add()
	 * or when using field accessors on added objects.
	 */
	@Test
	public void objectField_add() {
		executeAdd( "1", document -> {
			setNullValues( indexAccessors, document );

			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			setNonNullValues( indexAccessors.flattenedObject, flattenedObject );
			flattenedObject = indexAccessors.flattenedObject.self.add( document );
			setNullValues( indexAccessors.flattenedObject, flattenedObject );
			DocumentElement flattenedObjectSecondLevelObject =
					indexAccessors.flattenedObject.flattenedObject.self.add( flattenedObject );
			setNonNullValues( indexAccessors.flattenedObject.flattenedObject, flattenedObjectSecondLevelObject );
			flattenedObjectSecondLevelObject = indexAccessors.flattenedObject.nestedObject.self.add( flattenedObject );
			setNullValues( indexAccessors.flattenedObject.nestedObject, flattenedObjectSecondLevelObject );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			setNonNullValues( indexAccessors.nestedObject, nestedObject );
			nestedObject = indexAccessors.nestedObject.self.add( document );
			setNullValues( indexAccessors.nestedObject, nestedObject );
			DocumentElement nestedObjectSecondLevelObject =
					indexAccessors.nestedObject.flattenedObject.self.add( nestedObject );
			setNonNullValues( indexAccessors.nestedObject.flattenedObject, nestedObjectSecondLevelObject );
			nestedObjectSecondLevelObject = indexAccessors.nestedObject.nestedObject.self.add( nestedObject );
			setNullValues( indexAccessors.nestedObject.nestedObject, nestedObjectSecondLevelObject );
		} );
	}

	/**
	 * Test that object field accessors do not throw any exception when calling addMissing().
	 */
	@Test
	public void objectField_addMissing() {
		executeAdd( "1", document -> {
			setNullValues( indexAccessors, document );

			DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
			indexAccessors.flattenedObject.self.addMissing( document );
			indexAccessors.flattenedObject.flattenedObject.self.add( flattenedObject );
			indexAccessors.flattenedObject.flattenedObject.self.addMissing( flattenedObject );
			indexAccessors.flattenedObject.nestedObject.self.add( flattenedObject );
			indexAccessors.flattenedObject.nestedObject.self.addMissing( flattenedObject );

			DocumentElement nestedObject = indexAccessors.nestedObject.self.add( document );
			indexAccessors.nestedObject.self.addMissing( document );
			indexAccessors.nestedObject.flattenedObject.self.add( nestedObject );
			indexAccessors.nestedObject.flattenedObject.self.addMissing( nestedObject );
			indexAccessors.nestedObject.nestedObject.self.add( nestedObject );
			indexAccessors.nestedObject.nestedObject.self.addMissing( nestedObject );
		} );
	}

	/**
	 * Test that accessors to excluded fields can be called without any exception being thrown.
	 */
	@Test
	public void excludedFields_write() {
		executeAdd( "1", document -> {
			DocumentElement excludingObject = indexAccessors.excludingObject.self.add( document );
			setNonNullValues( indexAccessors.excludingObject, excludingObject );
			excludingObject = indexAccessors.excludingObject.self.add( document );
			setNullValues( indexAccessors.excludingObject, excludingObject );

			DocumentElement flattenedSecondLevelObject =
					indexAccessors.excludingObject.flattenedObject.self.add( excludingObject );
			setNonNullValues( indexAccessors.excludingObject.flattenedObject, flattenedSecondLevelObject );
			flattenedSecondLevelObject = indexAccessors.excludingObject.flattenedObject.self.add( excludingObject );
			setNullValues( indexAccessors.excludingObject.flattenedObject, flattenedSecondLevelObject );

			DocumentElement nestedSecondLevelObject = indexAccessors.excludingObject.nestedObject.self.add( excludingObject );
			setNullValues( indexAccessors.excludingObject.nestedObject, nestedSecondLevelObject );
			nestedSecondLevelObject = indexAccessors.excludingObject.nestedObject.self.add( excludingObject );
			setNullValues( indexAccessors.excludingObject.nestedObject, nestedSecondLevelObject );
		} );
	}

	@Test
	public void parentMismatch_flattenedObjectChild() {
		for ( IndexFieldAccessor<?> accessor : indexAccessors.flattenedObject.getFieldAccessors() ) {
			SubTest.expectException(
					"Parent mismatch with accessor " + accessor,
					() ->
							executeAdd( "1", document -> {
								accessor.write( document, null );
							} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid parent object for this field accessor" )
					.hasMessageContaining( "expected path 'flattenedObject'" )
					.hasMessageContaining( "got 'null'" );
		}
	}

	@Test
	public void parentMismatch_nestedObjectChild() {
		for ( IndexFieldAccessor<?> accessor : indexAccessors.nestedObject.getFieldAccessors() ) {
			SubTest.expectException(
					"Parent mismatch with accessor " + accessor,
					() ->
							executeAdd( "1", document -> {
								accessor.write( document, null );
							} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid parent object for this field accessor" )
					.hasMessageContaining( "expected path 'nestedObject'" )
					.hasMessageContaining( "got 'null'" );
		}
	}

	@Test
	public void parentMismatch_rootChild() {
		for ( IndexFieldAccessor<?> accessor : indexAccessors.getFieldAccessors() ) {
			SubTest.expectException(
					"Parent mismatch with accessor " + accessor,
					() ->
							executeAdd( "1", document -> {
								DocumentElement flattenedObject = indexAccessors.flattenedObject.self.add( document );
								accessor.write( flattenedObject, null );
							} )
			)
					.assertThrown()
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid parent object for this field accessor" )
					.hasMessageContaining( "expected path 'null'" )
					.hasMessageContaining( "got 'flattenedObject'" );
		}
	}

	private void setNonNullValues(AllTypesAccessors accessors, DocumentElement document) {
		accessors.string.write( document, "text 1" );
		accessors.string_analyzed.write( document, "text 1" );
		accessors.integer.write( document, 1 );
		accessors.localDate.write( document, LocalDate.of( 2018, 1, 1 ) );
		accessors.geoPoint.write( document, new ImmutableGeoPoint( 0, 1 ) );
	}

	private void setNullValues(AllTypesAccessors accessors, DocumentElement document) {
		accessors.string.write( document, null );
		accessors.string_analyzed.write( document, null );
		accessors.integer.write( document, null );
		accessors.localDate.write( document, null );
		accessors.geoPoint.write( document, null );
	}

	private void executeAdd(String id, Consumer<DocumentElement> documentContributor) {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
		workPlan.add( referenceProvider( id ), documentContributor::accept );
		workPlan.execute().join();
	}

	/**
	 * Defines one field for each field type.
	 */
	private static class AllTypesAccessors {
		final IndexFieldAccessor<String> string;
		final IndexFieldAccessor<String> string_analyzed;
		final IndexFieldAccessor<Integer> integer;
		final IndexFieldAccessor<LocalDate> localDate;
		final IndexFieldAccessor<GeoPoint> geoPoint;

		final List<IndexFieldAccessor<?>> fieldAccessors = new ArrayList<>();

		AllTypesAccessors(IndexSchemaElement root) {
			addFieldAccessors(
				string = root.field( "string" ).asString().createAccessor(),
				string_analyzed = root.field( "string_analyzed" ).asString()
						.analyzer( "default" )
						.createAccessor(),
				integer = root.field( "integer" ).asInteger().createAccessor(),
				localDate = root.field( "localDate" ).asLocalDate().createAccessor(),
				geoPoint = root.field( "geoPoint" ).asGeoPoint().createAccessor()
			);
		}

		protected void addFieldAccessors(IndexFieldAccessor<?> ... accessors) {
			Collections.addAll( fieldAccessors, accessors );
		}

		public List<IndexFieldAccessor<?>> getFieldAccessors() {
			return Collections.unmodifiableList( fieldAccessors );
		}
	}

	private static class IndexAccessors extends AllTypesAccessors {
		final FirstLevelObjectAccessors flattenedObject;
		final FirstLevelObjectAccessors nestedObject;
		final FirstLevelObjectAccessors excludingObject;

		IndexAccessors(IndexModelBindingContext ctx) {
			super( ctx.getSchemaElement() );
			IndexSchemaElement root = ctx.getSchemaElement();
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new FirstLevelObjectAccessors( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = root.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new FirstLevelObjectAccessors( nestedObjectField );

			// Simulate an embedded context which excludes every subfield
			IndexModelBindingContext excludingEmbeddedContext = ctx.addIndexedEmbeddedIfIncluded(
					new StubTypeModel( "embedded" ),
					"excludingObject.", ObjectFieldStorage.FLATTENED,
					null, Collections.singleton( "pathThatDoesNotMatchAnything" )
			).get();
			excludingObject = new FirstLevelObjectAccessors(
					excludingEmbeddedContext.getSchemaElement(),
					excludingEmbeddedContext.getParentIndexObjectAccessors().iterator().next()
			);
		}
	}

	private static class FirstLevelObjectAccessors extends AllTypesAccessors {
		final IndexObjectFieldAccessor self;

		final SecondLevelObjectAccessors flattenedObject;
		final SecondLevelObjectAccessors nestedObject;

		FirstLevelObjectAccessors(IndexSchemaObjectField objectField) {
			this( objectField, objectField.createAccessor() );
		}

		FirstLevelObjectAccessors(IndexSchemaElement objectField, IndexObjectFieldAccessor objectFieldAccessor) {
			super( objectField );
			self = objectFieldAccessor;
			IndexSchemaObjectField flattenedObjectField = objectField.objectField( "flattenedObject", ObjectFieldStorage.FLATTENED );
			flattenedObject = new SecondLevelObjectAccessors( flattenedObjectField );
			IndexSchemaObjectField nestedObjectField = objectField.objectField( "nestedObject", ObjectFieldStorage.NESTED );
			nestedObject = new SecondLevelObjectAccessors( nestedObjectField );
		}
	}

	private static class SecondLevelObjectAccessors extends AllTypesAccessors {
		final IndexObjectFieldAccessor self;

		SecondLevelObjectAccessors(IndexSchemaObjectField objectField) {
			super( objectField );
			self = objectField.createAccessor();
		}
	}
}
