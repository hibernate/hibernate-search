/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.impl.IdHashShardingStrategy;

public class SpecificShardingStrategy extends IdHashShardingStrategy {

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] filters) {
		FullTextFilter filter = getFilter( filters, "shard" );
		if ( filter == null ) {
			return getIndexManagersForAllShards();
		}
		else {
			return new IndexManager[] {
						getIndexManagersForAllShards()[Integer.parseInt( filter.getParameter( "index" )
					.toString() )]
				};
		}
	}

	private FullTextFilter getFilter(FullTextFilterImplementor[] filters, String name) {
		for ( FullTextFilterImplementor filter : filters ) {
			if ( filter.getName().equals( name ) ) {
				return filter;
			}
		}
		return null;
	}

}
