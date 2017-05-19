/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.Map;

import org.apache.lucene.document.Field.Index;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Partial metadata about a document field, used to provide partial information
 * to service providers while building the field metadata.
 *
 * @author Yoann Rodiere
 */
public interface PartialDocumentFieldMetadata {

	DocumentFieldPath getPath();

	PartialPropertyMetadata getSourceProperty();

	Map<String, BridgeDefinedField> getBridgeDefinedFields();

	boolean isNumeric();

	NumericEncodingType getNumericEncodingType();

	@SuppressWarnings("deprecation")
	Index getIndex();

	AnalyzerReference getAnalyzerReference();

}
