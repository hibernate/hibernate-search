/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

/**
 * Represents a {@code FullTextFilter} that is about to be applied.
 * Used to inject parameters
 *
 * @author Emmanuel Bernard
 */
public interface FullTextFilter {
	FullTextFilter setParameter(String name, Object value);

	Object getParameter(String name);
}
