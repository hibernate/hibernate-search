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
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.impl.QueueingProcessor;
import org.hibernate.search.backend.impl.TransactionalWorker;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.test.util.ManualConfiguration;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


/**
 * Tests to verify worker scope options.
 *
 * @author Hardy Ferentschik
 */
public class WorkerScopeConfigurationTest {
	private ManualConfiguration manualConfiguration;

	@Before
	public void setUp() {
		manualConfiguration = new ManualConfiguration();
		SearchMapping searchMapping = new SearchMapping();
		searchMapping.entity( Document.class ).indexed()
				.property( "id", ElementType.FIELD ).documentId()
				.property( "title", ElementType.FIELD ).field();
		manualConfiguration.setProgrammaticMapping( searchMapping );
		manualConfiguration.addProperty( "hibernate.search.default.directory_provider", "ram" );
		manualConfiguration.addClass( Document.class );
	}

	@Test
	public void testDefaultWorker() {
		SearchFactoryImplementor searchFactoryImplementor =
				new SearchFactoryBuilder().configuration( manualConfiguration ).buildSearchFactory();
		assertNotNull( "Worker should have been created", searchFactoryImplementor.getWorker() );
		assertTrue( "Wrong worker class", searchFactoryImplementor.getWorker() instanceof TransactionalWorker );
	}

	@Test
	public void testExplicitTransactionalWorker() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", "transaction" );
		SearchFactoryImplementor searchFactoryImplementor =
				new SearchFactoryBuilder().configuration( manualConfiguration ).buildSearchFactory();
		assertNotNull( "Worker should have been created", searchFactoryImplementor.getWorker() );
		assertTrue( "Wrong worker class", searchFactoryImplementor.getWorker() instanceof TransactionalWorker );
	}

	@Test
	public void testCustomWorker() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", CustomWorker.class.getName() );
		SearchFactoryImplementor searchFactoryImplementor =
				new SearchFactoryBuilder().configuration( manualConfiguration ).buildSearchFactory();
		assertNotNull( "Worker should have been created", searchFactoryImplementor.getWorker() );
		assertTrue( "Wrong worker class", searchFactoryImplementor.getWorker() instanceof CustomWorker );
	}

	@Test
	public void testCustomWorkerWithProperties() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", CustomWorkerExpectingFooAndBar.class.getName() );
		manualConfiguration.addProperty( "hibernate.search.worker.foo", "foo" );
		manualConfiguration.addProperty( "hibernate.search.worker.bar", "bar" );
		SearchFactoryImplementor searchFactoryImplementor =
				new SearchFactoryBuilder().configuration( manualConfiguration ).buildSearchFactory();
		assertNotNull( "Worker should have been created", searchFactoryImplementor.getWorker() );
		assertTrue( "Wrong worker class", searchFactoryImplementor.getWorker() instanceof CustomWorkerExpectingFooAndBar );
	}

	@Test
	public void testUnknownWorkerImplementationClass() {
		manualConfiguration.addProperty( "hibernate.search.worker.scope", "foo" );
		try {
			new SearchFactoryBuilder().configuration( manualConfiguration ).buildSearchFactory();
			fail();
		}
		catch (SearchException e) {
			assertTrue(
					"Unexpected error message",
					e.getMessage().contains( "Unable to find worker implementation class: foo" )
			);
		}
	}

	@SuppressWarnings("unused")
	public static final class Document {

		private long id;
		private String title;

	}

	public static final class CustomWorker implements Worker {
		@Override
		public void performWork(Work<?> work, TransactionContext transactionContext) {
		}

		@Override
		public void initialize(Properties props, WorkerBuildContext context, QueueingProcessor queueingProcessor) {
		}

		@Override
		public void close() {
		}

		@Override
		public void flushWorks(TransactionContext transactionContext) {
		}
	}

	public static final class CustomWorkerExpectingFooAndBar implements Worker {
		public static final String FOO = "hibernate.search.worker.foo";
		public static final String BAR = "hibernate.search.worker.bar";

		@Override
		public void performWork(Work<?> work, TransactionContext transactionContext) {
		}

		@Override
		public void initialize(Properties props, WorkerBuildContext context, QueueingProcessor queueingProcessor) {
			assertTrue( "Missing property: " + FOO, props.containsKey( FOO ) );
			assertTrue( "Missing property: " + BAR, props.containsKey( BAR ) );
		}

		@Override
		public void close() {
		}

		@Override
		public void flushWorks(TransactionContext transactionContext) {
		}
	}
}
