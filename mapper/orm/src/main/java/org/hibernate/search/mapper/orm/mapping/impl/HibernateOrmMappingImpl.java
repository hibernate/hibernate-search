/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchManager;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingImpl;

public class HibernateOrmMappingImpl extends PojoMappingImpl
		implements HibernateOrmMapping {

	private final SessionFactoryImplementor sessionFactoryImplementor;

	HibernateOrmMappingImpl(PojoMappingDelegate mappingDelegate, SessionFactoryImplementor sessionFactoryImplementor) {
		super( mappingDelegate );
		this.sessionFactoryImplementor = sessionFactoryImplementor;
	}

	@Override
	public HibernateOrmSearchManager createSearchManager(EntityManager entityManager) {
		return createSearchManagerBuilder( entityManager ).build();
	}

	@Override
	public HibernateOrmSearchManagerBuilder createSearchManagerWithOptions(EntityManager entityManager) {
		return createSearchManagerBuilder( entityManager );
	}

	private HibernateOrmSearchManagerBuilder createSearchManagerBuilder(EntityManager entityManager) {
		SessionImplementor sessionImplementor = entityManager.unwrap( SessionImplementor.class );
		// TODO check that the session refers to the same session factory used when building the mapping
		return new HibernateOrmSearchManagerImpl.Builder( getDelegate(), sessionImplementor );
	}
}
