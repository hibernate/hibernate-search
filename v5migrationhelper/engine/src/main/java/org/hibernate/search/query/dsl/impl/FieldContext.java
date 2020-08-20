/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

/**
 * @author Emmanuel Bernard
 */
public class FieldContext {
	private final String field;
	private boolean ignoreAnalyzer;
	private final QueryCustomizer fieldCustomizer;
	private boolean ignoreFieldBridge;

	public FieldContext(String field) {
		this.field = field;
		this.fieldCustomizer = new QueryCustomizer();
	}

	public String getField() {
		return field;
	}

	/**
	 * Whether to analyze the given field value or not.
	 * @return {@code true} if the field must be analyzed
	 */
	public boolean applyAnalyzer() {
		return !ignoreAnalyzer;
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

	@Override
	public String toString() {
		return "FieldContext [field=" + field + ", fieldCustomizer=" + fieldCustomizer + ", ignoreAnalyzer=" + ignoreAnalyzer
				+ ", ignoreFieldBridge=" + ignoreFieldBridge + "]";
	}
}
