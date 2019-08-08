/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.UnsupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.singleinstance.BeforeAll;
import org.hibernate.search.util.impl.test.singleinstance.InstanceRule;
import org.hibernate.search.util.impl.test.singleinstance.SingleInstanceRunnerWithParameters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior common to all single-field aggregations (range, terms, ...)
 * on unsupported types.
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(SingleInstanceRunnerWithParameters.Factory.class)
public class SingleFieldAggregationUnsupportedTypesIT<F> {

	private static final String INDEX_NAME = "IndexName";

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] aggregationTypeCombinations() {
		List<Object[]> combinations = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
				Optional<? extends UnsupportedSingleFieldAggregationExpectations> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getUnsupported();
				if ( expectations.isPresent() ) {
					combinations.add( new Object[] {
							aggregationDescriptor,
							fieldTypeDescriptor,
							expectations.get()
					} );
				}
			}
		}
		return combinations.toArray( new Object[0][] );
	}

	@InstanceRule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final FieldTypeDescriptor<F> typeDescriptor;

	private final UnsupportedSingleFieldAggregationExpectations expectations;

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	public SingleFieldAggregationUnsupportedTypesIT(AggregationDescriptor thisIsJustForTestName,
			FieldTypeDescriptor<F> typeDescriptor,
			UnsupportedSingleFieldAggregationExpectations expectations) {
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations;
	}

	@BeforeAll
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryWithUnsupportedType")
	public void simple() {
		FieldModel<F> model = indexMapping.fieldModel;
		String fieldPath = model.relativeFieldName;

		SubTest.expectException(
				() -> expectations.trySetup( indexManager.createScope().aggregation(), fieldPath )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				// Example: Numeric aggregations (range) are not supported by this field's type
				.hasMessageContaining( "aggregations" )
				.hasMessageContaining( "are not supported by this field's type" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	private FieldModel<F> mapField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return FieldModel.mapper( typeDescriptor )
				.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
	}

	private class IndexMapping {
		final FieldModel<F> fieldModel;

		IndexMapping(IndexSchemaElement root) {
			fieldModel = mapField(
					root, "",
					c -> { }
			);
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					FieldModel::new
			);
		}

		final IndexFieldReference<F> reference;
		final String relativeFieldName;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
		}
	}
}
