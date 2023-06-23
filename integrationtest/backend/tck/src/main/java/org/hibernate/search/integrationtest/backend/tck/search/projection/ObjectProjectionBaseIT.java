/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NestedRunner.class)
public class ObjectProjectionBaseIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( FromAsIT.index ).setup();

		BulkIndexer fromAsIndexer = FromAsIT.index.bulkIndexer();
		FromAsIT.dataSet.contribute( FromAsIT.index, fromAsIndexer );

		fromAsIndexer.join();
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	private static ObjectStructure requiredObjectStructure(boolean multivalued) {
		return multivalued
				&& TckConfiguration.get().getBackendFeatures().reliesOnNestedDocumentsForMultiValuedObjectProjection()
						? ObjectStructure.NESTED
						: ObjectStructure.DEFAULT;
	}

	@Nested
	public static class FromAsIT extends AbstractCompositeProjectionFromAsIT<FromAsIT.IndexBinding> {

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "fromAs" );

		private static final DataSet dataSet = new DataSet();

		public FromAsIT() {
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
