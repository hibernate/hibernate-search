/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;


/**
 * The initial step in a "within" predicate definition, where the target field can be set.
 */
public interface SpatialWithinPredicateFieldStep {

	/**
	 * Target the given field in the "within" predicate.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 */
	default SpatialWithinPredicateFieldMoreStep field(String absoluteFieldPath) {
		return fields( absoluteFieldPath );
	}

	/**
	 * @deprecated Use {@link #field(String)} instead.
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 */
	@Deprecated
	default SpatialWithinPredicateFieldMoreStep onField(String absoluteFieldPath) {
		return field( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the "within" predicate.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link RangePredicateFieldMoreStep#field(String)},
	 * the only difference being that calls to {@link RangePredicateFieldMoreStep#boost(float)}
	 * and other field-specific settings on the returned step will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 *
	 * @see #field(String)
	 */
	SpatialWithinPredicateFieldMoreStep fields(String ... absoluteFieldPaths);

	/**
	 * @deprecated Use {@link #fields(String...)} instead.
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 */
	@Deprecated
	default SpatialWithinPredicateFieldMoreStep onFields(String ... absoluteFieldPaths) {
		return fields( absoluteFieldPaths );
	}

}
