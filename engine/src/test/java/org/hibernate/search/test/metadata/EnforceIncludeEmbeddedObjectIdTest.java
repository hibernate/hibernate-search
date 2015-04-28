/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata;

import static org.junit.Assert.assertTrue;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Braun
 */
@TestForIssue(jiraKey = "HSEARCH-1861")
public class EnforceIncludeEmbeddedObjectIdTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		SearchConfigurationForTest searchConfiguration = new SearchConfigurationForTest();
		ConfigContext configContext = new ConfigContext( searchConfiguration, new BuildContextForTest( searchConfiguration ) );
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext, true );
	}

	@Test
	public void testEnforcedIncludeEmbeddedObjectId() {
		TypeMetadata rootTypeMetadata = metadataProvider.getTypeMetadataFor( IndexedEmbeddedTestEntity.class );
		this.assertRecursive( rootTypeMetadata );
	}

	private void assertRecursive(TypeMetadata curParentMetadata) {
		for ( EmbeddedTypeMetadata curChildMetadata : curParentMetadata.getEmbeddedTypeMetadata() ) {
			boolean foundOne = false;
			for ( DocumentFieldMetadata docFieldMeta : curChildMetadata.getAllDocumentFieldMetadata() ) {
				if ( docFieldMeta.isIdInEmbedded() ) {
					foundOne = true;
				}
			}
			assertTrue( foundOne || curChildMetadata.getAllDocumentFieldMetadata().size() == 0 );
			this.assertRecursive( curChildMetadata );
		}
	}

}
