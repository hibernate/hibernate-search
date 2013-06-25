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

import org.hibernate.search.SearchException;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.store.optimization.impl.ExplicitOnlyOptimizerStrategy;
import org.hibernate.search.store.optimization.impl.IncrementalOptimizerStrategy;
import org.hibernate.search.test.util.ManualConfiguration;
import org.junit.Test;


/**
 * Tests to verify configuration options regarding custom OptimizerStrategy
 * implementations.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class OptimizerStrategyLoadTest {

	@Test
	public void testDefaultImplementation() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.optimizer.implementation", "default" );
		verifyOptimizerImplementationIs( ExplicitOnlyOptimizerStrategy.class, cfg );
	}

	@Test
	public void testUnsetImplementation() {
		ManualConfiguration cfg = new ManualConfiguration();
		verifyOptimizerImplementationIs( ExplicitOnlyOptimizerStrategy.class, cfg );
	}

	@Test
	public void testIncrementalImplementation() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.optimizer.transaction_limit.max", "5" );
		verifyOptimizerImplementationIs( IncrementalOptimizerStrategy.class, cfg );
	}

	@Test(expected = SearchException.class)
	public void testIllegalImplementation() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.optimizer.implementation", "5" );
		verifyOptimizerImplementationIs( IncrementalOptimizerStrategy.class, cfg );
	}

	@Test
	public void testValidExtension() {
		ManualConfiguration cfg = new ManualConfiguration();
		cfg.addProperty( "hibernate.search.default.optimizer.implementation", CustomOptimizer.class.getName() );
		verifyOptimizerImplementationIs( CustomOptimizer.class, cfg );
	}

	@SuppressWarnings("unchecked")
	private void verifyOptimizerImplementationIs(Class type, ManualConfiguration cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Document.class ).indexed()
			.property( "id", ElementType.FIELD ).documentId()
			.property( "title", ElementType.FIELD ).field()
			;
		cfg.setProgrammaticMapping( mapping );
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.addClass( Document.class );
		SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( cfg ).buildSearchFactory();
		try {
			EntityIndexBinding indexBindingForEntity = sf.getIndexBinding( Document.class );
			DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
			OptimizerStrategy optimizerStrategy = indexManager.getOptimizerStrategy();
			Assert.assertTrue( type.isAssignableFrom( optimizerStrategy.getClass() ) );
		}
		finally {
			sf.close();
		}
	}

	@SuppressWarnings("unused")
	public static final class Document {

		private long id;
		private String title;

	}

	public static final class CustomOptimizer extends IncrementalOptimizerStrategy {

	}

}
