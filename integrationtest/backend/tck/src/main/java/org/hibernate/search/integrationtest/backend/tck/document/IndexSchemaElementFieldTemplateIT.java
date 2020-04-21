/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.document;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the behavior of implementations of {@link IndexSchemaElement} when defining field templates.
 * <p>
 * This does not check the effects of the definitions on the actual index schema,
 * since this would require backend-specific code to inspect that schema.
 * However, in search and projection tests, we check that defined fields behave correctly at runtime.
 */
public class IndexSchemaElementFieldTemplateIT {

	private static final String TYPE_NAME = "TypeName";
	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void nullName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.fieldTemplate( null, this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field template name 'null' is invalid: field template names cannot be null or empty" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" )
							.fieldTemplate( null, this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field template name 'null' is invalid: field template names cannot be null or empty" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectFieldTemplate( null );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field template name 'null' is invalid: field template names cannot be null or empty" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectFieldTemplate( null );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field template name 'null' is invalid: field template names cannot be null or empty" )
						.build() );
	}

	@Test
	public void emptyName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.fieldTemplate( "", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field template name '' is invalid: field template names cannot be null or empty" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).fieldTemplate( "", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field template name '' is invalid: field template names cannot be null or empty" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectFieldTemplate( "" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "Field template name '' is invalid: field template names cannot be null or empty" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectFieldTemplate( "" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure( "Field template name '' is invalid: field template names cannot be null or empty" )
						.build() );
	}

	@Test
	public void dotInName() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.fieldTemplate( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure(
								"Field template name 'foo.bar' is invalid: field template names cannot contain a dot ('.')."
						)
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).fieldTemplate( "foo.bar", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Field template name 'foo.bar' is invalid: field template names cannot contain a dot ('.')."
						)
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectFieldTemplate( "foo.bar" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure(
								"Field template name 'foo.bar' is invalid: field template names cannot contain a dot ('.')."
						)
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectField( "nonRoot" ).objectFieldTemplate( "foo.bar" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "nonRoot" )
						.failure(
								"Field template name 'foo.bar' is invalid: field template names cannot contain a dot ('.')."
						)
						.build() );
	}

	@Test
	public void nameCollision_fieldTemplates() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.fieldTemplate( "field1", this::irrelevantTypeContributor );
					root.fieldTemplate( "field1", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "field template 'field1' was added twice" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.fieldTemplate( "field1", this::irrelevantTypeContributor );
					objectField2.fieldTemplate( "field1", this::irrelevantTypeContributor );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "object1.object2" )
						.failure( "field template 'field1' was added twice" )
						.build() );
	}

	@Test
	public void nameCollision_objectFieldTemplates() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.objectFieldTemplate( "field1" );
					root.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "field template 'field1' was added twice" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.objectFieldTemplate( "field1" );
					objectField2.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "object1.object2" )
						.failure( "field template 'field1' was added twice" )
						.build() );
	}

	@Test
	public void nameCollision_fieldTemplateAndObjectFieldTemplate() {
		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.fieldTemplate( "field1", f -> f.asString() );
					root.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexSchemaRootContext()
						.failure( "field template 'field1' was added twice" )
						.build() );

		assertThatThrownBy(
				() -> setup( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField1 = root.objectField( "object1" );
					IndexSchemaObjectField objectField2 = objectField1.objectField( "object2" );
					objectField2.fieldTemplate( "field1", f -> f.asString() );
					objectField2.objectFieldTemplate( "field1" );
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( TYPE_NAME )
						.indexContext( INDEX_NAME )
						.indexFieldContext( "object1.object2" )
						.failure( "field template 'field1' was added twice" )
						.build() );
	}

	private IndexFieldTypeFinalStep<String> irrelevantTypeContributor(IndexFieldTypeFactory factoryContext) {
		return factoryContext.asString();
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						b -> b.mappedType( TYPE_NAME ),
						mappingContributor,
						ignored -> { }
				)
				.setup();
	}

}
