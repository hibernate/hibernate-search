/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.metadata.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;

/**
 * @author Hardy Ferentschik
 */
public class PropertyDescriptorImpl implements PropertyDescriptor {
	private final String name;
	private final Set<FieldDescriptor> fieldDescriptors;
	private final boolean id;

	public PropertyDescriptorImpl(String name, boolean id, Set<FieldDescriptor> fieldDescriptors) {
		this.name = name;
		this.fieldDescriptors = Collections.unmodifiableSet( new HashSet<FieldDescriptor>( fieldDescriptors ) );
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isId() {
		return id;
	}

	@Override
	public Set<FieldDescriptor> getIndexedFields() {
		return fieldDescriptors;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		PropertyDescriptorImpl that = (PropertyDescriptorImpl) o;

		if ( !name.equals( that.name ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "PropertyDescriptorImpl{" );
		sb.append( "name='" ).append( name ).append( '\'' );
		sb.append( ", fieldDescriptors=" ).append( fieldDescriptors );
		sb.append( ", id=" ).append( id );
		sb.append( '}' );
		return sb.toString();
	}
}


