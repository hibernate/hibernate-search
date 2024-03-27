/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class CompositeProjectionBaseIT {
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

	/**
	 * Tests composite projections created through the multi-step DSL,
	 * e.g. {@code f.composite().from( otherProjection1, otherProjection2 ).as( MyPair::new ) },
	 * as opposed to the single-step DSL,
	 * e.g. {@code f.composite( MyPair::new, otherProjection1, otherProjection2 ) },
	 * which is tested in {@link CompositeProjectionSingleStepIT}.
	 */
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
