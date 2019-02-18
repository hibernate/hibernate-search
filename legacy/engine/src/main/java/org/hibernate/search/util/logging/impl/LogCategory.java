/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;


/**
 * Log category to be used with {@link LoggerFactory#make(LogCategory)}.
 *
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public final class LogCategory {

	private final String name;

	public LogCategory(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
