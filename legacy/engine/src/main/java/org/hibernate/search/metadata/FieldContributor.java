/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.metadata;

import java.util.Set;

/**
 * @author Hardy Ferentschik
 */
public interface FieldContributor {
	/**
	 * @return a set of {@code FieldDescriptor}s for the fields contributed by this element
	 */
	Set<FieldDescriptor> getIndexedFields();
}


