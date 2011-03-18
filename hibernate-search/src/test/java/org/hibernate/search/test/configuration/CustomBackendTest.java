/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.search.FullTextSession;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sanne Grinovero
 */
public class CustomBackendTest {
	
	@Test
	public void test() {
		verifyBackendUsage( "blackhole", BlackHoleBackendQueueProcessorFactory.class );
		verifyBackendUsage( "lucene", LuceneBackendQueueProcessorFactory.class );
		verifyBackendUsage( BlackHoleBackendQueueProcessorFactory.class );
		verifyBackendUsage( LuceneBackendQueueProcessorFactory.class );
	}
	
	private void verifyBackendUsage(String name, Class<? extends BackendQueueProcessorFactory> backendType) {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSession ftSession = builder
			.setProperty( "hibernate.search.worker.backend", name )
			.openFullTextSession();
		SearchFactoryImplementor searchFactory = ( SearchFactoryImplementor) ftSession.getSearchFactory();
		ftSession.close();
		assertEquals( backendType, searchFactory.getBackendQueueProcessorFactory().getClass() );
		builder.close();
	}

	public void verifyBackendUsage(Class<? extends BackendQueueProcessorFactory> backendType) {
		verifyBackendUsage( backendType.getName(), backendType );
	}

}
