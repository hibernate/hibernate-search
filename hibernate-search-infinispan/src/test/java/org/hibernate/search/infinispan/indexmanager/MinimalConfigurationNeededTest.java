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
package org.hibernate.search.infinispan.indexmanager;

import junit.framework.Assert;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.infinispan.impl.indexmanager.InfinispanIndexManager;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Test;

/**
 * This test has a dual function:
 * - To verify just a single property is enough to get going with Infinispan
 * - Is testing the default Infinispan configuration which we include in the jar
 * 
 * So it's a good idea to not tune this particular test to avoid stating the networking stack.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-882")
public class MinimalConfigurationNeededTest {

	@Test
	public void configurationTest() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addClass( Dvd.class );
		cfg.addProperty( "hibernate.search.default.indexmanager", "infinispan" );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		try {
			IndexManager indexManager = sf.getAllIndexesManager().getIndexManager( "dvds" );
			Assert.assertNotNull( indexManager );
			Assert.assertEquals( InfinispanIndexManager.class, indexManager.getClass() );
		}
		finally {
			sf.close();
		}
	}

	@Indexed(index="dvds")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
