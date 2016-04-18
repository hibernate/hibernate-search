/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata;

import static org.junit.Assert.assertNotNull;

import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.AbstractAnnotationMetadataTest;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-1442")
public class IndexedEmbeddedWithDepthAndIncludePathTest extends AbstractAnnotationMetadataTest {

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
