/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
