/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.testsupport.TestForIssue;

import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HSEARCH-1442")
public class IndexedEmbeddedWithDepthAndIncludePathTest {

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
	public void testDepthIsProperlyHandled() {
		TypeMetadata rootTypeMetadata = metadataProvider
				.getTypeMetadataFor( IndexedEmbeddedTestEntity.class );

		EmbeddedTypeMetadata embeddedWithDepthTypeMetadata = null;
		for ( EmbeddedTypeMetadata typeMetadata : rootTypeMetadata.getEmbeddedTypeMetadata() ) {
			if ( "indexedEmbeddedWithDepth".equals( typeMetadata.getEmbeddedFieldName() ) ) {
				embeddedWithDepthTypeMetadata = typeMetadata;
			}
		}

		assertNotNull( embeddedWithDepthTypeMetadata );
		assertNotNull( embeddedWithDepthTypeMetadata.getPropertyMetadataForProperty( "name" ) );
	}

}
