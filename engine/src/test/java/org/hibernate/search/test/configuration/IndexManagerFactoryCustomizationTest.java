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
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import junit.framework.Assert;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.impl.DefaultIndexManagerFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Test;

/**
 * Test to verify pluggability of an alternative IndexManagerFactory
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1211")
public class IndexManagerFactoryCustomizationTest {

	@Test
	public void testDefaultImplementation() {
		ManualConfiguration cfg = new ManualConfiguration();
		verifyIndexManagerTypeIs( DirectoryBasedIndexManager.class, cfg );
	}

	@Test
	public void testOverriddenDefaultImplementation() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.setIndexManagerFactory( new DefaultIndexManagerFactory() {
			@Override
			public IndexManager createDefaultIndexManager() {
				return new NRTIndexManager();
			}
		} );
		verifyIndexManagerTypeIs( NRTIndexManager.class, cfg );
	}

	private void verifyIndexManagerTypeIs(Class<? extends IndexManager> expectedIndexManagerClass, ManualConfiguration cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Document.class ).indexed().indexName( "documents" )
			.property( "id", ElementType.FIELD ).documentId()
			.property( "title", ElementType.FIELD ).field()
			;
		cfg.setProgrammaticMapping( mapping );
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Document.class );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		try {
			Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "documents" ) );
			// trigger a SearchFactory rebuild:
			sf.addClasses( Dvd.class );
			// and verify the option is not lost:
			Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "dvds" ) );
			Assert.assertEquals( expectedIndexManagerClass, extractDocumentIndexManagerClassName( sf, "documents" ) );
		}
		finally {
			sf.close();
		}
	}

	private Class<? extends IndexManager> extractDocumentIndexManagerClassName(SearchFactoryImplementor sf, String indexName) {
		IndexManager indexManager = sf.getIndexManagerHolder().getIndexManager( indexName );
		Assert.assertNotNull( indexManager );
		return indexManager.getClass();
	}

	public static final class Document {
		long id;
		String title;
	}

	@Indexed(index = "dvds")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
