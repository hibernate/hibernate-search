/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.indexmanager.RamIndexManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the configured IndexManager implementation is used for each index .
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class IndexManagerOverrideTest {

	@Test
	public void verifyIndexExclusivity() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		FullTextSession ftSession = builder
			.setProperty( "hibernate.search.Book.indexmanager", "near-real-time" )
			.setProperty(
					"hibernate.search." + Foo.class.getName() + ".indexmanager",
					"org.hibernate.search.testsupport.indexmanager.RamIndexManager"
			)
			.addAnnotatedClass( BlogEntry.class )
			.addAnnotatedClass( Foo.class )
			.addAnnotatedClass( org.hibernate.search.test.query.Book.class )
			.addAnnotatedClass( org.hibernate.search.test.query.Author.class )
			.openFullTextSession();
		ExtendedSearchIntegrator integrator = ftSession.getSearchFactory().unwrap( ExtendedSearchIntegrator.class );
		ftSession.close();
		IndexManagerHolder allIndexesManager = integrator.getIndexManagerHolder();

		//checks for the default implementation
		checkIndexManagerType( allIndexesManager, "org.hibernate.search.test.configuration.BlogEntry",
				org.hibernate.search.indexes.spi.DirectoryBasedIndexManager.class );

		//Uses "NRT" taken from shortcut names
		checkIndexManagerType( allIndexesManager, "Book",
				org.hibernate.search.indexes.impl.NRTIndexManager.class );

		//Uses a fully qualified name to load an implementation
		checkIndexManagerType( allIndexesManager, Foo.class.getName(),
				RamIndexManager.class );

		builder.close();
	}

	private void checkIndexManagerType(IndexManagerHolder allIndexesManager, String name, Class expectedType) {
		IndexManager indexManager = allIndexesManager.getIndexManager( name );
		Assert.assertEquals( expectedType, indexManager.getClass() );
	}

	@Indexed
	@Entity
	@Table(name = "Foo")
	public static class Foo {

		@Id
		private int id;
	}
}
