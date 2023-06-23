/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.IntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests behavior of all single-field aggregations (range, terms, ...)
 * when invalid fields are targeted.
 */
@RunWith(Parameterized.class)
public class SingleFieldAggregationInvalidFieldIT<F> {

	private static final IntegerFieldTypeDescriptor FIELD_TYPE = IntegerFieldTypeDescriptor.INSTANCE;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
					aggregationDescriptor.getSingleFieldAggregationExpectations( FIELD_TYPE ).getSupported();
			parameters.add( new Object[] { expectations.get() } );
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

	private final SupportedSingleFieldAggregationExpectations<F> expectations;
	private final FieldTypeDescriptor<F> fieldType;

	public SingleFieldAggregationInvalidFieldIT(SupportedSingleFieldAggregationExpectations<F> expectations) {
		this.expectations = expectations;
		this.fieldType = expectations.fieldType();
	}

	@Test
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.FacetUnknownFieldFailureTest.testUnknownFieldNameThrowsException")
	public void unknownField() {
		String fieldPath = "unknownField";

		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( index.name() );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testNullFieldNameThrowsException")
	public void nullFieldPath() {
		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'fieldPath'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void objectField_nested() {
		String fieldPath = index.binding().nestedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( fieldType ) );

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'" );
	}

	@Test
	public void objectField_flattened() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( fieldType ) );

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'" );
	}

	private static class IndexBinding {
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}
}
