/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.programmaticmapping;

import java.lang.annotation.ElementType;

import junit.framework.Assert;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.test.util.SearchFactoryHolder;
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
		SearchFactoryImplementor searchFactory = sfHolder.getSearchFactory();

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
