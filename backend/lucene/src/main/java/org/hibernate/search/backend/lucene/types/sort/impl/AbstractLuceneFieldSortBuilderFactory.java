/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractLuceneFieldSortBuilderFactory<F, C extends LuceneFieldCodec<F>>
		implements LuceneFieldSortBuilderFactory {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final boolean sortable;

	protected final C codec;

	public AbstractLuceneFieldSortBuilderFactory(boolean sortable, C codec) {
		this.sortable = sortable;
		this.codec = codec;
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldSortBuilderFactory other) {
		if ( this == other ) {
			return true;
		}
		if ( other.getClass() != this.getClass() ) {
			return false;
		}

		AbstractLuceneFieldSortBuilderFactory<?, ?> otherFactory =
				(AbstractLuceneFieldSortBuilderFactory<?, ?>) other;
		return sortable == otherFactory.sortable && codec.isCompatibleWith( otherFactory.codec );
	}

	protected void checkSortable(String absoluteFieldPath) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
