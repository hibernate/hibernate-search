/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.context.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.mapper.session.context.SessionContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRuntimeIntrospector;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class HibernateOrmSessionContextImpl implements PojoSessionContextImplementor, HibernateOrmSessionContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * SessionImplementor.unwrap() is a bit dodgy: if you define your own interface that extends Session,
	 * for example, passing this interface to unwrap() will work, but will return a Session instance,
	 * obviously not an instance of your own type.
	 * <p>
	 * Let's avoid that kind of dodgy behavior by explicitly listing which types are supported.
	 */
	private static final Set<Class<? super SessionImplementor>> SUPPORTED_SESSION_IMPLEMENTOR_UNWRAP_CLASSES =
			CollectionHelper.toImmutableSet(
					CollectionHelper.<Class<? super SessionImplementor>>asLinkedHashSet(
							EntityManager.class,
							Session.class
					)
			);

	private final SessionImplementor sessionImplementor;

	private final HibernateOrmRuntimeIntrospector runtimeIntrospector;

	public HibernateOrmSessionContextImpl(SessionImplementor sessionImplementor) {
		this.sessionImplementor = sessionImplementor;
		this.runtimeIntrospector = new HibernateOrmRuntimeIntrospector( sessionImplementor );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( HibernateOrmSessionContext.class ) ) {
			return (T) this;
		}
		/*
		 * As a shortcut, we allow to unwrap directly to the Hibernate ORM session instead of
		 * having to write .unwrap( HibernateOrmSessionContext.class ).getSession().
		 */
		else if ( SUPPORTED_SESSION_IMPLEMENTOR_UNWRAP_CLASSES.contains( clazz ) ) {
			return (T) sessionImplementor;
		}
		throw log.sessionContextUnwrappingWithUnknownType( clazz, HibernateOrmSessionContext.class );
	}

	@Override
	public SessionContext toAPI() {
		return this;
	}

	@Override
	public String getTenantIdentifier() {
		return sessionImplementor.getTenantIdentifier();
	}

	@Override
	public PojoRuntimeIntrospector getRuntimeIntrospector() {
		return runtimeIntrospector;
	}

	@Override
	public Session getSession() {
		return sessionImplementor;
	}
}
