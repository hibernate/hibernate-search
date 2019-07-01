/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import org.hibernate.search.util.common.impl.Contracts;

public class MutableEntityLoadingOptions {
	private int fetchSize;

	public MutableEntityLoadingOptions(HibernateOrmLoadingMappingContext mappingContext) {
		this.fetchSize = mappingContext.getFetchSize();
	}

	int getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		Contracts.assertStrictlyPositive( fetchSize, "fetchSize" );
		this.fetchSize = fetchSize;
	}
}
