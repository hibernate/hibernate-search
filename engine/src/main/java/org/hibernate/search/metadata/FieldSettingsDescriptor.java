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

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;

/**
 * Metadata related to a single Lucene Document field and its options. Some of the values in this interface do not have
 * a direct counterpart in the Lucene works, but are an abstraction of Hibernate Search and mapped to the appropriate Lucene
 * construct.
 *
 * @author Hardy Ferentschik
 */
public interface FieldSettingsDescriptor {
	/**
	 * Returns the Lucene {@code Document} field name for this indexed property.
	 *
	 * @return Returns the field name for this index property
	 */
	String getName();

	/**
	 * @return an {@code Index} enum instance defining whether this field is indexed
	 */
	Index getIndex();

	/**
	 * @return an {@code Analyze} enum instance defining the type of analyzing applied to this field
	 */
	Analyze getAnalyze();

	/**
	 * @return a {@code Store} enum instance defining whether the index value is stored in the index itself
	 */
	Store getStore();

	/**
	 * @return a {@code TermVector} enum instance defining whether and how term vectors are stored for this field
	 */
	TermVector getTermVector();

	/**
	 * @return a {@code Norms} enum instance defining whether and how norms are stored for this field
	 */
	Norms getNorms();

	/**
	 * @return the boost value for this field. 1 being the default value.
	 */
	float getBoost();

	/**
	 * @return {@code Type} of this field
	 */
	Type getType();

	/**
	 * Narrows the type of this descriptor down to the specified {@code type}. The appropriate
	 * type should be checked beforehand by calling {@link #getType()}.
	 *
	 * @param <T> the type to narrow down to
	 * @param type class object representing the descriptor type to narrow down to
	 * to
	 *
	 * @return this descriptor narrowed down to the given type.
	 */
	<T extends FieldSettingsDescriptor> T as(Class<T> type);

	/**
	 * Defines different logical field types
	 */
	public enum Type {
		/**
		 * A basic field
		 */
		BASIC,

		/**
		 * A numeric field
		 */
		NUMERIC
	}
}


