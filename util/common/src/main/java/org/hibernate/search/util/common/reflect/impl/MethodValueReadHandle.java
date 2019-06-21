/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public final class MethodValueReadHandle<T> implements ValueReadHandle<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Method method;

	public MethodValueReadHandle(Method method) {
		this.method = method;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + method + "]";
	}

	@Override
	public T get(Object thiz) {
		try {
			return (T) method.invoke( thiz );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw log.errorInvokingMember( method, thiz, e );
		}
	}

	@Override
	public int hashCode() {
		return method.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		MethodValueReadHandle<?> other = (MethodValueReadHandle) obj;
		return method.equals( other.method );
	}

}
