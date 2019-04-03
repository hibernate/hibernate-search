/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.search.Query;

public class LuceneExistsPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements ExistsPredicateBuilder<LuceneSearchPredicateBuilder> {

	private final String absoluteFieldPath;
	private final LuceneFieldCodec<?> codec;

	LuceneExistsPredicateBuilder(String absoluteFieldPath, LuceneFieldCodec<?> codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
		// Score is always constant for this query
		withConstantScore();
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return codec.createExistsQuery( absoluteFieldPath );
	}

}
