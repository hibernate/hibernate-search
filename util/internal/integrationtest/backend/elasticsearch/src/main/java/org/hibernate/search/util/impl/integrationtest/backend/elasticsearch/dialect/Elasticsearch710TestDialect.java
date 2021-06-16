/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

public class Elasticsearch710TestDialect extends Elasticsearch712TestDialect {

	@Override
	public boolean normalizesStringArgumentToWildcardPredicateForAnalyzedStringField() {
		// In ES 7.7 through 7.11, wildcard predicates on analyzed fields get their pattern normalized,
		// but that was deemed a bug and fixed in 7.12.2+: https://github.com/elastic/elasticsearch/pull/53127
		return true;
	}
}
