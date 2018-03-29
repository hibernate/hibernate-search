/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;

/**
 * @author Guillaume Smet
 */
class IndexSchemaFieldIntegerContext extends AbstractLuceneIndexSchemaFieldTypedContext<Integer> {

	private Sortable sortable;

	public IndexSchemaFieldIntegerContext(String fieldName) {
		super( fieldName );
	}

	@Override
	public IndexSchemaFieldIntegerContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldAccessor<Integer> accessor, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaFieldNode<Integer> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getFieldName(),
				new IntegerFieldFormatter( getStore(), sortable ),
				IntegerFieldQueryFactory.INSTANCE
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	private static final class IntegerFieldFormatter implements LuceneFieldFormatter<Integer> {

		private final Store store;

		private final Sortable sortable;

		private final int hashCode;

		private IntegerFieldFormatter(Store store, Sortable sortable) {
			this.store = store;
			this.sortable = sortable;

			this.hashCode = buildHashCode();
		}

		@Override
		public void addFields(LuceneDocumentBuilder documentBuilder, LuceneIndexSchemaObjectNode parentNode, String fieldName, Integer value) {
			if ( value == null ) {
				return;
			}

			if ( Store.YES.equals( store ) ) {
				documentBuilder.addField( parentNode, new StoredField( fieldName, value ) );
			}

			if ( Sortable.YES.equals( sortable ) ) {
				documentBuilder.addField( parentNode, new NumericDocValuesField( fieldName, value.longValue() ) );
			}

			documentBuilder.addField( parentNode, new IntPoint( fieldName, value ) );
		}

		@Override
		public Integer parse(Document document, String fieldName) {
			IndexableField field = document.getField( fieldName );

			if ( field == null ) {
				return null;
			}

			return (Integer) field.numericValue();
		}

		@Override
		public Object format(Object value) {
			return value;
		}

		@Override
		public Type getDefaultSortFieldType() {
			return SortField.Type.INT;
		}

		@Override
		public Object getSortMissingFirst() {
			return Integer.MIN_VALUE;
		}

		@Override
		public Object getSortMissingLast() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( IntegerFieldFormatter.class != obj.getClass() ) {
				return false;
			}

			IntegerFieldFormatter other = (IntegerFieldFormatter) obj;

			return Objects.equals( store, other.store );
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		private int buildHashCode() {
			return Objects.hashCode( store );
		}
	}

	private static final class IntegerFieldQueryFactory implements LuceneFieldQueryFactory {

		private static final IntegerFieldQueryFactory INSTANCE = new IntegerFieldQueryFactory();

		private IntegerFieldQueryFactory() {
		}

		@Override
		public Query createMatchQuery(String fieldName, Object value, MatchQueryOptions matchQueryOptions) {
			return IntPoint.newExactQuery( fieldName, (Integer) value );
		}

		@Override
		public Query createRangeQuery(String fieldName, Object lowerLimit, Object upperLimit, RangeQueryOptions rangeQueryOptions) {
			return IntPoint.newRangeQuery(
					fieldName,
					getLowerValue( lowerLimit, rangeQueryOptions.isExcludeLowerLimit() ),
					getUpperValue( upperLimit, rangeQueryOptions.isExcludeUpperLimit() )
			);
		}

		private static int getLowerValue(Object lowerLimit, boolean excludeLowerLimit) {
			if ( lowerLimit == null ) {
				return Integer.MIN_VALUE;
			}
			else {
				return excludeLowerLimit ? Math.addExact( (int) lowerLimit, 1 ) : (int) lowerLimit;
			}
		}

		private static int getUpperValue(Object upperLimit, boolean excludeUpperLimit) {
			if ( upperLimit == null ) {
				return Integer.MAX_VALUE;
			}
			else {
				return excludeUpperLimit ? Math.addExact( (int) upperLimit, -1 ) : (int) upperLimit;
			}
		}
	}
}
