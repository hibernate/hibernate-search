/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;

import org.apache.lucene.index.LeafReaderContext;

public class LuceneConstantProjection<T> extends AbstractLuceneProjection<T>
		implements LuceneSearchProjection.Extractor<T, T>, Values<T> {
	private final T value;

	public LuceneConstantProjection(LuceneSearchIndexScope<?> scope, T value) {
		super( scope );
		this.value = value;
	}

	@Override
	public Extractor<?, T> request(ProjectionRequestContext context) {
		return this;
	}

	@Override
	public Values<T> values(ProjectionExtractContext context) {
		return this;
	}

	@Override
	public void context(LeafReaderContext context) throws IOException {
		// Ignore.
	}

	@Override
	public T get(int doc) throws IOException {
		return value;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, T extractedData, ProjectionTransformContext context) {
		return extractedData;
	}
}
