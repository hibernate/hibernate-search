/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

/**
 * Used with JBoss Logging to display class names in log messages.
 *
 * @author Gunnar Morling
 */
public class ClassObjectFormatter {

	private final String stringRepresentation;

	public ClassObjectFormatter(Class<?> clazz) {
		this.stringRepresentation = clazz.getName();
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}
}
