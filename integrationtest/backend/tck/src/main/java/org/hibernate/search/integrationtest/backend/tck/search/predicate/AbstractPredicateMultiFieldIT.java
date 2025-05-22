/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateMultiFieldIT<V extends AbstractPredicateTestValues<?>> {

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void fieldAndField(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndField( f, field0Path( index, dataSet ), field1Path( index, dataSet ),
						0, dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndField( f, field0Path( index, dataSet ), field1Path( index, dataSet ),
						1, dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void fields(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicateOnFields( f,
						new String[] { field0Path( index, dataSet ), field1Path( index, dataSet ) }, 0, dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicateOnFields( f,
						new String[] { field0Path( index, dataSet ), field1Path( index, dataSet ) }, 1, dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void fieldAndFields(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndFields( f, field0Path( index, dataSet ),
						new String[] { field1Path( index, dataSet ), field2Path( index, dataSet ) },
						0, dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndFields( f, field0Path( index, dataSet ), new String[] {
						field1Path(
								index, dataSet ),
						field2Path( index, dataSet ) },
						1, dataSet
				) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		if ( dataSet.hasMoreThanTwoValues() ) {
			// Booleans have only two values...
			assertThatQuery( index.query()
					.where( f -> predicateOnFieldAndFields( f, field0Path( index, dataSet ),
							new String[] { field1Path( index, dataSet ), field2Path( index, dataSet ) }, 2, dataSet
					) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 2 ) );
		}
	}

	protected abstract PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
			String otherFieldPath, int matchingDocOrdinal, DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths,
			int matchingDocOrdinal,
			DataSet<?, V> dataSet);

	protected abstract PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
			String[] fieldPaths, int matchingDocOrdinal, DataSet<?, V> dataSet);

	private String field0Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().field0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String field1Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().field1.get( dataSet.fieldType ).relativeFieldName;
	}

	private String field2Path(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		return index.binding().field2.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field0;
		private final SimpleFieldModelsByType field1;
		private final SimpleFieldModelsByType field2;

		public IndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field0 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "field0_" );
			field1 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "field1_" );
			field2 = SimpleFieldModelsByType.mapAll( fieldTypes, root, "field2_" );
		}
	}

	public static final class DataSet<F, V extends AbstractPredicateTestValues<F>>
			extends AbstractPerFieldTypePredicateDataSet<F, V> {
		public DataSet(V values) {
			super( values );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
			indexer.add( docId( 0 ), routingKey,
					document -> initDocument( index, document, values.fieldValue( 0 ) ) );
			indexer.add( docId( 1 ), routingKey,
					document -> initDocument( index, document, values.fieldValue( 1 ) ) );
			if ( hasMoreThanTwoValues() ) {
				// Booleans have only two values...
				indexer.add( docId( 2 ), routingKey,
						document -> initDocument( index, document, values.fieldValue( 2 ) ) );
			}
		}

		private boolean hasMoreThanTwoValues() {
			return values.size() > 2;
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document,
				F fieldValue) {
			IndexBinding binding = index.binding();
			document.addValue( binding.field0.get( fieldType ).reference, fieldValue );
			document.addValue( binding.field1.get( fieldType ).reference, fieldValue );
			document.addValue( binding.field2.get( fieldType ).reference, fieldValue );
		}
	}
}
