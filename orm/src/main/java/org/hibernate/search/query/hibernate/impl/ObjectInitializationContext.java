/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.TimeoutManager;

/**
 * Context class wrapping all resources needed in the context of object initialization.
 *
 * @see org.hibernate.search.query.hibernate.impl.ObjectInitializer
 */
public class ObjectInitializationContext {
	private final Criteria criteria;
	private final Class<?> entityType;
	private final ExtendedSearchIntegrator extendedIntegrator;
	private final TimeoutManager timeoutManager;
	private final Session session;

	/**
	 * @param criteria A user specified {@code Criteria query} or {@code null}.
	 * See also {@link org.hibernate.search.FullTextQuery#setCriteriaQuery(org.hibernate.Criteria)}.
	 * @param targetedEntityType The entity type targeted explicitly by the user
	 * @param extendedIntegrator Handle to the search factory
	 * @param timeoutManager Handle to the timeout manager
	 * @param session Handle to the ORM session
	 */
	public ObjectInitializationContext(Criteria criteria,
			Class<?> targetedEntityType,
			ExtendedSearchIntegrator extendedIntegrator,
			TimeoutManager timeoutManager,
			Session session) {
		this.criteria = criteria;
		this.entityType = targetedEntityType;
		this.extendedIntegrator = extendedIntegrator;
		this.timeoutManager = timeoutManager;
		this.session = session;
	}

	public Criteria getCriteria() {
		return criteria;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public ExtendedSearchIntegrator getExtendedSearchintegrator() {
		return extendedIntegrator;
	}

	public TimeoutManager getTimeoutManager() {
		return timeoutManager;
	}

	public Session getSession() {
		return session;
	}
}
