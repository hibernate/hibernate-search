/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Member;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public final class MethodHandlePropertyHandle<T> implements PropertyHandle<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;
	private final Member member;
	private final MethodHandle getter;

	public MethodHandlePropertyHandle(String name, Member member, MethodHandle getter) {
		this.name = name;
		this.member = member;
		this.getter = getter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + member + "]";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public T get(Object thiz) {
		try {
			return (T) getter.invoke( thiz );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw log.errorInvokingMember( member, thiz, e );
		}
	}

	@Override
	public int hashCode() {
		return member.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		MethodHandlePropertyHandle<?> other = (MethodHandlePropertyHandle) obj;
		return name.equals( other.name ) && member.equals( other.member );
	}

}
