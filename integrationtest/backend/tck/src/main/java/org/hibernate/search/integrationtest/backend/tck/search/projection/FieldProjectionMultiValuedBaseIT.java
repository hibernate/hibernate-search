/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests basic behavior of projections on a multi-valued field, common to all supported types.
 * <p>
 * See {@link FieldProjectionSingleValuedBaseIT} for single-valued fields.
 */
@TestForIssue(jiraKey = "HSEARCH-3391")

class FieldProjectionMultiValuedBaseIT<F> {

	private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAllStandard();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();

	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
			for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
				if ( fieldStructure.isSingleValued() ) {
					continue;
				}
				DataSet<?> dataSet = new DataSet<>( fieldStructure, fieldType );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( fieldStructure, fieldType, dataSet ) );
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@SuppressWarnings("unused") // For EJC and lambda arg
	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.create(
					root,
					(Collection<? extends StandardFieldTypeDescriptor<?>>) supportedFieldTypes,
					TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault()
							? c -> {}
							: c -> c.projectable( Projectable.YES )
			);

	private static final SimpleMappedIndex<SingleFieldIndexBinding> index = SimpleMappedIndex.of( bindingFactory );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.contribute( indexer );
		}
		indexer.join();
	}

	@Deprecated(since = "test")
	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void simple(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ).multi() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValues( 1 ),
						dataSet.getFieldValues( 2 ),
						dataSet.getFieldValues( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void simpleList(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ).list() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValues( 1 ),
						dataSet.getFieldValues( 2 ),
						dataSet.getFieldValues( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void simpleSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ).set() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						new HashSet<>( dataSet.getFieldValues( 1 ) ),
						new HashSet<>( dataSet.getFieldValues( 2 ) ),
						new HashSet<>( dataSet.getFieldValues( 3 ) ),
						Collections.emptySet() // Empty document
				);
	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void simpleArray(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() )
						.array( fieldType.getJavaType() ) )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValues( 1 ).toArray( n -> (F[]) Array.newInstance( fieldType.getJavaType(), n ) ),
						dataSet.getFieldValues( 2 ).toArray( n -> (F[]) Array.newInstance( fieldType.getJavaType(), n ) ),
						dataSet.getFieldValues( 3 ).toArray( n -> (F[]) Array.newInstance( fieldType.getJavaType(), n ) ),
						(F[]) Array.newInstance( fieldType.getJavaType(), 0 ) // Empty document
				);

	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void noClass(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath ).list() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						Collections.unmodifiableList( dataSet.getFieldValues( 1 ) ),
						Collections.unmodifiableList( dataSet.getFieldValues( 2 ) ),
						Collections.unmodifiableList( dataSet.getFieldValues( 3 ) ),
						Collections.emptyList() // Empty document
				);
	}

	/**
	 * Test that mentioning the same projection twice works as expected.
	 */
	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void duplicated(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.composite(
						f.field( fieldPath, fieldType.getJavaType() ).list(),
						f.field( fieldPath, fieldType.getJavaType() ).list()
				)
				)
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						Arrays.asList( dataSet.getFieldValues( 1 ), dataSet.getFieldValues( 1 ) ),
						Arrays.asList( dataSet.getFieldValues( 2 ), dataSet.getFieldValues( 2 ) ),
						Arrays.asList( dataSet.getFieldValues( 3 ), dataSet.getFieldValues( 3 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ) // Empty document
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void set(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		var multi = scope.projection().field( fieldPath, fieldType.getJavaType() )
				.set()
				.toProjection();

		assertThatQuery( scope.query()
				.select( multi )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						new HashSet<>( dataSet.getFieldValues( 1 ) ),
						new HashSet<>( dataSet.getFieldValues( 2 ) ),
						new HashSet<>( dataSet.getFieldValues( 3 ) ),
						Collections.emptySet() // Empty document
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void sortedSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		assumeTrue( Comparable.class.isAssignableFrom( fieldType.getJavaType() ),
				"Some types that cannot be compared will fail the test as they cannot be added to a set without a comparator defined" );

		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		var multi = scope.projection().field( fieldPath, fieldType.getJavaType() )
				.sortedSet()
				.toProjection();

		assertThatQuery( scope.query()
				.select( multi )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						new TreeSet<>( dataSet.getFieldValues( 1 ) ),
						new TreeSet<>( dataSet.getFieldValues( 2 ) ),
						new TreeSet<>( dataSet.getFieldValues( 3 ) ),
						Collections.emptySortedSet() // Empty document
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void sortedSetComparator(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		Comparator<F> comparator = Comparator.comparing( Objects::toString );
		var multi = scope.projection().field( fieldPath, fieldType.getJavaType() )
				.sortedSet( comparator )
				.toProjection();

		assertThatQuery( scope.query()
				.select( multi )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						treeSet( comparator, dataSet.getFieldValues( 1 ) ),
						treeSet( comparator, dataSet.getFieldValues( 2 ) ),
						treeSet( comparator, dataSet.getFieldValues( 3 ) ),
						Collections.emptySortedSet() // Empty document
				);
	}

	private static <F> TreeSet<F> treeSet(Comparator<F> comparator, List<F> values) {
		TreeSet<F> set = new TreeSet<>( comparator );
		set.addAll( values );
		return set;
	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void array(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		var multi = scope.projection().field( fieldPath, fieldType.getJavaType() )
				.array( fieldType.getJavaType() )
				.toProjection();

		F[] arrayToConvert = (F[]) Array.newInstance( fieldType.getJavaType(), 0 );
		assertThatQuery( scope.query()
				.select( multi )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValues( 1 ).toArray( arrayToConvert ),
						dataSet.getFieldValues( 2 ).toArray( arrayToConvert ),
						dataSet.getFieldValues( 3 ).toArray( arrayToConvert ),
						arrayToConvert // Empty document
				);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("params")
	void explicitList(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = index.createScope();

		String fieldPath = getFieldPath( fieldStructure, fieldType );

		assertThatQuery( scope.query()
				.select( f -> f.field( fieldPath, fieldType.getJavaType() ).list() )
				.where( f -> f.matchAll() )
				.routing( dataSet.routingKey )
				.toQuery() )
				.hasHitsAnyOrder(
						dataSet.getFieldValues( 1 ),
						dataSet.getFieldValues( 2 ),
						dataSet.getFieldValues( 3 ),
						Collections.emptyList() // Empty document
				);
	}

	private String getFieldPath(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType) {
		return index.binding().getFieldPath( fieldStructure, fieldType );
	}

	private static class DataSet<F> {
		private final TestedFieldStructure fieldStructure;
		private final FieldTypeDescriptor<F, ?> fieldType;
		private final String routingKey;

		private DataSet(TestedFieldStructure fieldStructure, FieldTypeDescriptor<F, ?> fieldType) {
			this.fieldStructure = fieldStructure;
			this.fieldType = fieldType;
			this.routingKey = fieldType.getUniqueName() + "_" + fieldStructure.getUniqueName();
		}

		private String docId(int docNumber) {
			return routingKey + "_doc_" + docNumber;
		}

		private String emptyDocId(int docNumber) {
			return routingKey + "_emptyDoc_" + docNumber;
		}

		private void contribute(BulkIndexer indexer) {
			indexer.add( documentProvider( emptyDocId( 1 ), routingKey,
					document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
							document, Collections.emptyList() ) ) );
			indexer.add( documentProvider( docId( 1 ), routingKey,
					document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 1 ) ) ) );
			indexer.add( documentProvider( docId( 2 ), routingKey,
					document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 2 ) ) ) );
			indexer.add( documentProvider( docId( 3 ), routingKey,
					document -> index.binding().initMultiValued( fieldType, fieldStructure.location,
							document, getFieldValues( 3 ) ) ) );
		}

		private List<F> getFieldValues(int documentNumber) {
			return fieldType.getIndexableValues().getMulti().get( documentNumber - 1 );
		}
	}
}
