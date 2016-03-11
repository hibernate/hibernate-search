/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * Phrase queries with only one term.
 *
 * @author Davide D'Alto
 */
public class PhraseAsTermQuery extends TermQuery {

	public PhraseAsTermQuery(Term t) {
		super( t );
	}
}
