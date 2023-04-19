/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public class LuceneEntityCompositeProjection<E> extends AbstractLuceneProjection<E> {
	private final LuceneSearchProjection<E> delegate;

	public LuceneEntityCompositeProjection(LuceneSearchIndexScope<?> scope, LuceneSearchProjection<E> delegate) {
		super( scope );
		this.delegate = delegate;
	}

	@Override
	public Extractor<?, E> request(ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.ENTITY,
				LuceneSearchHints.INSTANCE.entityProjectionNestingNotSupportedHint()
		);
		return delegate.request( context );
	}
}
