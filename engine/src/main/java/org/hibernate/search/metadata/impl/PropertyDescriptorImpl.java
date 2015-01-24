/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
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


