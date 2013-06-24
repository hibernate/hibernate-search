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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;

/**
 * @author Hardy Ferentschik
 */
public class IndexedTypeDescriptorImpl implements IndexedTypeDescriptor {
	private final float classBoost;
	private final BoostStrategy boostStrategy;
	private final Set<FieldDescriptor> fieldDescriptors;
	private final IndexDescriptor indexDescriptor;

	public IndexedTypeDescriptorImpl(TypeMetadata typeMetadata, IndexManager[] indexManagers) {
		this.classBoost = typeMetadata.getStaticBoost();
		this.boostStrategy = typeMetadata.getDynamicBoost();

		this.fieldDescriptors = new HashSet<FieldDescriptor>();
		for ( String fieldName : typeMetadata.getAllFieldNames() ) {
			DocumentFieldMetadata documentFieldMetadata = typeMetadata.getDocumentFieldMetadataFor( fieldName );
			FieldDescriptor fieldDescriptor = new FieldDescriptorImpl( documentFieldMetadata );
			fieldDescriptors.add( fieldDescriptor );
		}

		indexDescriptor = new IndexDescriptorImpl( indexManagers );
	}

	@Override
	public boolean isIndexed() {
		return true;
	}

	@Override
	public float getStaticBoost() {
		return classBoost;
	}

	@Override
	public BoostStrategy getDynamicBoost() {
		return boostStrategy;
	}

	@Override
	public IndexDescriptor getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public Set<FieldDescriptor> getIndexedFields() {
		return fieldDescriptors;
	}

	@Override
	public FieldDescriptor getIndexedField(String fieldName) {
		for ( FieldDescriptor fieldDescriptor : fieldDescriptors ) {
			if ( fieldDescriptor.getName().equals( fieldName ) ) {
				return fieldDescriptor;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "IndexedTypeDescriptorImpl{" );
		sb.append( "classBoost=" ).append( classBoost );
		sb.append( ", boostStrategy=" ).append( boostStrategy );
		sb.append( ", fieldDescriptors=" ).append( fieldDescriptors );
		sb.append( ", indexDescriptor=" ).append( indexDescriptor );
		sb.append( '}' );
		return sb.toString();
	}
}


