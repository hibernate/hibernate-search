/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
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
public class CompositeProjectionBaseIT {

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

	/**
	 * Tests composite projections created through the multi-step DSL,
	 * e.g. {@code f.composite().from( otherProjection1, otherProjection2 ).as( MyPair::new ) },
	 * as opposed to the single-step DSL,
	 * e.g. {@code f.composite( MyPair::new, otherProjection1, otherProjection2 ) },
	 * which is tested in {@link CompositeProjectionSingleStepIT}.
	 */
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
			return f.composite();
		}

		@Override
		protected CompositeProjectionInnerStep startProjectionForMulti(SearchProjectionFactory<?, ?> f) {
			return f.composite();
		}

		// Just use fields at the root of the index
		public static class IndexBinding extends AbstractCompositeProjectionFromAsIT.AbstractIndexBinding {
			private final CompositeBinding delegate;

			IndexBinding(IndexSchemaElement parent) {
				delegate = new CompositeBinding( parent, null );
			}

			@Override
			CompositeBinding composite() {
				return delegate;
			}

			@Override
			CompositeBinding compositeForMulti() {
				return delegate;
			}
		}

		public static class DataSet extends AbstractCompositeProjectionFromAsIT.AbstractDataSet<IndexBinding> {
			@Override
			void initDocument(IndexBinding binding, int docOrdinal, DocumentElement document) {
				document.addValue( binding.delegate.field1.reference, field1Value( docOrdinal ) );
				document.addValue( binding.delegate.field2.reference, field2Value( docOrdinal ) );
				document.addValue( binding.delegate.field3.reference, field3Value( docOrdinal ) );
				document.addValue( binding.delegate.field4.reference, field4Value( docOrdinal ) );
			}

			@Override
			<T> List<T> forEachObjectInDocument(IntFunction<T> function) {
				return Collections.singletonList( function.apply( 0 ) );
			}
		}
	}

}
