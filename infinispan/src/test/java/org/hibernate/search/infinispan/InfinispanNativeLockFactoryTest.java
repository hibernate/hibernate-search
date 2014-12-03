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
package org.hibernate.search.infinispan;

import java.io.File;
import java.io.IOException;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.TestForIssue;
import org.hibernate.search.util.impl.FileHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the infinispan directory works with a file based lock factory
 *
 * @author gustavonalle
 */
@TestForIssue(jiraKey = "HSEARCH-1737")
public class InfinispanNativeLockFactoryTest {

	private static File indexBase;

	@BeforeClass
	public static void setup() throws IOException {
		indexBase = new File( System.getProperty( "java.io.tmpdir" ), "index" );
		indexBase.mkdir();
	}

	@AfterClass
	public static void tearDown() throws IOException {
		FileHelper.delete( indexBase );
	}

	@Rule
	public SearchFactoryHolder holder = new SearchFactoryHolder( InfinispanLockFactoryOptionsTest.BookTypeZero.class )
			.withProperty( "hibernate.search.default.directory_provider", "infinispan" )
			.withProperty( "hibernate.search.default.locking_strategy", "native" )
			.withProperty( "hibernate.search.default.indexBase", indexBase.getAbsolutePath() );

	@Test
	public void verifyLockCreated() {
		IndexManager indexManager = holder.getSearchFactory().getIndexManagerHolder().getIndexManager( "INDEX0" );
		assertNotNull( indexManager );
	}

}
