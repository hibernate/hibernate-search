/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.filter.spi;

import java.util.concurrent.atomic.AtomicReference;

public class PojoAutomaticIndexingTypeFilterHolder {
	private AtomicReference<PojoAutomaticIndexingTypeFilter> filter = new AtomicReference<>();

	public PojoAutomaticIndexingTypeFilterHolder() {
		this( PojoAutomaticIndexingTypeFilter.ACCEPT_ALL );
	}

	public PojoAutomaticIndexingTypeFilterHolder(PojoAutomaticIndexingTypeFilter filter) {
		this.filter.set( filter );
	}

	public PojoAutomaticIndexingTypeFilter filter() {
		return filter.get();
	}

	public void filter(PojoAutomaticIndexingTypeFilter filter) {
		this.filter.set( filter );
	}
}
