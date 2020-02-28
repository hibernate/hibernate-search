/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.FieldContextSimpleQueryParser;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;

public final class LuceneSimpleQueryStringPredicateBuilderFieldState
	implements SimpleQueryStringPredicateBuilder.FieldState, FieldContextSimpleQueryParser.FieldContext {

	private final Analyzer analyzerOrNormalizer;
	private Float boost;

	LuceneSimpleQueryStringPredicateBuilderFieldState(Analyzer analyzerOrNormalizer) {
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public Query wrap(Query query) {
		if ( boost != null ) {
			return new BoostQuery( query, boost );
		}
		else {
			return query;
		}
	}

	public Analyzer getAnalyzerOrNormalizer() {
		return analyzerOrNormalizer;
	}

	public Float getBoost() {
		return boost;
	}

}
