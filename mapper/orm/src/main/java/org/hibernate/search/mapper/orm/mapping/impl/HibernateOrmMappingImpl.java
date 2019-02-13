/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchManagerImpl;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManager;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManagerBuilder;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoMappingWorkExecutor;
import org.hibernate.search.util.logging.impl.LoggerFactory;

public class HibernateOrmMappingImpl extends AbstractPojoMappingImplementor<HibernateOrmMapping>
		implements HibernateOrmMapping {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmMappingContextImpl mappingContext;

	HibernateOrmMappingImpl(PojoMappingDelegate mappingDelegate, SessionFactoryImplementor sessionFactoryImplementor) {
		super( mappingDelegate );
		this.mappingContext = new HibernateOrmMappingContextImpl( sessionFactoryImplementor );
	}

	@Override
	public HibernateOrmMapping toAPI() {
		return this;
	}

	@Override
	public HibernateOrmSearchManager createSearchManager(EntityManager entityManager) {
		return createSearchManagerBuilder( entityManager ).build();
	}

	@Override
	public HibernateOrmSearchManagerBuilder createSearchManagerWithOptions(EntityManager entityManager) {
		return createSearchManagerBuilder( entityManager );
	}

	@Override
	public boolean isWorkable(Class<?> type) {
		return getDelegate().isWorkable( type );
	}

	@Override
	public boolean isIndexable(Class<?> type) {
		return getDelegate().isIndexable( type );
	}

	@Override
	public boolean isSearchable(Class<?> type) {
		return getDelegate().isSearchable( type );
	}

	@Override
	public boolean isWorkable(Object entity) {
		return isWorkable( Hibernate.getClass( entity ) );
	}

	@Override
	public <E> Set<Class<? extends E>> getIndexedTypesPolymorphic(Class<E> entityType) {
		return getDelegate().getIndexedTypesPolymorphic( entityType );
	}

	@Override
	public PojoMappingWorkExecutor createMappingWorkExecutor() {
		return getDelegate().createMappingWorkExecutor();
	}

	private HibernateOrmSearchManagerBuilder createSearchManagerBuilder(EntityManager entityManager) {
		SessionImplementor sessionImplementor = entityManager.unwrap( SessionImplementor.class );

		SessionFactory expectedSessionFactory = mappingContext.getSessionFactory();
		SessionFactory givenSessionFactory = sessionImplementor.getSessionFactory();

		if ( !givenSessionFactory.equals( expectedSessionFactory ) ) {
			throw log.usingDifferentSessionFactories( expectedSessionFactory, givenSessionFactory );
		}

		return new HibernateOrmSearchManagerImpl.HibernateOrmSearchManagerBuilderImpl( getDelegate(), mappingContext, sessionImplementor );
	}
}
