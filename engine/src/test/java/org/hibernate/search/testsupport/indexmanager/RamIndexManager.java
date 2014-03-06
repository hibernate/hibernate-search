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
package org.hibernate.search.testsupport.indexmanager;

import java.util.Properties;

import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.WorkerBuildContextForTest;

/**
 * At this point mainly used for tests
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class RamIndexManager extends DirectoryBasedIndexManager {

	public static RamIndexManager makeRamDirectory() {
		RamIndexManager ramIndexManager = new RamIndexManager();
		Properties properties = new Properties();
		properties.setProperty( "directory_provider", "ram" );
		ramIndexManager.initialize(
				"testIndex",
				properties,
				new DefaultSimilarity(),
				new WorkerBuildContextForTest( new SearchConfigurationForTest() )
		);
		return ramIndexManager;
	}
}
