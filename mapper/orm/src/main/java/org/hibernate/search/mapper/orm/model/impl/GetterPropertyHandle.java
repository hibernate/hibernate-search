/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public final class GetterPropertyHandle implements PropertyHandle {

	private final Getter getter;

	public GetterPropertyHandle(Getter getter) {
		this.getter = getter;
	}

	@Override
	public Class<?> getType() {
		return getter.getReturnType();
	}

	@Override
	public Object get(Object thiz) {
		try {
			return getter.get( thiz );
		}
		catch (Error e) {
			throw e;
		}
		catch (Throwable e) {
			if ( e instanceof InterruptedException ) {
				Thread.currentThread().interrupt();
			}
			throw new SearchException( "Exception while invoking '" + getter + "' on '" + thiz + "'" , e );
		}
	}

}
