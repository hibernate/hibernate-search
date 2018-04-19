/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Locale;
import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldFormatter;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.document.model.impl.MatchQueryOptions;
import org.hibernate.search.backend.lucene.document.model.impl.RangeQueryOptions;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;

/**
 * @author Guillaume Smet
 */
public class LocalDateIndexSchemaFieldContext extends AbstractLuceneIndexSchemaFieldTypedContext<LocalDate> {

	private Sortable sortable;

	public LocalDateIndexSchemaFieldContext(String fieldName) {
		super( fieldName );
	}

	@Override
	public LocalDateIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldAccessor<LocalDate> accessor, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		LocalDateFieldFormatter localDateFieldFormatter = new LocalDateFieldFormatter( getStore(), sortable );

		LuceneIndexSchemaFieldNode<LocalDate> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getFieldName(),
				localDateFieldFormatter,
				new LocalDateFieldQueryFactory( localDateFieldFormatter ),
				LocalDateFieldSortContributor.INSTANCE
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	private static final class LocalDateFieldFormatter implements LuceneFieldFormatter<LocalDate> {

		private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
				.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
				.appendLiteral( '-' )
				.appendValue( MONTH_OF_YEAR, 2 )
				.appendLiteral( '-' )
				.appendValue( DAY_OF_MONTH, 2 )
				.toFormatter( Locale.ROOT )
				.withResolverStyle( ResolverStyle.STRICT );

		private final Store store;

		private final Sortable sortable;

		private LocalDateFieldFormatter(Store store, Sortable sortable) {
			this.store = store;
			this.sortable = sortable;
		}

		@Override
		public void addFields(LuceneDocumentBuilder documentBuilder, LuceneIndexSchemaObjectNode parentNode, String fieldName, LocalDate value) {
			if ( value == null ) {
				return;
			}

			if ( Store.YES.equals( store ) ) {
				documentBuilder.addField( parentNode, new StoredField( fieldName, FORMATTER.format( value ) ) );
			}

			long valueToEpochDay = value.toEpochDay();

			if ( Sortable.YES.equals( sortable ) ) {
				documentBuilder.addField( parentNode, new NumericDocValuesField( fieldName, valueToEpochDay ) );
			}

			documentBuilder.addField( parentNode, new LongPoint( fieldName, valueToEpochDay ) );
		}

		@Override
		public Object format(Object value) {
			return ((LocalDate) value).toEpochDay();
		}

		@Override
		public LocalDate parse(Document document, String fieldName) {
			IndexableField field = document.getField( fieldName );

			if ( field == null ) {
				return null;
			}

			String value = field.stringValue();

			if ( value == null ) {
				return null;
			}

			return LocalDate.parse( value, FORMATTER );
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( LocalDateFieldFormatter.class != obj.getClass() ) {
				return false;
			}

			LocalDateFieldFormatter other = (LocalDateFieldFormatter) obj;

			return Objects.equals( store, other.store ) && Objects.equals( sortable, other.sortable );
		}

		@Override
		public int hashCode() {
			return Objects.hash( store, sortable );
		}
	}

	private static final class LocalDateFieldQueryFactory implements LuceneFieldQueryFactory {

		private final LocalDateFieldFormatter localDateFieldFormatter;

		private LocalDateFieldQueryFactory(LocalDateFieldFormatter localDateFieldFormatter) {
			this.localDateFieldFormatter = localDateFieldFormatter;
		}

		@Override
		public Query createMatchQuery(String fieldName, Object value, MatchQueryOptions matchQueryOptions) {
			return LongPoint.newExactQuery( fieldName, ((LocalDate) value).toEpochDay() );
		}

		@Override
		public Query createRangeQuery(String fieldName, Object lowerLimit, Object upperLimit, RangeQueryOptions rangeQueryOptions) {
			return LongPoint.newRangeQuery(
					fieldName,
					getLowerValue( lowerLimit, rangeQueryOptions.isExcludeLowerLimit() ),
					getUpperValue( upperLimit, rangeQueryOptions.isExcludeUpperLimit() )
			);
		}

		private long getLowerValue(Object lowerLimit, boolean excludeLowerLimit) {
			if ( lowerLimit == null ) {
				return excludeLowerLimit ? Math.addExact( Long.MIN_VALUE, 1 ) : Long.MIN_VALUE;
			}
			else {
				long lowerLimitAsLong = (long) localDateFieldFormatter.format( lowerLimit );
				return excludeLowerLimit ? Math.addExact( lowerLimitAsLong, 1 ) : lowerLimitAsLong;
			}
		}

		private long getUpperValue(Object upperLimit, boolean excludeUpperLimit) {
			if ( upperLimit == null ) {
				return excludeUpperLimit ? Math.addExact( Long.MAX_VALUE, -1 ) : Long.MAX_VALUE;
			}
			else {
				long upperLimitAsLong = (long) localDateFieldFormatter.format( upperLimit );
				return excludeUpperLimit ? Math.addExact( upperLimitAsLong, -1 ) : upperLimitAsLong;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( LocalDateFieldQueryFactory.class != obj.getClass() ) {
				return false;
			}

			LocalDateFieldQueryFactory other = (LocalDateFieldQueryFactory) obj;

			return Objects.equals( localDateFieldFormatter, other.localDateFieldFormatter );
		}

		@Override
		public int hashCode() {
			return Objects.hash( localDateFieldFormatter );
		}
	}

	private static class LocalDateFieldSortContributor extends AbstractScalarLuceneFieldSortContributor {

		private static final LocalDateFieldSortContributor INSTANCE = new LocalDateFieldSortContributor();

		private LocalDateFieldSortContributor() {
			super( Long.MIN_VALUE, Long.MAX_VALUE );
		}

		@Override
		public void contribute(LuceneSearchSortCollector collector, String absoluteFieldPath, SortOrder order, Object missingValue) {
			SortField sortField = new SortField( absoluteFieldPath, SortField.Type.LONG, order == SortOrder.DESC ? true : false );
			setEffectiveMissingValue( sortField, missingValue, order );

			collector.collectSortField( sortField );
		}
	}
}
