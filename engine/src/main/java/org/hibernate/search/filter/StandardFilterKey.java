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
package org.hibernate.search.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a filter key using all injected parameters to compute
 * equals and hashCode
 * the order the parameters are added is significant
 *
 * @author Emmanuel Bernard
 */
public class StandardFilterKey extends FilterKey {
	private final List<Object> parameters = new ArrayList<Object>();
	private boolean implSet;

	@Override
	public void setImpl(Class<?> impl) {
		super.setImpl( impl );
		//add impl once and only once
		if ( implSet ) {
			parameters.set( 0, impl );
		}
		else {
			implSet = true;
			parameters.add( 0, impl );
		}
	}

	public void addParameter(Object value) {
		parameters.add( value );
	}

	@Override
	public int hashCode() {
		int hash = 23;
		for ( Object param : parameters ) {
			hash = 31 * hash + ( param != null ? param.hashCode() : 0 );
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof StandardFilterKey ) ) {
			return false;
		}
		StandardFilterKey that = (StandardFilterKey) obj;
		int size = parameters.size();
		if ( size != that.parameters.size() ) {
			return false;
		}
		for ( int index = 0; index < size; index++ ) {
			Object paramThis = parameters.get( index );
			Object paramThat = that.parameters.get( index );
			if ( paramThis == null && paramThat != null ) {
				return false;
			}
			if ( paramThis != null && !paramThis.equals( paramThat ) ) {
				return false;
			}
		}
		return true;
	}
}
