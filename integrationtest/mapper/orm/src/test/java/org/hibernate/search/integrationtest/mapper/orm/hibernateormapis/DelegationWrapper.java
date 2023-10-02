/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class DelegationWrapper implements InvocationHandler, Serializable {
	Object realSession;

	public DelegationWrapper(Session session) {
		this.realSession = session;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke( realSession, args );
		}
		catch (InvocationTargetException e) {
			if ( e.getTargetException() instanceof RuntimeException ) {
				throw (RuntimeException) e.getTargetException();
			}
			else {
				throw e;
			}
		}
	}
}
