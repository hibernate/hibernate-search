/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		NUMERIC,

		/**
		 * A spatial field
		 */
		SPATIAL
	}
}


