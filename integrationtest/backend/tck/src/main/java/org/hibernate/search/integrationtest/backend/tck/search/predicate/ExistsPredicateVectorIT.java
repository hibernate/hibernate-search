/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.VectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExistsPredicateVectorIT {

	private static final List<VectorFieldTypeDescriptor<?>> supportedFieldTypes = VectorFieldTypeDescriptor.getAll();

	@RegisterExtension
	public SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index =
			SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
					.name( "singleField" );

	public static List<? extends Arguments> params() {
		return supportedFieldTypes.stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@BeforeEach
	void before() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearch(),
				"This test only makes sense for backends that support vector search."
		);
	}

	@ParameterizedTest
	@MethodSource("params")
	void exists(VectorFieldTypeDescriptor<?> descriptor) {
		setupHelper.start().withIndexes( index ).setup();

		BulkIndexer bulkIndexer = index.bulkIndexer();
		List<?> values = descriptor.getUniquelyMatchableValues();
		for ( int i = 0; i < values.size(); i++ ) {
			Object value = values.get( i );
			bulkIndexer.add( "id:" + i, descriptor.getUniqueName(), d -> d.addValue( descriptor.getUniqueName(), value ) );
		}
		for ( int i = 0; i < values.size(); i++ ) {
			bulkIndexer.add( "id:" + ( i + values.size() ), descriptor.getUniqueName(),
					d -> d.addValue( descriptor.getUniqueName(), null ) );
		}

		bulkIndexer.join();

		assertThatQuery( index.query()
				.where( f -> f.exists().field( descriptor.getUniqueName() ) )
				.routing( descriptor.getUniqueName() ) )
				.hasTotalHitCount( values.size() );
	}

	private static class IndexBinding {
		private final Map<VectorFieldTypeDescriptor<?>, IndexFieldReference<?>> fields = new HashMap<>();

		IndexBinding(IndexSchemaElement root, List<VectorFieldTypeDescriptor<?>> descriptors) {
			descriptors.forEach(
					descriptor -> fields.put(
							descriptor,
							root.field( descriptor.getUniqueName(), descriptor::configure ).toReference()
					) );
		}
	}
}
