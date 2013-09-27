/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.util;

import java.lang.reflect.Method;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * Hibernate ORM 4.3 changed some internal methods names compared to 4.2 which we use
 * in the testsuite only, so rather than bothering with backwards compatibility in ORM
 * we can workaround the problem with some reflection.
 */
public final class ServiceRegistryTools {

	private static final Method builderMethod = identifyCorrectBuilderMethod();

	private ServiceRegistryTools() {
		//not allowed
	}

	public static ServiceRegistry build(ServiceRegistryBuilder registryBuilder) {
		try {
			return (ServiceRegistry) builderMethod.invoke( registryBuilder );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static Method identifyCorrectBuilderMethod() {
		try {
			return ServiceRegistryBuilder.class.getMethod( "build" );
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
		catch (NoSuchMethodException e) {
			try {
				return ServiceRegistryBuilder.class.getMethod( "buildServiceRegistry" );
			}
			catch (SecurityException e1) {
				e1.printStackTrace();
			}
			catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			}
		}
		return null; //better to allow NPE than to debug a class initialization failure
	}

}
