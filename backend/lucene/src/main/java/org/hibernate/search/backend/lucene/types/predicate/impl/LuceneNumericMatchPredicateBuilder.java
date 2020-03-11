/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneStandardMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;

import org.apache.lucene.search.Query;

class LuceneNumericMatchPredicateBuilder<F, E extends Number>
		extends AbstractLuceneStandardMatchPredicateBuilder<F, E, AbstractLuceneNumericFieldCodec<F, E>> {

	LuceneNumericMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchContext, absoluteFieldPath, nestedPathHierarchy, converter, rawConverter, converterChecker, codec );
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return codec.getDomain().createExactQuery( absoluteFieldPath, value );
	}
}
