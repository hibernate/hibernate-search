/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoRawTypeIdentifierResolver;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class HibernateOrmRuntimeIntrospector implements PojoRuntimeIntrospector {

	private final PojoRawTypeIdentifierResolver typeIdentifierResolver;
	private final SharedSessionContractImplementor sessionImplementor;

	public HibernateOrmRuntimeIntrospector(PojoRawTypeIdentifierResolver typeIdentifierResolver,
			SharedSessionContractImplementor sessionImplementor) {
		this.typeIdentifierResolver = typeIdentifierResolver;
		this.sessionImplementor = sessionImplementor;
	}

	@Override
	// As long as T is the declared type of an entity or one of its supertypes, this cast is safe
	@SuppressWarnings("unchecked")
	public <T> PojoRawTypeIdentifier<? extends T> detectEntityType(T entity) {
		if ( entity == null ) {
			return null;
		}
		String entityName = sessionImplementor.bestGuessEntityName( entity );
		if ( entityName == null ) {
			return null;
		}
		// bestGuessEntityName(...) returns a Hibernate ORM "native" entity name,
		// and that's what we call "secondary" entity names in Hibernate Search
		// (the primary name is the JPA entity name).
		return (PojoRawTypeIdentifier<? extends T>) typeIdentifierResolver.typeIdentifierBySecondaryEntityName()
				.getOrNull( entityName );
	}

	@Override
	public Object unproxy(Object value) {
		if ( value instanceof HibernateProxy ) {
			final HibernateProxy proxy = (HibernateProxy) value;
			final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
			Object initialized = lazyInitializer.getImplementation( sessionImplementor );
			if ( initialized != null ) {
				return initialized;
			}
			else {
				// This is the case in which the proxy was created by a different session.
				// unproxyAndReassociate is the ultimate bomb,
				// able to deal with a Session change:
				return sessionImplementor.getPersistenceContext().unproxyAndReassociate( proxy );
			}
		}
		return value;
	}

	@Override
	public boolean isIgnorableDataAccessThrowable(Throwable throwable) {
		// Ideally we would only need to ignore LazyInitializationException,
		// but we have to work around HHH-14811 somehow,
		// and there are other situations where lazy loading with bytecode enhancement enabled lead to
		// a plain HibernateException (not a particular subclass).
		// So this addresses HibernateException, LazyInitializationException, AssertionFailure.
		return throwable instanceof HibernateException
				// Ideally this shouldn't be needed, but we have to work around HHH-14811 somehow
				// See https://hibernate.atlassian.net/browse/HHH-14811
				|| throwable instanceof AssertionFailure;
	}
}
