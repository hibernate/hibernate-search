/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the configured IndexManager implementation is used for each index
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class IndexManagerOverrideTest {

	@Test
	public void verifyIndexExclusivity() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSession ftSession = builder
			.setProperty( "hibernate.search.Book.indexmanager", "near-real-time" )
			.setProperty( "hibernate.search.org.hibernate.search.test.perf.Boat.indexmanager",
					"org.hibernate.search.test.util.RamIndexManager" )
			.addAnnotatedClass( BlogEntry.class )
			.addAnnotatedClass( org.hibernate.search.test.perf.Boat.class )
			.addAnnotatedClass( org.hibernate.search.test.query.Book.class )
			.addAnnotatedClass( org.hibernate.search.test.query.Author.class )
			.openFullTextSession();
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) ftSession.getSearchFactory();
		ftSession.close();
		IndexManagerHolder allIndexesManager = searchFactory.getAllIndexesManager();

		//checks for the default implementation
		checkIndexManagerType( allIndexesManager, "org.hibernate.search.test.configuration.BlogEntry",
				org.hibernate.search.indexes.impl.DirectoryBasedIndexManager.class );

		//Uses "NRT" taken from shortcut names
		checkIndexManagerType( allIndexesManager, "Book",
				org.hibernate.search.indexes.impl.NRTIndexManager.class );

		//Uses a fully qualified name to load an implementation
		checkIndexManagerType( allIndexesManager, "org.hibernate.search.test.perf.Boat",
				org.hibernate.search.test.util.RamIndexManager.class );

		builder.close();
	}

	private void checkIndexManagerType(IndexManagerHolder allIndexesManager, String name, Class expectedType) {
		IndexManager indexManager = allIndexesManager.getIndexManager( name );
		Assert.assertEquals( expectedType, indexManager.getClass() );
	}

}
