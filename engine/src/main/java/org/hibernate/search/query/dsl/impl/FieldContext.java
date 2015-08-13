/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;

/**
 * @author Emmanuel Bernard
 */
public class FieldContext {
	private final String field;
	private final boolean isIdField;
	private boolean ignoreAnalyzer;
	private final QueryCustomizer fieldCustomizer;
	private boolean ignoreFieldBridge;
	private FieldBridge fieldBridge;

	public FieldContext(String field, QueryBuildingContext queryContext) {
		this.field = field;
		this.fieldCustomizer = new QueryCustomizer();
		isIdField = Helper.getDocumentBuilder( queryContext ).getIdKeywordName().equals( field );
	}

	public String getField() {
		return field;
	}

	/**
	 * Whether to analyze the given field value or not.
	 * @return {@code true} if the field must be analyzed
	 */
	public boolean applyAnalyzer() {
		return !ignoreAnalyzer && !isIdField;
	}

	public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
		this.ignoreAnalyzer = ignoreAnalyzer;
	}

	public QueryCustomizer getFieldCustomizer() {
		return fieldCustomizer;
	}

	public boolean isIgnoreFieldBridge() {
		return ignoreFieldBridge;
	}

	public void setIgnoreFieldBridge(boolean ignoreFieldBridge) {
		this.ignoreFieldBridge = ignoreFieldBridge;
	}

	public void setFieldBridge(FieldBridge fieldBridge) {
		this.fieldBridge = fieldBridge;
	}

	public FieldBridge getFieldBridge() {
		return fieldBridge;
	}

	public String objectToString(DocumentBuilderIndexedEntity documentBuilder, Object value, ConversionContext conversionContext) {
		if ( isIgnoreFieldBridge() ) {
			return value == null ? null : value.toString();
		}
		else if ( fieldBridge != null ) {
			return documentBuilder.objectToString( field, fieldBridge, value, conversionContext );
		}
		else {
			return documentBuilder.objectToString( field, value, conversionContext );
		}
	}

	@Override
	public String toString() {
		return "FieldContext [field=" + field + ", fieldBridge=" + fieldBridge + ", fieldCustomizer=" + fieldCustomizer + ", ignoreAnalyzer=" + ignoreAnalyzer
				+ ", ignoreFieldBridge=" + ignoreFieldBridge + "]";
	}
}
