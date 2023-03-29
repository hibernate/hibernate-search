/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

import org.apache.lucene.analysis.Analyzer;

public class LuceneMultiIndexSearchIndexValueFieldContext<F>
		extends AbstractMultiIndexSearchIndexValueFieldContext<
						LuceneSearchIndexValueFieldContext<F>,
						LuceneSearchIndexScope<?>,
						LuceneSearchIndexValueFieldTypeContext<F>,
						F
				>
		implements LuceneSearchIndexValueFieldContext<F>, LuceneSearchIndexValueFieldTypeContext<F> {

	public LuceneMultiIndexSearchIndexValueFieldContext(LuceneSearchIndexScope<?> scope, String absolutePath,
			List<? extends LuceneSearchIndexValueFieldContext<F>> fieldForEachIndex) {
		super( scope, absolutePath, fieldForEachIndex );
	}

	@Override
	protected LuceneSearchIndexValueFieldContext<F> self() {
		return this;
	}

	@Override
	protected LuceneSearchIndexValueFieldTypeContext<F> selfAsNodeType() {
		return this;
	}

	@Override
	protected LuceneSearchIndexValueFieldTypeContext<F> typeOf(LuceneSearchIndexValueFieldContext<F> indexElement) {
		return indexElement.type();
	}

	@Override
	public LuceneSearchIndexCompositeNodeContext toComposite() {
		return SearchIndexSchemaElementContextHelper.throwingToComposite( this );
	}

	@Override
	public LuceneSearchIndexCompositeNodeContext toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public Analyzer searchAnalyzerOrNormalizer() {
		return fromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::searchAnalyzerOrNormalizer, Object::equals,
				"searchAnalyzerOrNormalizer" );
	}

	@Override
	public boolean hasTermVectorsConfigured() {
		return fromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::hasTermVectorsConfigured, Object::equals,
				"hasTermVectorsConfigured" );
	}
}
