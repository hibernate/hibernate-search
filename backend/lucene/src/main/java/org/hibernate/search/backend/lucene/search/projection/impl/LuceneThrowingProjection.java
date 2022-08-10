/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.util.common.SearchException;

import org.apache.lucene.index.LeafReaderContext;

public class LuceneThrowingProjection<T> extends AbstractLuceneProjection<T>
		implements LuceneSearchProjection.Extractor<Object, T>, Values<Object> {
	private final Supplier<SearchException> exceptionSupplier;

	public LuceneThrowingProjection(LuceneSearchIndexScope<?> scope, Supplier<SearchException> exceptionSupplier) {
		super( scope );
		this.exceptionSupplier = exceptionSupplier;
	}

	@Override
	public Extractor<?, T> request(ProjectionRequestContext context) {
		return this;
	}

	@Override
	public Values<Object> values(ProjectionExtractContext context) {
		return this;
	}

	@Override
	public void context(LeafReaderContext context) throws IOException {
		// Nothing to do
	}

	@Override
	public Object get(int doc) throws IOException {
		throw exceptionSupplier.get();
	}

	@Override
	public T transform(LoadingResult<?, ?> loadingResult, Object extractedData, ProjectionTransformContext context) {
		throw exceptionSupplier.get();
	}
}
