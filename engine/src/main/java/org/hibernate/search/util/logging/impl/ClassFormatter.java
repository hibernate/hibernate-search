/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith} to display {@link Class} names in log
 * messages.
 *
 * @author Gunnar Morling
 */
public class ClassFormatter {

	private final String stringRepresentation;

	public ClassFormatter(Class<?> clazz) {
		// TODO clazz should never be null, but it actually is in some cases; Ignoring that for now as it should be gone
		// with the free-form rework
		this.stringRepresentation = clazz != null ? clazz.getName() : null;
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
