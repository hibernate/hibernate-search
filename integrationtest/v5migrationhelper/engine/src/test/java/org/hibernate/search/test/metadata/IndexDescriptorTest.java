/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import java.util.Set;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-436")
public class IndexDescriptorTest {
	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		ConfigContext configContext = new ConfigContext(
				searchConfiguration,
				new BuildContextForTest( searchConfiguration )
		);
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testIndexInformation() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );

		Set<IndexDescriptor> indexDescriptors = typeDescriptor.getIndexDescriptors();
		assertEquals(
				"Wrong index name",
				DescriptorTestHelper.TEST_INDEX_NAMES.get( 0 ), indexDescriptors.iterator().next().getName()
		);
	}

	@Test
	public void testSharedIndexInformation() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( Foo.class, LuceneEmbeddedIndexManagerType.INSTANCE );
		IndexedTypeDescriptor typeDescriptor = new IndexedTypeDescriptorImpl(
				typeMetadata,
				DescriptorTestHelper.getDummyShardedIndexManager()
		);
		assertTrue( typeDescriptor.isSharded() );

		Set<IndexDescriptor> indexDescriptors = typeDescriptor.getIndexDescriptors();
		assertTrue( indexDescriptors.size() == 3 );
		for ( IndexDescriptor indexDescriptor : indexDescriptors ) {
			String shardName = indexDescriptor.getName();
			assertTrue(
					"Missing shard name: " + shardName,
					DescriptorTestHelper.TEST_INDEX_NAMES.contains( indexDescriptor.getName() )
			);
		}
	}
}


