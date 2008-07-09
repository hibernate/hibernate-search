// $Id$
package org.hibernate.search.test.filter;

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
