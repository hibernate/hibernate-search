/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "within" predicate definition where the area to match can be set
 * (see the superinterface {@link SpatialWithinPredicateAreaStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 */
public interface SpatialWithinPredicateFieldMoreStep
		extends SpatialWithinPredicateAreaStep,
		MultiFieldPredicateFieldBoostStep<SpatialWithinPredicateFieldMoreStep> {

	/**
	 * Target the given field in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return The next step.
	 *
	 * @see SpatialWithinPredicateFieldStep#field(String)
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
	default SpatialWithinPredicateFieldMoreStep orField(String absoluteFieldPath) {
		return field( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the "within" predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link SpatialWithinPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 *
	 * @see SpatialWithinPredicateFieldStep#fields(String...)
	 */
	SpatialWithinPredicateFieldMoreStep fields(String ... absoluteFieldPaths);

	/**
	 * @deprecated Use {@link #fields(String...)} instead.
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return The next step.
	 */
	@Deprecated
	default SpatialWithinPredicateFieldMoreStep orFields(String ... absoluteFieldPaths) {
		return fields( absoluteFieldPaths );
	}

}
