/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.definition;

import java.util.Optional;

import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link PredicateDefinition#create(PredicateDefinitionContext)}.
 * @see PredicateDefinition#create(PredicateDefinitionContext)
 */
@Incubating
public interface PredicateDefinitionContext {

	/**
	 * @return A predicate factory.
	 * If the named predicate was registered on an object field,
	 * this factory expects field paths to be provided relative to that same object field.
	 * This factory is only valid in the present context and must not be used after
	 * {@link PredicateDefinition#create(PredicateDefinitionContext)} returns.
	 * @see SearchPredicateFactory
	 */
	SearchPredicateFactory predicate();

	/**
	 * @param name The name of the parameter.
	 * @return The value provided to {@link NamedPredicateOptionsStep#param(String, Object)} for this parameter.
	 * @throws SearchException no value was provided for this parameter.
	 * @see NamedPredicateOptionsStep#param(String, Object)
	 */
	Object param(String name);

	/**
	 * @param name The name of the parameter.
	 * @return An optional containing the value provided to {@link NamedPredicateOptionsStep#param(String, Object)}
	 * for this parameter, or {@code Optional.empty()} if no value was provided for this parameter.
	 * @see NamedPredicateOptionsStep#param(String, Object)
	 */
	Optional<Object> paramOptional(String name);

}
