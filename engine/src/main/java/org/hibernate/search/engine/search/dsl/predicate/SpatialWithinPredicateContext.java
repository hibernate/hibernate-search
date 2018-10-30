/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when starting to define a spatial predicate.
 */
public interface SpatialWithinPredicateContext {

	/**
	 * Target the given field in the "within" predicate.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A {@link SpatialWithinPredicateFieldSetContext} allowing to
	 * continue the definition of the spatial predicate.
	 */
	default SpatialWithinPredicateFieldSetContext onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the "within" predicate.
	 * <p>
	 * Equivalent to {@link #onField(String)} followed by multiple calls to
	 * {@link RangePredicateFieldSetContext#orField(String)},
	 * the only difference being that calls to {@link RangePredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link SpatialWithinPredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onField(String)
	 */
	SpatialWithinPredicateFieldSetContext onFields(String ... absoluteFieldPaths);

}
