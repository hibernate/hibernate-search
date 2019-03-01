/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.DslConverter;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
abstract class AbstractLuceneStandardFieldSortBuilderFactory<F, C extends LuceneStandardFieldCodec<F, ?>>
		implements LuceneFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean sortable;

	private final ToDocumentFieldValueConverter<?, ? extends F> converter;
	private final ToDocumentFieldValueConverter<F, ? extends F> rawConverter;

	protected final C codec;

	protected AbstractLuceneStandardFieldSortBuilderFactory(boolean sortable,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			C codec) {
		this.sortable = sortable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldSortBuilderFactory obj, DslConverter dslConverter) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != this.getClass() ) {
			return false;
		}

		AbstractLuceneStandardFieldSortBuilderFactory<?, ?> other = (AbstractLuceneStandardFieldSortBuilderFactory<?, ?>) obj;

		if ( sortable != other.sortable || !codec.isCompatibleWith( other.codec ) ) {
			return false;
		}
		return !dslConverter.isEnabled() || converter.isCompatibleWith( other.converter );
	}

	protected void checkSortable(String absoluteFieldPath) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	protected ToDocumentFieldValueConverter<?, ? extends F> getConverter(DslConverter dslConverter) {
		return ( dslConverter.isEnabled() ) ? converter : rawConverter;
	}
}
