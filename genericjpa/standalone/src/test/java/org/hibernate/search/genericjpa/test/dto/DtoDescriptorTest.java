/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.dto;

import org.hibernate.search.genericjpa.dto.impl.DtoDescriptor;
import org.hibernate.search.genericjpa.dto.impl.DtoDescriptor.DtoDescription;
import org.hibernate.search.genericjpa.dto.impl.DtoDescriptorImpl;
import org.hibernate.search.genericjpa.annotations.DtoField;
import org.hibernate.search.genericjpa.annotations.DtoOverEntity;

import junit.framework.TestCase;

public class DtoDescriptorTest extends TestCase {

	public void testDescriptor() {
		DtoDescriptor descriptor = new DtoDescriptorImpl();
		DtoDescription description = descriptor.getDtoDescription( A.class );
		assertEquals( A.class, description.getDtoClass() );
		assertEquals( B.class, description.getEntityClass() );
		assertEquals( 1, description.getFieldDescriptionsForProfile( "toast" ).size() );
		assertEquals(
				"toastFieldName",
				description.getFieldDescriptionsForProfile( "toast" ).iterator().next().getFieldName()
		);
		assertEquals( 2, description.getFieldDescriptionsForProfile( DtoDescription.DEFAULT_PROFILE ).size() );

		int found = 0;
		for ( DtoDescription.FieldDescription fDesc : description.getFieldDescriptionsForProfile( DtoDescription.DEFAULT_PROFILE ) ) {
			if ( "fieldOne".equals( fDesc.getFieldName() ) ) {
				++found;
			}
			else if ( "fieldTwo".equals( fDesc.getFieldName() ) ) {
				++found;
			}
		}
		if ( found != 2 ) {
			fail( "the default profile for " + A.class + " should have 2 different FieldDescriptions" );
		}

		try {
			descriptor.getDtoDescription( C.class );
			fail( "invalid description with two fieldnames annotated to one field in the same profile" + " should yield an exception" );
		}
		catch (IllegalArgumentException e) {

		}
	}

	// the value of entityClass isn't that important in this test
	// but we want to check if it's set properly in the resulting
	// DtoDescription
	@DtoOverEntity(entityClass = B.class)
	public static class A {

		@DtoField(fieldName = "toastFieldName", profileName = "toast")
		@DtoField
		String fieldOne;

		@DtoField
		String fieldTwo;

	}

	public static class B {

	}

	@DtoOverEntity(entityClass = C.class)
	public static class C {

		// this should be the reason for an exception
		// when used with the Descriptor
		@DtoField
		@DtoField
		String field;

	}
}
