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

package org.hibernate.search.test.metadata;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;

/**
 * @author Hardy Ferentschik
 */
public final class DescriptorTestHelper {

	public static final List<String> TEST_INDEX_NAMES = Arrays.asList(
			"index-0", "index-0", "index-0" );

	private DescriptorTestHelper() {
		//not allowed
	}

	public static IndexedTypeDescriptor getTypeDescriptor(AnnotationMetadataProvider metadataProvider, Class<?> clazz) {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( clazz );
		return new IndexedTypeDescriptorImpl(
				typeMetadata,
				getDummyUnShardedIndexManager()
		);
	}

	public static IndexManager[] getDummyUnShardedIndexManager() {
		IndexManager[] managers = new IndexManager[1];
		managers[0] = new DummyIndexManager( TEST_INDEX_NAMES.get( 0 ) );
		return managers;
	}

	public static IndexManager[] getDummyShardedIndexManager() {
		IndexManager[] managers = new IndexManager[3];
		int i = 0;
		for ( String indexName : TEST_INDEX_NAMES ) {
			managers[i] = new DummyIndexManager( indexName );
			i++;
		}
		return managers;
	}
}
