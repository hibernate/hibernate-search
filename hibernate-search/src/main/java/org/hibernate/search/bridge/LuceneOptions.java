/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.apache.lucene.document.Field;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 * 
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface LuceneOptions {
	
	void addFieldToDocument(String name, String indexedString, Document document);
	
	/**
	 * Might be removed in version 3.3 to better support Lucene 3
	 * which is missing COMPRESS Store Type.
	 * To use compression either use #addFieldToDocument or refer
	 * to Lucene documentation to implement your own compression
	 * strategy.
	 * @deprecated use addToDocument to add fields to the Document if possible 
	 */
	Field.Store getStore();

	/**
	 * @deprecated likely to be removed in version 3.3, use #addFieldToDocument
	 */
	Field.Index getIndex();

	/**
	 * @deprecated likely to be removed in version 3.3, use #addFieldToDocument
	 */
	Field.TermVector getTermVector();

	/**
	 * @deprecated likely to be removed in version 3.3, use #addFieldToDocument
	 */
	Float getBoost();
	
}
