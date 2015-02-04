/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.junit.Assert;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.store.optimization.impl.ExplicitOnlyOptimizerStrategy;
import org.hibernate.search.store.optimization.impl.IncrementalOptimizerStrategy;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
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
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addProperty( "hibernate.search.default.optimizer.implementation", "default" );
		verifyOptimizerImplementationIs( ExplicitOnlyOptimizerStrategy.class, cfg );
	}

	@Test
	public void testUnsetImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		verifyOptimizerImplementationIs( ExplicitOnlyOptimizerStrategy.class, cfg );
	}

	@Test
	public void testIncrementalImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addProperty( "hibernate.search.default.optimizer.transaction_limit.max", "5" );
		verifyOptimizerImplementationIs( IncrementalOptimizerStrategy.class, cfg );
	}

	@Test(expected = SearchException.class)
	public void testIllegalImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addProperty( "hibernate.search.default.optimizer.implementation", "5" );
		verifyOptimizerImplementationIs( IncrementalOptimizerStrategy.class, cfg );
	}

	@Test
	public void testValidExtension() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addProperty( "hibernate.search.default.optimizer.implementation", CustomOptimizer.class.getName() );
		verifyOptimizerImplementationIs( CustomOptimizer.class, cfg );
	}

	@SuppressWarnings("unchecked")
	private void verifyOptimizerImplementationIs(Class type, SearchConfigurationForTest cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Document.class ).indexed()
			.property( "id", ElementType.FIELD ).documentId()
			.property( "title", ElementType.FIELD ).field()
			;
		cfg.setProgrammaticMapping( mapping );
		cfg.addClass( Document.class );
		try ( SearchIntegrator sf = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator() ) {
			EntityIndexBinding indexBindingForEntity = sf.getIndexBinding( Document.class );
			DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
			OptimizerStrategy optimizerStrategy = indexManager.getOptimizerStrategy();
			Assert.assertTrue( type.isAssignableFrom( optimizerStrategy.getClass() ) );
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
