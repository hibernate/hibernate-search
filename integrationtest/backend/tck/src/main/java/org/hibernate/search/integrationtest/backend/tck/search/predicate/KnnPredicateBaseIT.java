/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.ByteVectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FloatVectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.VectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

class KnnPredicateBaseIT {

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
	void setUp() {
		setupHelper.start().withIndexes( index ).setup();

		BulkIndexer bulkIndexer = index.bulkIndexer();
		for ( VectorFieldTypeDescriptor<?> descriptor : supportedFieldTypes ) {
			List<?> values = descriptor.getUniquelyMatchableValues();
			for ( int i = 0; i < values.size(); i++ ) {
				Object value = values.get( i );
				bulkIndexer.add(
						identifier( i ), descriptor.getUniqueName(), d -> d.addValue( descriptor.getUniqueName(), value ) );
			}
		}
		bulkIndexer.join();
	}

	private static String identifier(int i) {
		return "id:" + i;
	}

	@Test
	void simpleSingleValueByte() {
		List<?> values = ByteVectorFieldTypeDescriptor.INSTANCE.getUniquelyMatchableValues();
		for ( int i = 0; i < values.size(); i++ ) {
			Object value = values.get( i );
			assertThatQuery( index.query()
					.where( f -> f.knn( 1 )
							.field( ByteVectorFieldTypeDescriptor.INSTANCE.getUniqueName() )
							.matching( (byte[]) value ) )
					.routing( ByteVectorFieldTypeDescriptor.INSTANCE.getUniqueName() ) )
					.hasTotalHitCount( 1 )
					.hasDocRefHitsAnyOrder( index.typeName(), identifier( i ) );
		}
	}

	@Test
	void simpleSingleValueFloat() {
		List<?> values = FloatVectorFieldTypeDescriptor.INSTANCE.getUniquelyMatchableValues();
		for ( int i = 0; i < values.size(); i++ ) {
			Object value = values.get( i );
			assertThatQuery( index.query()
					.where( f -> f.knn( 1 )
							.field( FloatVectorFieldTypeDescriptor.INSTANCE.getUniqueName() )
							.matching( (float[]) value ) )
					.routing( FloatVectorFieldTypeDescriptor.INSTANCE.getUniqueName() ) )
					.hasTotalHitCount( 1 )
					.hasDocRefHitsAnyOrder( index.typeName(), identifier( i ) );
		}
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
