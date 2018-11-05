/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

public class MutableObjectLoadingOptions {
	private int fetchSize;

	public int getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		if ( fetchSize <= 0 ) {
			throw new IllegalArgumentException( "'fetch size' parameter less than or equals to 0" );
		}
		this.fetchSize = fetchSize;
	}
}
