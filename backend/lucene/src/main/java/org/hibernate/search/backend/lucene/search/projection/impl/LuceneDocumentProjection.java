/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;

class LuceneDocumentProjection extends AbstractLuceneProjection<Document>
		implements LuceneSearchProjection.Extractor<Document, Document> {

	LuceneDocumentProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, Document> request(ProjectionRequestContext context) {
		context.checkNotNested(
				LuceneProjectionTypeKeys.DOCUMENT,
				LuceneSearchHints.INSTANCE.documentProjectionNestingNotSupportedHint()
		);
		context.requireAllStoredFields();
		return this;
	}

	@Override
	public Values<Document> values(ProjectionExtractContext context) {
		StoredFieldsValuesDelegate delegate = context.collectorExecutionContext().storedFieldsValuesDelegate();
		return new Values<Document>() {
			@Override
			public void context(LeafReaderContext context) {
				// Nothing to do
			}

			@Override
			public Document get(int doc) {
				return delegate.get( doc );
			}
		};
	}

	@Override
	public Document transform(LoadingResult<?> loadingResult, Document extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}
}
