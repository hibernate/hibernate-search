/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.UnsupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior common to all single-field aggregations (range, terms, ...)
 * on unsupported types.
 */
@RunWith(Parameterized.class)
public class SingleFieldAggregationUnsupportedTypesIT<F> {

	private static Set<FieldTypeDescriptor<?>> unsupportedFieldTypes;

	@Parameterized.Parameters(name = "{1}")
	public static Object[][] parameters() {
		unsupportedFieldTypes = new LinkedHashSet<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
				Optional<? extends UnsupportedSingleFieldAggregationExpectations> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getUnsupported();
				if ( expectations.isPresent() ) {
					unsupportedFieldTypes.add( fieldTypeDescriptor );
					parameters.add( new Object[] { fieldTypeDescriptor, expectations.get() } );
				}
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	private final FieldTypeDescriptor<F> fieldType;
	private final UnsupportedSingleFieldAggregationExpectations expectations;

	public SingleFieldAggregationUnsupportedTypesIT(FieldTypeDescriptor<F> fieldType,
			UnsupportedSingleFieldAggregationExpectations expectations) {
		this.fieldType = fieldType;
		this.expectations = expectations;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1748")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryWithUnsupportedType")
	public void simple() {
		SimpleFieldModel<F> model = index.binding().fieldModels.get( fieldType );
		String fieldPath = model.relativeFieldName;

		assertThatThrownBy(
				() -> expectations.trySetup( index.createScope().aggregation(), fieldPath )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'",
						"'aggregation:" + expectations.aggregationName()
								+ "' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	private SimpleFieldModel<F> mapField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return SimpleFieldModel.mapper( fieldType, additionalConfiguration )
				.map( parent, prefix + fieldType.getUniqueName() );
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( unsupportedFieldTypes, root, "" );
		}
	}
}
