/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.metadata;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.FieldBridge;

/**
 * Metadata related to a single indexed field.
 *
 * @author Hardy Ferentschik
 */
public interface FieldDescriptor {
	/**
	 * Returns the Lucene {@code Document} field name for this indexed property.
	 *
	 * @return Returns the field name for this index property
	 */
	String getName();

	/**
	 * @return {@code true} if the field is the document id, {@code false} otherwise
	 */
	boolean isId();

	/**
	 * @return an {@code Analyze} enum instance defining the type of analyzing applied to this field.
	 */
	Analyze getAnalyzeType();

	/**
	 * @return a {@code Store} enum instance defining whether the index value is stored in the index itself.
	 */
	Store getStoreType();

	/**
	 * @return a {@code TermVector} enum instance defining whether and how term vectors are stored for this field
	 */
	TermVector getTermVectorType();

	/**
	 * @return a {@code Norms} enum instance defining whether and how norms are stored for this field
	 */
	Norms getNormType();

	/**
	 * @return the boost value for this field. 1 being the default value.
	 */
	float getBoost();

	/**
	 * @return the string used to index {@code null} values. {@code null} in case null values are not indexed.
	 */
	String nullIndexedAs();

	/**
	 * @return the field bridge instance used to convert the property value into a string based field value
	 */
	FieldBridge getFieldBridge();

	/**
	 * @return the analyzer used for this field, {@code null} if the field is not analyzed
	 */
	Analyzer getAnalyzer();
}
