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
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Padding Integer bridge.
 * All numbers will be padded with 0 to match 5 digits
 *
 * @author Emmanuel Bernard
 */
public class PaddedIntegerBridge implements TwoWayStringBridge, ParameterizedBridge {

	public static final String PADDING_PROPERTY = "padding";

	private int padding = 5; //default

	@Override
	public void setParameterValues(Map<String,String> parameters) {
		String padding = parameters.get( PADDING_PROPERTY );
		if ( padding != null ) {
			this.padding = Integer.parseInt( padding );
		}
	}

	@Override
	public String objectToString(Object object) {
		String rawInteger = object.toString();
		if ( rawInteger.length() > padding ) {
			throw new IllegalArgumentException( "Try to pad on a number too big" );
		}
		StringBuilder paddedInteger = new StringBuilder();
		for ( int padIndex = rawInteger.length(); padIndex < padding; padIndex++ ) {
			paddedInteger.append( '0' );
		}
		return paddedInteger.append( rawInteger ).toString();
	}

	@Override
	public Object stringToObject(String stringValue) {
		return new Integer( stringValue );
	}

}
