/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * We already have plenty of tests verifying the parsing of configuration properties,
 * so this test actually verifies that the property is also being applied on the
 * IndexWriter.
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1508")
public class IndexWriterTuningAppliedTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Dvd.class, Book.class )
		.withProperty( "hibernate.search.default.indexwriter.max_thread_states", "23" )
		.withProperty( "hibernate.search.index2.indexwriter.max_thread_states", "7" );

	@Test
	public void testIndexWriterTuningApplied() throws IOException {
		AbstractWorkspaceImpl dvdsWorkspace = sfHolder.extractWorkspace( Dvd.class );
		IndexWriter dvdsIndexWriter = dvdsWorkspace.getIndexWriter();
		try {
			Assert.assertEquals( 23, dvdsIndexWriter.getConfig().getMaxThreadStates() );
		}
		finally {
			dvdsIndexWriter.close( false );
		}
	}

	@Test
	public void testIndexWriterTuningAppliedOnDefault() throws IOException {
		AbstractWorkspaceImpl booksWorkspace = sfHolder.extractWorkspace( Book.class );
		IndexWriter booksIndexWriter = booksWorkspace.getIndexWriter();
		try {
			Assert.assertEquals( 7, booksIndexWriter.getConfig().getMaxThreadStates() );
		}
		finally {
			booksIndexWriter.close( false );
		}
	}

	@Indexed(index = "index1")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

	@Indexed(index = "index2")
	public static final class Book {
		@DocumentId long id;
		@Field String title;
	}

}
