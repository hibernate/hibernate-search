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
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
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

	protected final DslConverter<?, ? extends F> converter;
	protected final DslConverter<F, ? extends F> rawConverter;

	protected final C codec;

	protected AbstractLuceneStandardFieldSortBuilderFactory(boolean sortable,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			C codec) {
		this.sortable = sortable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			String nestedDocumentPath, GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldSortBuilderFactory other) {
		if ( this == other ) {
			return true;
		}
		if ( other.getClass() != this.getClass() ) {
			return false;
		}

		AbstractLuceneStandardFieldSortBuilderFactory<?, ?> otherFactory = (AbstractLuceneStandardFieldSortBuilderFactory<?, ?>) other;
		return sortable == otherFactory.sortable && codec.isCompatibleWith( otherFactory.codec );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldSortBuilderFactory other) {
		if ( this == other ) {
			return true;
		}
		if ( other.getClass() != this.getClass() ) {
			return false;
		}

		AbstractLuceneStandardFieldSortBuilderFactory<?, ?> otherFactory = (AbstractLuceneStandardFieldSortBuilderFactory<?, ?>) other;
		return converter.isCompatibleWith( otherFactory.converter );
	}

	protected void checkSortable(String absoluteFieldPath) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
