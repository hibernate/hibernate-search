/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;

public final class FuzzyQueryBuilder extends QueryBuilder {
	private final int maxEditDistance;
	private final int prefixLength;

	public FuzzyQueryBuilder(Analyzer analyzer, int maxEditDistance, int prefixLength) {
		super( analyzer );
		this.maxEditDistance = maxEditDistance;
		this.prefixLength = prefixLength;
	}

	@Override
	protected Query newTermQuery(Term term) {
		return new FuzzyQuery( term, maxEditDistance, prefixLength );
	}
}
