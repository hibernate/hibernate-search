/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;

/**
 * Created by Martin on 14.11.2015.
 */
public class ORMReusableEntityProvider implements ReusableEntityProvider {

	private static final String QUERY_FORMAT = "SELECT obj FROM %s obj " + "WHERE obj.%s IN :ids";
	private final Map<Class<?>, String> idProperties;

	private final SessionFactory sessionFactory;
	private Session session;

	public ORMReusableEntityProvider(SessionFactory sessionFactory, Map<Class<?>, String> idProperties) {
		this.sessionFactory = sessionFactory;
		this.idProperties = idProperties;
	}

	@Override
	public void close() {
		if ( this.session == null ) {
			throw new AssertionFailure( "ORMReusableEntityProvider was not open!" );
		}
		this.session.close();
	}

	@Override
	public void open() {
		if ( this.session != null ) {
			throw new AssertionFailure( "ORMReusableEntityProvider was already open!" );
		}
		this.session = this.sessionFactory.openSession();
	}

	@Override
	public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
		return this.session.get( entityClass, (Serializable) id );
	}

	@Override
	public List getBatch(
			Class<?> entityClass, List<Object> ids, Map<String, Object> hints) {
		List<Object> ret = new ArrayList<>( ids.size() );
		if ( ids.size() > 0 ) {
			String idProperty = this.idProperties.get( entityClass );
			String queryString = String.format(
					(Locale) null,
					QUERY_FORMAT,
					this.session.getSessionFactory().getTypeHelper().entity( entityClass ).getName(),
					idProperty
			);
			Query query = this.session.createQuery( queryString );
			query.setParameter( "ids", ids );
			ret.addAll( query.list() );
		}
		return ret;
	}
}
