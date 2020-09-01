/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reporting;

/**
 * An atomic element of context when an event occurs:
 * a mapped type, or an index, or a field path, ...
 */
public interface EventContextElement {

	/**
	 * @return A human-readable representation of this context.
	 * The representation should use brief, natural language to refer to objects rather than class names,
	 * e.g. "index 'myIndexName'" rather than "ElasticsearchIndexManager{name = 'myIndexName'}".
	 * The representation may change without prior notice in new versions of Hibernate Search:
	 * callers should not try to parse it.
	 */
	String render();

}
