/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection;

/**
 * Allow to specify whether any values projected from the query should be converted back, if {@code ENABLED}, or not, if {@code DISABLED}.
 * If no conversion is expected on the field, this option will have no effect.
 * <p>
 * Similarly to {@link org.hibernate.search.engine.search.predicate.DslConverter}, if {@code DISABLED},
 * projection will return values exactly as they are stored on back-end.
 */
public enum ProjectionConverter {

	ENABLED, DISABLED;

	public boolean isEnabled() {
		return this.equals( ENABLED );
	}
}
