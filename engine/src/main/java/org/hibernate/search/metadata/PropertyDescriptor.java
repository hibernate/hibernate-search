/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metadata;

/**
 * @author Hardy Ferentschik
 */
public interface PropertyDescriptor extends FieldContributor {
	/**
	 * Name of the property.
	 *
	 * @return name of the property
	 */
	String getName();

	/**
	 * Returns {@code true} if the property is the document id, {@code false} otherwise
	 *
	 * @return {@code true} if the property is the document id, {@code false} otherwise
	 *
	 * @see org.hibernate.search.annotations.DocumentId
	 */
	boolean isId();
}


