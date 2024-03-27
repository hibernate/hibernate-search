/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ObjectProjectionSpecificsIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( index )
				.setup();
	}

	@Test
	void nullFieldPath() {
		assertThatThrownBy( () -> index.createScope().projection().object( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'objectFieldPath' must not be null" );
	}

	@Test
	void unknownFieldPath() {
		assertThatThrownBy( () -> index.createScope().projection().object( "unknownField" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unknown field 'unknownField'" );
	}

	@Test
	void trait() {
		assertThat( Arrays.asList( "level1", "flattenedLevel1", "level1.level2", "flattenedLevel1.level2" ) )
				.allSatisfy( fieldPath -> assertThat( index.toApi().descriptor().field( fieldPath ) )
						.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
								.as( "traits of field '" + fieldPath + "'" )
								.contains( "projection:object" ) ) );
	}

	@Test
	void trait_nonObjectFieldPath() {
		String fieldPath = "level1.field1";
		assertThat( index.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( "projection:object" ) );
	}

	@Test
	void use_nonObjectFieldPath() {
		assertThatThrownBy( () -> index.createScope().projection().object( "level1.field1" ) )
				.hasMessageContainingAll( "Cannot use 'projection:object' on field 'level1.field1'" );
	}

	@Test
	void innerObjectProjectionOnFieldOutsideOuterObjectProjectionFieldTree() {
		assertThatThrownBy( () -> index.query()
				.select( f -> f.object( "level1.level2" )
						.from(
								// This is incorrect: the inner projection is executed for each object in field "level1",
								// which won't be present in "level1.level2".
								f.object( "level1" )
										.from( f.field( "level1.field1" ) )
										.asList()
										.multi()
						)
						.asList()
						.multi() )
				.where( f -> f.matchAll() )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid context for projection on field 'level1'",
						"the surrounding projection is executed for each object in field 'level1.level2',"
								+ " which is not a parent of field 'level1'",
						"Check the structure of your projections"
				);
	}

	@Test
	void multiValuedObjectField_flattened_unsupported() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().reliesOnNestedDocumentsForMultiValuedObjectProjection(),
				"This test is only relevant if the backend relies on nested documents to implement object projections on multi-valued fields"
		);
		SearchProjectionFactory<?, ?> f = index.createScope().projection();
		assertThatThrownBy( () -> f.object( "flattenedLevel1" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot use 'projection:object' on field 'flattenedLevel1'",
						"This multi-valued field has a 'FLATTENED' structure,"
								+ " which means the structure of objects is not preserved upon indexing,"
								+ " making object projections impossible",
						"Try setting the field structure to 'NESTED' and reindexing all your data" );
	}

	@Test
	void multiValuedObjectField_singleValuedObjectProjection() {
		SearchProjectionFactory<?, ?> f = index.createScope().projection();
		assertThatThrownBy( () -> f.object( "level1" )
				.from( f.field( "level1.field1" ) )
				.asList()
				// A call to .multi() is missing here
				.toProjection()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid cardinality for projection on field 'level1'",
						"the projection is single-valued, but this field is multi-valued",
						"Make sure to call '.multi()' when you create the projection"
				);
	}

	@Test
	void singleValuedObjectField_effectivelyMultiValuedInContext() {
		assertThatThrownBy( () -> index.query()
				.select( f -> f.object( "level1WithSingleValuedLevel2.level2" )
						.from( f.field( "level1WithSingleValuedLevel2.level2.field1" ) )
						.asList()
				// A call to .multi() is missing here
				)
				.where( f -> f.matchAll() )
				.toQuery() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid cardinality for projection on field 'level1WithSingleValuedLevel2.level2'",
						"the projection is single-valued, but this field is effectively multi-valued in this context",
						"because parent object field 'level1WithSingleValuedLevel2' is multi-valued",
						"call '.multi()' when you create the projection on field 'level1WithSingleValuedLevel2.level2'",
						"or wrap that projection in an object projection like this:"
								+ " 'f.object(\"level1WithSingleValuedLevel2\").from(<the projection on field level1WithSingleValuedLevel2.level2>).as(...).multi()'."
				);
	}

	private static class IndexBinding {
		final Level1ObjectBinding level1;
		final Level1ObjectBinding flattenedLevel1;
		final Level1ObjectBindingWithSingleValuedLevel2 level1WithSingleValuedLevel2;

		IndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObjectField = root.objectField( "level1", ObjectStructure.NESTED )
					.multiValued();
			level1 = new Level1ObjectBinding( nestedObjectField );
			IndexSchemaObjectField flattenedObjectField = root.objectField( "flattenedLevel1", ObjectStructure.FLATTENED )
					.multiValued();
			flattenedLevel1 = new Level1ObjectBinding( flattenedObjectField );
			IndexSchemaObjectField otherNestedObjectField =
					root.objectField( "level1WithSingleValuedLevel2", ObjectStructure.NESTED )
							.multiValued();
			level1WithSingleValuedLevel2 = new Level1ObjectBindingWithSingleValuedLevel2( otherNestedObjectField );
		}
	}

	private static class ObjectBinding {
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> field1;
		final IndexFieldReference<String> field2;

		ObjectBinding(IndexSchemaObjectField objectField) {
			self = objectField.toReference();
			field1 = objectField.field( "field1", f -> f.asString().projectable( Projectable.YES ) )
					.toReference();
			field2 = objectField.field( "field2", f -> f.asString().projectable( Projectable.YES ) )
					.multiValued().toReference();
		}
	}

	private static class Level1ObjectBinding extends ObjectBinding {
		final ObjectBinding level2;

		Level1ObjectBinding(IndexSchemaObjectField objectField) {
			super( objectField );
			IndexSchemaObjectField nestedObjectField = objectField.objectField( "level2", ObjectStructure.NESTED )
					.multiValued();
			level2 = new ObjectBinding( nestedObjectField );
		}
	}

	private static class Level1ObjectBindingWithSingleValuedLevel2 extends ObjectBinding {
		final ObjectBinding level2;

		Level1ObjectBindingWithSingleValuedLevel2(IndexSchemaObjectField objectField) {
			super( objectField );
			IndexSchemaObjectField nestedObjectField = objectField.objectField( "level2", ObjectStructure.NESTED );
			level2 = new ObjectBinding( nestedObjectField );
		}
	}
}
