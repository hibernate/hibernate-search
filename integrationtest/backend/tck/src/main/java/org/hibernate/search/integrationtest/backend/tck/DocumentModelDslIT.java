/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectField;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of the document model definition DSL.
 * <p>
 * This does not check the effects of the model definition on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
public class DocumentModelDslIT {

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void nullFieldName() {
		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
					root.field( null );
					fail( "Expected null field name to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by null field name" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" );
		}

		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( null );
					fail( "Expected null object field name to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by null object field name" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" );
		}
	}

	@Test
	public void emptyFieldName() {
		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
				root.field( "" );
				fail( "Expected empty field name to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by empty field name" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" );
		}

		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
				root.objectField( "" );
				fail( "Expected empty object field name to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by empty object field name" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" );
		}
	}

	@Test
	public void dotInFieldName() {
		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
				root.field( "foo.bar" );
				fail( "Expected field name containing a dot to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by field name containing a dot" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
					.hasMessageContaining(" Remove the dot from your field name" )
					.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
							+ " declare an object field using the objectField() method." );
		}

		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
				root.objectField( "foo.bar" );
				fail( "Expected object field name containing a dot to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by object field name containing a dot" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
					.hasMessageContaining(" Remove the dot from your field name" )
					.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
							+ " declare an object field using the objectField() method." );
		}
	}

	@Test
	public void nameCollision_fields() {
		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
				root.field( "field1" );
				root.field( "field1" );
				fail( "Expected name collision to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by name collision between two fields" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "schema node 'field1' was added twice" )
					.hasMessageContaining( "at path 'null'" );
		}

		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
				IndexSchemaObjectField objectField1 = root.objectField( "object1" );
				IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
				objectField2.field( "field1" );
				objectField2.field( "field1" );
				fail( "Expected name collision to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by name collision between two fields" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "schema node 'field1' was added twice" )
					.hasMessageContaining( "at path 'object1.object2'" );
		}
	}

	@Test
	public void nameCollision_objectFields() {
		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "field1" );
					root.objectField( "field1" );
					fail( "Expected name collision to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by name collision between two fields" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "schema node 'field1' was added twice" )
					.hasMessageContaining( "at path 'null'" );
		}

		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.objectField( "field1" );
					objectField2.objectField( "field1" );
					fail( "Expected name collision to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by name collision between two fields" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "schema node 'field1' was added twice" )
					.hasMessageContaining( "at path 'object1.object2'" );
		}
	}

	@Test
	public void nameCollision_fieldAndObjectField() {
		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "field1" );
					root.objectField( "field1" );
					fail( "Expected name collision to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by name collision between two fields" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "schema node 'field1' was added twice" )
					.hasMessageContaining( "at path 'null'" );
		}

		try {
			setup( ctx -> {
				IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1" );
					objectField2.objectField( "field1" );
					fail( "Expected name collision to trigger an exception" );
			} );
		}
		catch (Exception e) {
			assertThat( e )
					.as( "Exception triggered by name collision between two fields" )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "schema node 'field1' was added twice" )
					.hasMessageContaining( "at path 'object1.object2'" );
		}
	}

	private void setup(Consumer<IndexModelBindingContext> mappingContributor) {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", "IndexName",
						mappingContributor,
						(indexManager, indexName) -> { }
				)
				.setup();
	}

}
