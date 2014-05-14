/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metadata.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;

/**
 * Dummy descriptor for an unindexed type
 *
 * @author Hardy Ferentschik
 */
public class IndexedTypeDescriptorForUnindexedType implements IndexedTypeDescriptor {
	private final Class<?> type;

	public IndexedTypeDescriptorForUnindexedType(Class<?> type) {
		this.type = type;
	}

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public boolean isIndexed() {
		return false;
	}

	@Override
	public boolean isSharded() {
		return false;
	}

	@Override
	public float getStaticBoost() {
		return 1;
	}

	@Override
	public BoostStrategy getDynamicBoost() {
		return DefaultBoostStrategy.INSTANCE;
	}

	@Override
	public Set<IndexDescriptor> getIndexDescriptors() {
		return Collections.emptySet();
	}

	@Override
	public Set<PropertyDescriptor> getIndexedProperties() {
		return Collections.emptySet();
	}

	@Override
	public PropertyDescriptor getProperty(String propertyName) {
		return null;
	}

	@Override
	public Set<FieldDescriptor> getIndexedFields() {
		return Collections.emptySet();
	}

	@Override
	public FieldDescriptor getIndexedField(String fieldName) {
		return null;
	}

	@Override
	public Set<FieldDescriptor> getFieldsForProperty(String propertyName) {
		return Collections.emptySet();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexedTypeDescriptorForUnindexedType{" );
		sb.append( "type=" ).append( type );
		sb.append( '}' );
		return sb.toString();
	}
}


