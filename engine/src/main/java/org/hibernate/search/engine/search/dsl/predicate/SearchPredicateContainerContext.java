/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;

/**
 * A context allowing to specify the type of a predicate.
 * <p>
 * This is the main entry point to the predicate DSL.
 *
 * <h3>Common concepts</h3>
 *
 * <h4 id="commonconcepts-parametertype">Type of predicate parameters</h4>
 *
 * Some predicates, such as the {@link #match()} predicate or the {@link #range()} predicate,
 * target an index field and define an {@link Object} parameter ({@link MatchPredicateFieldSetContext#matching(Object)},
 * {@link RangePredicateFieldSetContext#from(Object)}, ...).
 * The type of arguments passed to these methods is expected to be consistent with the targeted field:
 * if you want a field "myField" to be between values {@code X} and {@code Y},
 * Hibernate Search needs to be able to convert the values {@code X} and {@code Y}
 * to something comparable to the indexed value of "myField".
 * <p>
 * Generally the expected type of this argument should be rather obvious:
 * for example if you created a field by mapping an {@link Integer} property,
 * then an {@link Integer} value will be expected when building a predicate;
 * if you mapped a {@link java.time.LocalDate}, then a {@link java.time.LocalDate} will be expected.
 * Note that the type that is actually stored in the index does not matter:
 * if you mapped a custom enum type, then this same enum type will be expected throughout the predicate DSL,
 * even if it is stored as a {@link String} in the index.
 * <p>
 * Sometimes a predicate targets <em>multiple</em> fields, either explicitly
 * (multiple field names passed to the {@code onFields} method when defining the predicate)
 * or implicitly (multiple targeted indexes).
 * In that case, the type of the targeted fields must be compatible.
 * For example targeting an {@link Integer} field and a {@link java.time.LocalDate} field
 * in the same {@link #match()} predicate won't work, because you won't be able to pass a non-null argument
 * that is both an {@link Integer} and a {@link java.time.LocalDate}
 * to the {@link MatchPredicateFieldSetContext#matching(Object)} method.
 * Thus you generally cannot target fields with different types in the same predicate.
 * When targeting different fields in the same index, you can, however,
 * define one predicate for each field and combine them using a {@link #bool() boolean predicate}.
 * <p>
 * Note that custom bridges have the ability to customize the expected type of arguments in the DSL:
 * for example they could accept {@link String} arguments when targeting {@link Integer} fields.
 * See the documentation of bridges for more information on custom bridges.
 */
public interface SearchPredicateContainerContext {

	/**
	 * Match all documents.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#end() end the predicate definition}.
	 * @see MatchAllPredicateContext
	 */
	MatchAllPredicateContext matchAll();

	/**
	 * Match documents if they match a combination of boolean clauses.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#end() end the predicate definition}.
	 * @see BooleanJunctionPredicateContext
	 */
	BooleanJunctionPredicateContext bool();

	/**
	 * Match documents if they match a combination of boolean clauses,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return A context allowing to end the predicate definition.
	 * @see BooleanJunctionPredicateContext
	 */
	SearchPredicateTerminalContext bool(Consumer<? super BooleanJunctionPredicateContext> clauseContributor);

	/**
	 * Match documents where targeted fields have a value that "matches" a given single value.
	 * <p>
	 * Note that "value matching" may be exact or approximate depending on the type of the targeted fields:
	 * numeric fields in particular imply exact matches,
	 * while analyzed, full-text fields imply approximate matches depending on how they are analyzed.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#end() end the predicate definition}.
	 * @see MatchPredicateContext
	 */
	MatchPredicateContext match();

	/**
	 * Match documents where targeted fields have a value within lower and upper bounds.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#end() end the predicate definition}.
	 * @see RangePredicateContext
	 */
	RangePredicateContext range();

	/**
	 * Match documents where a
	 * {@link org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage#NESTED nested object}
	 * matches a given predicate.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#end() end the predicate definition}.
	 * @see NestedPredicateContext
	 */
	NestedPredicateContext nested();

	/**
	 * Access the different types of spatial predicates.
	 *
	 * @return A context allowing to define the type of spatial predicate.
	 * @see SpatialPredicateContext
	 */
	SpatialPredicateContext spatial();

	/**
	 * Match documents that match the given, previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that should match.
	 */
	void predicate(SearchPredicate searchPredicate);

	// TODO ids query (Type + list of IDs? Just IDs? See https://www.elastic.co/guide/en/elasticsearch/reference/5.5/query-dsl-ids-query.html)
	// TODO other queries (spatial, ...)

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering different types of predicates.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws org.hibernate.search.util.SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchPredicateContainerContextExtension<T> extension);

	/**
	 * Create a context allowing to try to apply multiple extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchPredicateContainerContextExtension)} method instead.
	 *
	 * @return A context allowing to define the extensions to attempt, and the corresponding predicates.
	 */
	SearchPredicateContainerExtensionContext extension();

}
