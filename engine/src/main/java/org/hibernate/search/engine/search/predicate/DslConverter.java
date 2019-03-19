/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate;

/**
 * Allow to specify whether any values passed to the DSL should be converted, if {@code ENABLED}, or not, if {@code DISABLED}.
 * If no conversion is expected on the field, this option will have no effect.
 * <p>
 * Similarly to {@link org.hibernate.search.engine.search.projection.ProjectionConverter}, if {@code DISABLED},
 * values will be passed to the back-end exactly as the user has inserted them.
 */
public enum DslConverter {

	ENABLED, DISABLED;

	public boolean isEnabled() {
		return this.equals( ENABLED );
	}
}
