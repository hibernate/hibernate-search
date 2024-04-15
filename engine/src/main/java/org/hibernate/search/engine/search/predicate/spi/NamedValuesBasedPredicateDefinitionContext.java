/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.spi.MapNamedValues;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class NamedValuesBasedPredicateDefinitionContext implements PredicateDefinitionContext {

	private final SearchPredicateFactory factory;
	private final NamedValues parameters;

	public NamedValuesBasedPredicateDefinitionContext(SearchPredicateFactory factory, Map<String, Object> params,
			Function<String, SearchException> namedValueMissing) {
		this.factory = factory;
		this.parameters = MapNamedValues.fromMap( params, namedValueMissing );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return factory;
	}

	@Override
	public NamedValues params() {
		return parameters;
	}

}
