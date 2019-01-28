/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class FieldPropertyHandle implements PropertyHandle {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;
	private final Field field;

	public FieldPropertyHandle(String name, Field field) {
		this.name = name;
		this.field = field;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + field + "]";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object get(Object thiz) {
		try {
			return field.get( thiz );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw log.errorInvokingMember( field, thiz, e );
		}
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || !obj.getClass().equals( getClass() ) ) {
			return false;
		}
		FieldPropertyHandle other = (FieldPropertyHandle) obj;
		return name.equals( other.name ) && field.equals( other.field );
	}

}
