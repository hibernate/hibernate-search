/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Test;

public abstract class AbstractPredicateMultiFieldIT<V extends AbstractPredicateTestValues<?>> {

	private final SimpleMappedIndex<IndexBinding> index;
	protected final DataSet<?, V> dataSet;

	protected AbstractPredicateMultiFieldIT(SimpleMappedIndex<IndexBinding> index, DataSet<?, V> dataSet) {
		this.index = index;
		this.dataSet = dataSet;
	}

	@Test
	public void fieldAndField() {
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndField( f, field0Path(), field1Path(),
						0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndField( f, field0Path(), field1Path(),
						1 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	public void fields() {
		assertThatQuery( index.query()
				.where( f -> predicateOnFields( f,
						new String[] { field0Path(), field1Path() }, 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicateOnFields( f,
						new String[] { field0Path(), field1Path() }, 1 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
	}

	@Test
	public void fieldAndFields() {
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndFields( f, field0Path(),
						new String[] { field1Path(), field2Path() },
						0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
		assertThatQuery( index.query()
				.where( f -> predicateOnFieldAndFields( f, field0Path(), new String[] { field1Path(), field2Path() },
						1 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 1 ) );
		if ( dataSet.hasMoreThanTwoValues() ) {
			// Booleans have only two values...
			assertThatQuery( index.query()
					.where( f -> predicateOnFieldAndFields( f, field0Path(),
							new String[] { field1Path(), field2Path() }, 2 ) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 2 ) );
		}
	}

	protected abstract PredicateFinalStep predicateOnFieldAndField(SearchPredicateFactory f, String fieldPath,
			String otherFieldPath, int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateOnFields(SearchPredicateFactory f, String[] fieldPaths,
			int matchingDocOrdinal);

	protected abstract PredicateFinalStep predicateOnFieldAndFields(SearchPredicateFactory f, String fieldPath,
			String[] fieldPaths, int matchingDocOrdinal);

	private String field0Path() {
		return index.binding().field0.get( dataSet.fieldType ).relativeFieldName;
	}

	private String field1Path() {
		return index.binding().field1.get( dataSet.fieldType ).relativeFieldName;
	}

	private String field2Path() {
		return index.binding().field2.get( dataSet.fieldType ).relativeFieldName;
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field0;
		private final SimpleFieldModelsByType field1;
		private final SimpleFieldModelsByType field2;

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
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
