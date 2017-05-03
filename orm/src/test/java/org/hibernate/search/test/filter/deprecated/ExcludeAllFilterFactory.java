/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter.deprecated;

import org.apache.lucene.search.Filter;
import org.hibernate.search.annotations.Factory;

/**
 * @author Emmanuel Bernard
 */
public class ExcludeAllFilterFactory {

	@Factory
	public Filter getFilter() {
		return new ExcludeAllFilter();
	}

}
