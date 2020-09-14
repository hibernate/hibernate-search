/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;

/**
 * @author Emmanuel Bernard
 */
public class MyComponent {
	@Field
	@Analyzer(impl = AnalyzerForTests4.class)
	private String componentProperty;

	public String getComponentProperty() {
		return componentProperty;
	}

	public void setComponentProperty(String componentProperty) {
		this.componentProperty = componentProperty;
	}
}
