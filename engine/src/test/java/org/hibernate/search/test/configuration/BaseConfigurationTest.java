/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

/**
 * @author gustavonalle
 */
public class BaseConfigurationTest {

	protected static AbstractWorkspaceImpl extractWorkspace(MutableSearchFactory sf, Class<?> type) {
		EntityIndexBinding indexBindingForEntity = sf.getIndexBinding( type );
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexBindingForEntity.getIndexManagers()[0];
		LuceneBackendQueueProcessor backend = (LuceneBackendQueueProcessor) indexManager.getBackendQueueProcessor();
		return backend.getIndexResources().getWorkspace();
	}

	protected MutableSearchFactory getMutableSearchFactoryWithSingleEntity(SearchConfigurationForTest cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Document.class ).indexed().indexName( "index1" )
				.property( "id", ElementType.FIELD ).documentId()
				.property( "title", ElementType.FIELD ).field()
		;
		cfg.setProgrammaticMapping( mapping );
		cfg.addClass( Document.class );
		return (MutableSearchFactory) new SearchIntegratorBuilder().configuration( cfg )
				.buildSearchIntegrator();
	}

	public static final class Document {
		long id;
		String title;
	}

	@Indexed(index = "index1")
	public static final class Dvd {
		@DocumentId
		long id;
		@Field
		String title;
	}

	@Indexed(index = "index2")
	public static final class Book {
		@DocumentId
		long id;
		@Field
		String title;
	}

}
