/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.lucene.AbstractWorkspaceImpl;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.impl.MutableSearchFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;


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

	protected MutableSearchFactory getMutableSearchFactoryWithSingleEntity(ManualConfiguration cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
				.entity( Document.class ).indexed().indexName( "index1" )
				.property( "id", ElementType.FIELD ).documentId()
				.property( "title", ElementType.FIELD ).field()
		;
		cfg.setProgrammaticMapping( mapping );
		cfg.addClass( Document.class );
		cfg.addProperty( "hibernate.search.default.directory_provider", "ram" );
		return (MutableSearchFactory) new SearchFactoryBuilder().configuration( cfg )
				.buildSearchFactory();
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
