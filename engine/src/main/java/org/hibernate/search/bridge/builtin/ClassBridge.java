/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Convert a Class back and forth
 *
 * @author Emmanuel Bernard
 */
public class ClassBridge implements TwoWayStringBridge {

	private final ClassLoaderService classLoaderService;

	public ClassBridge(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		else {
			try {
				return classLoaderService.classForName( stringValue );
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
