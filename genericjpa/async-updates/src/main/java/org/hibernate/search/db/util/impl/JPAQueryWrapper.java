/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.util.impl;

import javax.persistence.Query;
import java.util.List;

/**
 * Created by Martin on 11.11.2015.
 */
public final class JPAQueryWrapper implements QueryWrapper {

	private final Query query;

	public JPAQueryWrapper(Query query) {
		this.query = query;
	}

	@Override
	public void setMaxResults(long maxResults) {
		this.query.setMaxResults( (int) maxResults );
	}

	@Override
	public void setFirstResult(long firstResult) {
		this.query.setFirstResult( (int) firstResult );
	}

	@Override
	public List getResultList() {
		return this.query.getResultList();
	}

	@Override
	public Object getSingleResult() {
		return this.query.getSingleResult();
	}

	@Override
	public int executeUpdate() {
		return this.query.executeUpdate();
	}
}
