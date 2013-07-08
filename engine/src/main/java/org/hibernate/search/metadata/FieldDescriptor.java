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
import org.hibernate.search.bridge.FieldBridge;

/**
 * Metadata related to a single indexed field.
 *
 * @author Hardy Ferentschik
 */
public interface FieldDescriptor extends FieldSettingsDescriptor {

	/**
	 * @return the field type for this field
	 */
	Type getFieldType();

	/**
	 * @return the string used to index {@code null} values. {@code null} in case null values are not indexed
	 */
	String indexNullAs();

	/**
	 * @return {@code true} if {@code null} values are indexed, {@code false} otherwise
	 *
	 * @see #indexNullAs()
	 */
	boolean indexNull();

	/**
	 * @return the numeric precision step in case this field is indexed as a numeric value. If the field is not numeric
	 *         {@code null} is returned.
	 */
	Integer precisionStep();

	/**
	 * @return {@code true} if this field is indexed as numeric field, {@code false} otherwise
	 *
	 * @see #precisionStep()
	 */
	boolean isNumeric();

	/**
	 * @return the field bridge instance used to convert the property value into a string based field value
	 */
	FieldBridge getFieldBridge();

	/**
	 * @return the analyzer used for this field, {@code null} if the field is not analyzed
	 */
	Analyzer getAnalyzer();

	/**
	 * Defines different logical field types
	 */
	public static enum Type {
		/**
		 * This field is used as the document id
		 */
		ID,

		/**
		 * A basic field generate by a field bridge
		 */
		BASIC
	}
}
