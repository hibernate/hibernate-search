/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.programmaticmapping;

import java.lang.annotation.ElementType;

import org.junit.Assert;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;

/**
 * To verify a new instance of a SearchFactory is still able to deal with programmatic mapping
 * such as defined by SearchMapping @see {@link SearchMapping}, both initially and after triggering
 * an internal mutation of a MutableSearchFactory.
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public class MutatingSearchFactoryTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( buildMappingDefinition() );

	@Test
	public void mutationTest() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();

		Assert.assertNull( searchFactory.getIndexManagerHolder().getIndexManager( "phoneNumbersIndex" ) );
		searchFactory.addClasses( TelephoneRecord.class );
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "phoneNumbersIndex" ) );

		Assert.assertNull( searchFactory.getIndexManagerHolder().getIndexManager( "addressBookIndex" ) );
		searchFactory.addClasses( AddressBook.class );
		Assert.assertNotNull( searchFactory.getIndexManagerHolder().getIndexManager( "addressBookIndex" ) );
	}

	static SearchMapping buildMappingDefinition() {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( TelephoneRecord.class )
				.indexed()
					.indexName( "phoneNumbersIndex" )
				.property( "id", ElementType.FIELD ).documentId()
				.property( "phone", ElementType.FIELD ).field().analyze( Analyze.NO ).store( Store.YES )
			.entity( AddressBook.class )
				.indexed()
					.indexName( "addressBookIndex" )
				.property( "id", ElementType.FIELD ).documentId()
				.property( "name", ElementType.FIELD ).field().store( Store.YES )
			;
		return mapping;
	}

	public static final class TelephoneRecord {

		private long id;
		private String phone;

		public TelephoneRecord(long id, String phone) {
			this.id = id;
			this.phone = phone;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getPhone() {
			return phone;
		}

		public void setPhone(String phone) {
			this.phone = phone;
		}

	}

	public static final class AddressBook {

		private long id;
		private String name;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
