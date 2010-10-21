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
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;

/**
 * Link between a java property and a Lucene Document
 * Usually a Java property will be linked to a Document Field.
 * <p/>
 * All implementations need to be threadsafe.
 *
 * @author Emmanuel Bernard
 */
public interface FieldBridge {

	/**
	 * Manipulate the document to index the given value.
	 * <p/>
	 * A common implementation is to add a Field with the given {@code name} to {@code document} following
	 * the parameters {@code luceneOptions} if the {@code value} is not {@code null}.
	 *
	 * {code}
	 * String fieldValue = convertToString(value);
	 * luceneOptions.addFieldToDocument(name, fieldValue, document);
	 * {code}
	 *
	 * @param name The field to add to the Lucene document
	 * @param value The actual value to index
	 * @param document The Lucene document into which we want to index the value.
	 * @param luceneOptions Contains the parameters used for adding {@code value} to
	 * the Lucene document.
	 */
	void set(String name, Object value, Document document, LuceneOptions luceneOptions);
}
