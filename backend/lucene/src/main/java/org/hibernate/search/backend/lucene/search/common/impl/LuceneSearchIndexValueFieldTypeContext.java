/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;

import org.apache.lucene.analysis.Analyzer;

public interface LuceneSearchIndexValueFieldTypeContext<F>
		extends SearchIndexValueFieldTypeContext<LuceneSearchIndexScope<?>, LuceneSearchIndexValueFieldContext<F>, F> {

	LuceneFieldCodec<F> codec();

	Analyzer searchAnalyzerOrNormalizer();

	boolean hasTermVectorsConfigured();

}
