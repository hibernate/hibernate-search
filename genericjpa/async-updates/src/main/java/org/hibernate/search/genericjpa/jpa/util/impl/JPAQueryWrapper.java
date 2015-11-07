package org.hibernate.search.genericjpa.jpa.util.impl;

import javax.persistence.Query;
import java.util.List;

import org.hibernate.search.genericjpa.jpa.util.impl.QueryWrapper;

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
