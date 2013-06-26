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
import java.util.Set;

import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;

/**
 * Dummy descriptor for an unindexed type
 *
 * @author Hardy Ferentschik
 */
public class IndexedTypeDescriptorForUnindexedType implements IndexedTypeDescriptor {
	public static final IndexedTypeDescriptor INSTANCE = new IndexedTypeDescriptorForUnindexedType();

	private IndexedTypeDescriptorForUnindexedType() {
	}

	@Override
	public boolean isIndexed() {
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
	public IndexDescriptor getIndexDescriptor() {
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
}


