/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.entity.impl;

import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.genericjpa.entity.EntityManagerEntityProvider;
import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;

/**
 * @author Martin Braun
 */
public class JPAReusableEntityProvider extends TransactionWrappedReusableEntityProvider
		implements ReusableEntityProvider {

	private final Map<Class<?>, String> idProperties;
	private final Map<Class<?>, EntityManagerEntityProvider> customEntityProviders;
	private BasicEntityProvider provider;

	public JPAReusableEntityProvider(
			EntityManagerFactory emf,
			Map<Class<?>, String> idProperties) {
		this( emf, idProperties, null, new HashMap<>() );
	}

	public JPAReusableEntityProvider(
			EntityManagerFactory emf,
			Map<Class<?>, String> idProperties,
			TransactionManager transactionManager,
			Map<Class<?>, EntityManagerEntityProvider> customEntityProviders) {
		super( emf, transactionManager );
		this.idProperties = idProperties;
		this.customEntityProviders = customEntityProviders;
	}

	@Override
	public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
		if ( this.provider == null ) {
			throw new IllegalStateException( "not open!" );
		}
		if ( this.customEntityProviders.containsKey( entityClass ) ) {
			return this.customEntityProviders.get( entityClass ).get( this.getEntityManager(), entityClass, id, hints );
		}
		return this.provider.get( entityClass, id );
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List getBatch(Class<?> entityClass, List<Object> ids, Map<String, Object> hints) {
		if ( this.provider == null ) {
			throw new IllegalStateException( "not open!" );
		}
		if ( this.customEntityProviders.containsKey( entityClass ) ) {
			return this.customEntityProviders.get( entityClass ).getBatch(
					this.getEntityManager(),
					entityClass,
					ids,
					hints
			);
		}
		return this.provider.getBatch( entityClass, ids );
	}

	@Override
	public void close() {
		super.close();
		this.provider = null;
	}

	@Override
	public void open() {
		super.open();
		this.provider = new BasicEntityProvider( this.getEntityManager(), this.idProperties );
	}

}
