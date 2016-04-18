/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.AbstractAnnotationMetadataTest;
import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1759")
public class FieldConfigurationTest extends AbstractAnnotationMetadataTest {

	@Test
	public void testFieldAnnotationTargetingSameFieldAsDocumentIdIsNotAllowed() {
		try {
			metadataProvider.getTypeMetadataFor( Qux.class );
			fail( "Invalid configuration should have failed" );

		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000247" ) );
		}
	}

	@Test
	public void testFieldAnnotationAddsAdditionalFieldForIdProperty() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Quux.class );
		PropertyDescriptor propertyDescriptor = typeDescriptor.getProperty( "id" );
		assertEquals( "Unexpected number of fields", 2, propertyDescriptor.getIndexedFields().size() );
	}
}
