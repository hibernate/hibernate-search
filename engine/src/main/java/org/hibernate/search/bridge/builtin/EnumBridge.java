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

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;


/**
 * Map an Enum field
 *
 * @author Sylvain Vieujot
 */
public class EnumBridge implements TwoWayStringBridge, AppliedOnTypeAwareBridge {

	private Class<? extends Enum> clazz = null;

	@Override
	public Enum<? extends Enum> stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		return Enum.valueOf( clazz, stringValue );
	}

	@Override
	public String objectToString(Object object) {
		Enum e = (Enum) object;
		return e != null ? e.name() : null;
	}

	@Override
	public void setAppliedOnType(Class<?> returnType) {
		@SuppressWarnings("unchecked") //only called for an enum
		Class<? extends Enum> enumReturnType = (Class<? extends Enum>) returnType;
		this.clazz = enumReturnType;
	}
}
