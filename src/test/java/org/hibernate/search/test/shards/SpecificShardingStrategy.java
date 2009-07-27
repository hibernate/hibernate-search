//$Id$
package org.hibernate.search.test.shards;

import org.hibernate.search.FullTextFilter;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IdHashShardingStrategy;

public class SpecificShardingStrategy extends IdHashShardingStrategy {	

	@Override
	public DirectoryProvider<?>[] getDirectoryProvidersForQuery(FullTextFilterImplementor[] filters) {
				
		FullTextFilter filter = getFilter(filters, "shard");
		if (filter == null) {
			return getDirectoryProvidersForAllShards();
		}
		else {
			return new DirectoryProvider[] { getDirectoryProvidersForAllShards()[Integer.parseInt(filter.getParameter("index").toString())] };
		}
	}

	private FullTextFilter getFilter(FullTextFilterImplementor[] filters, String name) {
		for (FullTextFilterImplementor filter: filters) {
			if (filter.getName().equals(name)) return filter;
		}
		return null;
	}	

}
