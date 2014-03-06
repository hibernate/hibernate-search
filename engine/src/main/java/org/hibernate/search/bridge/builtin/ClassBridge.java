/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.bridge.builtin;

import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * Convert a Class back and forth
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
