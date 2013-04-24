/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.tika.metadata.Metadata;

/**
 * @author Hardy Ferentschik
 */
public interface TikaMetadataProcessor {
	/**
	 * This method is called by the {@link org.hibernate.search.bridge.builtin.TikaBridge} prior to processing the data
	 *
	 * @return Tika metadata used for data processing. Additional metadata can be set here.
	 * @see <a href="http://tika.apache.org/1.1/parser.html#apiorgapachetikametadataMetadata.html">Tika API</a>
	 */
	Metadata prepareMetadata();

	/**
	 * This method called by the {@link org.hibernate.search.bridge.builtin.TikaBridge} after processing the data.
	 * It can be used to add extracted metadata to the  document.
	 *
	 * @param name The field name  to add to the Lucene document
	 * @param value The value to index
	 * @param document The Lucene document into which we want to index the value.
	 * @param luceneOptions Contains the parameters used for adding {@code value} to
	 * the Lucene document.
	 * @param metadata the metadata discovered by the Tika parsing process
	 */
	void set(String name, Object value, Document document, LuceneOptions luceneOptions, Metadata metadata);
}
