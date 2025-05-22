/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class ObjectProjectionBaseIT {
	//CHECKSTYLE:ON

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( FromAsConfigured.index ).setup();

		BulkIndexer fromAsIndexer = FromAsConfigured.index.bulkIndexer();
		FromAsConfigured.dataSet.contribute( FromAsConfigured.index, fromAsIndexer );

		fromAsIndexer.join();
	}

	private static ObjectStructure requiredObjectStructure(boolean multivalued) {
		return multivalued
				&& TckConfiguration.get().getBackendFeatures().reliesOnNestedDocumentsForMultiValuedObjectProjection()
						? ObjectStructure.NESTED
						: ObjectStructure.DEFAULT;
	}

	@Nested
	class FromAsIT extends FromAsConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class FromAsConfigured extends AbstractCompositeProjectionFromAsIT<FromAsConfigured.IndexBinding> {

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "fromAs" );

		private static final DataSet dataSet = new DataSet();

		public FromAsConfigured() {
			super( index, dataSet );
		}

		@Override
		protected CompositeProjectionInnerStep startProjection(SearchProjectionFactory<?, ?> f) {
			return f.object( index.binding().objectField.relativeName );
		}

		@Override
		protected CompositeProjectionInnerStep startProjectionForMulti(SearchProjectionFactory<?, ?> f) {
			return f.object( index.binding().objectField_multi.relativeName );
		}

		// Just use fields at the root of the index
		public static class IndexBinding extends AbstractCompositeProjectionFromAsIT.AbstractIndexBinding {
			private final ObjectBinding objectField;
			private final ObjectBinding objectField_multi;

			IndexBinding(IndexSchemaElement parent) {
				objectField = ObjectBinding.create( parent, "objectField", false );
				objectField_multi = ObjectBinding.create( parent, "objectField_multi", true );
			}

			@Override
			CompositeBinding composite() {
				return objectField;
			}

			@Override
			CompositeBinding compositeForMulti() {
				return objectField_multi;
			}

			private static class ObjectBinding extends AbstractCompositeProjectionFromAsIT.CompositeBinding {
				public static ObjectBinding create(IndexSchemaElement parent, String relativeName,
						boolean multiValued) {
					IndexSchemaObjectField objectField =
							parent.objectField( relativeName, requiredObjectStructure( multiValued ) );
					if ( multiValued ) {
						objectField.multiValued();
					}
					return new ObjectBinding( objectField, relativeName );
				}

				private final String relativeName;
				private final IndexObjectFieldReference reference;

				private ObjectBinding(IndexSchemaObjectField self, String relativeName) {
					super( self, relativeName );
					this.relativeName = relativeName;
					this.reference = self.toReference();
				}
			}
		}

		public static class DataSet extends AbstractCompositeProjectionFromAsIT.AbstractDataSet<IndexBinding> {
			@Override
			void initDocument(IndexBinding binding, int docOrdinal, DocumentElement document) {
				DocumentElement object = document.addObject( binding.objectField.reference );
				object.addValue( binding.objectField.field1.reference, field1Value( docOrdinal, 0 ) );
				object.addValue( binding.objectField.field2.reference, field2Value( docOrdinal, 0 ) );
				object.addValue( binding.objectField.field3.reference, field3Value( docOrdinal, 0 ) );
				object.addValue( binding.objectField.field4.reference, field4Value( docOrdinal, 0 ) );

				object = document.addObject( binding.objectField_multi.reference );
				object.addValue( binding.objectField_multi.field1.reference, field1Value( docOrdinal, 0 ) );
				object.addValue( binding.objectField_multi.field2.reference, field2Value( docOrdinal, 0 ) );
				object.addValue( binding.objectField_multi.field3.reference, field3Value( docOrdinal, 0 ) );
				object.addValue( binding.objectField_multi.field4.reference, field4Value( docOrdinal, 0 ) );
				object = document.addObject( binding.objectField_multi.reference );
				object.addValue( binding.objectField_multi.field1.reference, field1Value( docOrdinal, 1 ) );
				object.addValue( binding.objectField_multi.field2.reference, field2Value( docOrdinal, 1 ) );
				object.addValue( binding.objectField_multi.field3.reference, field3Value( docOrdinal, 1 ) );
				object.addValue( binding.objectField_multi.field4.reference, field4Value( docOrdinal, 1 ) );
			}

			@Override
			<T> List<T> forEachObjectInDocument(IntFunction<T> function) {
				return Arrays.asList( function.apply( 0 ), function.apply( 1 ) );
			}
		}
	}

}
