/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
