/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Set;

import org.hibernate.search.engine.search.predicate.dsl.RegexpQueryFlag;

public interface RegexpPredicateBuilder extends SearchPredicateBuilder {

	void pattern(String regexpPattern);

	void param(String parameterName);

	void flags(Set<RegexpQueryFlag> flags);
}
