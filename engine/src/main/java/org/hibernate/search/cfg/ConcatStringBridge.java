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
package org.hibernate.search.cfg;

import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.StringBridge;

/**
 * @author Emmanuel Bernard
 */
public class ConcatStringBridge implements StringBridge, ParameterizedBridge{
	public static final String SIZE = "size";
	private int size;

	public String objectToString(Object object) {
		if (object == null) return "";
		if ( ! (object instanceof String) ) {
			throw new RuntimeException( "not a string" );
		}
		String string = object.toString();
		int maxSize = string.length() >= size ? size : string.length();
		return string.substring( 0, maxSize );
	}

	public void setParameterValues(Map<String,String> parameters) {
		size =  Integer.valueOf( parameters.get( SIZE ) );
	}
}
