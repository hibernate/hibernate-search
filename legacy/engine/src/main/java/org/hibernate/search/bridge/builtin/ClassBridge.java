/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * Bridge a {@link Class} to a {@link String}.
 *
 * @author Emmanuel Bernard
 */
public class ClassBridge implements TwoWayStringBridge {

	private final ServiceManager serviceManager;

	public ClassBridge(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		else {
			try {
				return ClassLoaderHelper.classForName( stringValue, serviceManager );
			}
			catch (ClassLoadingException e) {
				throw new SearchException( "Unable to deserialize Class: " + stringValue, e );
			}
		}
	}

	@Override
	public String objectToString(Object object) {
		return object == null ? null : ( (Class) object ).getName();
	}
}
