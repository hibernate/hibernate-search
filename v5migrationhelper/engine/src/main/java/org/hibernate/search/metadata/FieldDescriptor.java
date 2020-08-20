/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metadata;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.bridge.FieldBridge;

/**
 * Metadata related to a single field. It extends @{code FieldSettingsDescriptor} to add Search specific
 * information, like {@link #indexNullAs()}. It also contains the analyzer and field bridge used to create the
 * actual field for the Lucene {@code Document}.
 *
 * @author Hardy Ferentschik
 */
public interface FieldDescriptor extends FieldSettingsDescriptor {
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
	 * @return the field bridge instance used to convert the property value into a string based field value
	 */
	FieldBridge getFieldBridge();

	/**
	 * @return the analyzer used for this field, {@code null} if the field is not analyzed
	 */
	Analyzer getAnalyzer();
}
