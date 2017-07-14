/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

import java.util.Set;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Criterion;
import org.hibernate.search.jsr352.massindexing.impl.steps.lucene.IndexScope;
import org.hibernate.search.util.StringHelper;

/**
 * Internal utility class for persistence usage.
 *
 * @author Mincong Huang
 */
public final class PersistenceUtil {

	private PersistenceUtil() {
		// Private constructor, do not use it.
	}

	/**
	 * Open a session with specific tenant ID. If the tenant ID argument is {@literal null} or empty, then a normal
	 * session will be returned. The entity manager factory should be not null and opened when calling this method.
	 *
	 * @param entityManagerFactory entity manager factory
	 * @param tenantId tenant ID, can be {@literal null} or empty.
	 *
	 * @return a new session
	 */
	public static Session openSession(EntityManagerFactory entityManagerFactory, String tenantId) {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		Session session;
		if ( StringHelper.isEmpty( tenantId ) ) {
			session = sessionFactory.openSession();
		}
		else {
			session = sessionFactory.withOptions()
					.tenantIdentifier( tenantId )
					.openSession();
		}
		return session;
	}

	/**
	 * Open a stateless session with specific tenant ID. If the tenant ID argument is {@literal null} or empty, then a
	 * normal stateless session will be returned. The entity manager factory should be not null and opened when calling
	 * this method.
	 *
	 * @param entityManagerFactory entity manager factory
	 * @param tenantId tenant ID, can be {@literal null} or empty.
	 *
	 * @return a new stateless session
	 */
	public static StatelessSession openStatelessSession(EntityManagerFactory entityManagerFactory, String tenantId) {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		StatelessSession statelessSession;
		if ( StringHelper.isEmpty( tenantId ) ) {
			statelessSession = sessionFactory.openStatelessSession();
		}
		else {
			statelessSession = sessionFactory.withStatelessOptions()
					.tenantIdentifier( tenantId )
					.openStatelessSession();
		}
		return statelessSession;
	}

	/**
	 * Determines the index scope using the input parameters.
	 *
	 * @see IndexScope
	 */
	public static IndexScope getIndexScope(String hql, Set<Criterion> criterionSet) {
		if ( StringHelper.isNotEmpty( hql ) ) {
			return IndexScope.HQL;
		}
		else if ( criterionSet != null && criterionSet.size() > 0 ) {
			return IndexScope.CRITERIA;
		}
		else {
			return IndexScope.FULL_ENTITY;
		}
	}

}
