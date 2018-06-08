/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.test.SubTest;

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
		SubTest.expectException(
				"Null field name",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( null );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" );

		SubTest.expectException(
				"Null object field name",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( null );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'null' is invalid: field names cannot be null or empty" );
	}

	@Test
	public void emptyFieldName() {
		SubTest.expectException(
				"empty field name",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" );

		SubTest.expectException(
				"empty object field name",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name '' is invalid: field names cannot be null or empty" );
	}

	@Test
	public void dotInFieldName() {
		SubTest.expectException(
				"field name containing a dot",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "foo.bar" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
				.hasMessageContaining( " Remove the dot from your field name" )
				.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
						+ " declare an object field using the objectField() method." );

		SubTest.expectException(
				"object field name containing a dot",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "foo.bar" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Field name 'foo.bar' is invalid: field names cannot contain a dot ('.')." )
				.hasMessageContaining( " Remove the dot from your field name" )
				.hasMessageContaining( "if you are declaring the field in a bridge and want a tree of fields,"
						+ " declare an object field using the objectField() method." );
	}

	@Test
	public void nameCollision_fields() {
		SubTest.expectException(
				"Name collision between two fields",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "field1" );
					root.field( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.hasMessageContaining( "at path 'null'" );

		SubTest.expectException(
				"Name collision between two fields",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1" );
					objectField2.field( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.hasMessageContaining( "at path 'object1.object2'" );
	}

	@Test
	public void nameCollision_objectFields() {
		SubTest.expectException(
				"Name collision between two fields",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "field1" );
					root.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.hasMessageContaining( "at path 'null'" );

		SubTest.expectException(
				"Name collision between two fields",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.objectField( "field1" );
					objectField2.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.hasMessageContaining( "at path 'object1.object2'" );
	}

	@Test
	public void nameCollision_fieldAndObjectField() {
		SubTest.expectException(
				"Name collision between two fields",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "field1" );
					root.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.hasMessageContaining( "at path 'null'" );

		SubTest.expectException(
				"Name collision between two fields",
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.field( "field1" );
					objectField2.objectField( "field1" );
				} )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "schema node 'field1' was added twice" )
				.hasMessageContaining( "at path 'object1.object2'" );
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
