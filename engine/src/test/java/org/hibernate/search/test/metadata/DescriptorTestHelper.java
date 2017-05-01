/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;

/**
 * @author Hardy Ferentschik
 */
public final class DescriptorTestHelper {

	public static final List<String> TEST_INDEX_NAMES = Arrays.asList(
			"index-0", "index-0", "index-0" );

	private DescriptorTestHelper() {
		//not allowed
	}

	/**
	 * @deprecated use {@link #getTypeDescriptor(AnnotationMetadataProvider, IndexedTypeIdentifier)}
	 */
	@Deprecated
	public static IndexedTypeDescriptor getTypeDescriptor(AnnotationMetadataProvider metadataProvider, Class<?> clazz) {
		return getTypeDescriptor( metadataProvider, new PojoIndexedTypeIdentifier( clazz ) );
	}

	public static IndexedTypeDescriptor getTypeDescriptor(AnnotationMetadataProvider metadataProvider, IndexedTypeIdentifier type) {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( type, LuceneEmbeddedIndexManagerType.INSTANCE );
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
