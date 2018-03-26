/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;

abstract class PojoTypeWorker {

	final PojoSessionContext sessionContext;

	PojoTypeWorker(PojoSessionContext sessionContext) {
		this.sessionContext = sessionContext;
	}

	public abstract void add(Object id, Object entity);

	public abstract void update(Object id, Object entity);

	public abstract void delete(Object id, Object entity);

}
