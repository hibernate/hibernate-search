/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

public interface PhrasePredicateBuilder extends SearchPredicateBuilder {

	void slop(int slop);

	void phrase(String phrase);

	void param(String parameterName);

	void analyzer(String analyzerName);

	void skipAnalysis();
}
