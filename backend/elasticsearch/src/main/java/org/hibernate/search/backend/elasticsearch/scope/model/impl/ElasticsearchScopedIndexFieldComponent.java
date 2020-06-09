/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

public class ElasticsearchScopedIndexFieldComponent<T> {

	private T component;
	private boolean multiValuedFieldInRoot;
	private ElasticsearchCompatibilityChecker converterCompatibilityChecker = new ElasticsearchSucceedingCompatibilityChecker();
	private ElasticsearchCompatibilityChecker analyzerCompatibilityChecker = new ElasticsearchSucceedingCompatibilityChecker();

	public T getComponent() {
		return component;
	}

	public boolean isMultiValuedFieldInRoot() {
		return multiValuedFieldInRoot;
	}

	public ElasticsearchCompatibilityChecker getConverterCompatibilityChecker() {
		return converterCompatibilityChecker;
	}

	public ElasticsearchCompatibilityChecker getAnalyzerCompatibilityChecker() {
		return analyzerCompatibilityChecker;
	}

	void setComponent(T component) {
		this.component = component;
	}

	public void setMultiValuedFieldInRoot(boolean multiValuedFieldInRoot) {
		this.multiValuedFieldInRoot = multiValuedFieldInRoot;
	}

	void setConverterCompatibilityChecker(ElasticsearchCompatibilityChecker converterCompatibilityChecker) {
		this.converterCompatibilityChecker = converterCompatibilityChecker;
	}

	void setAnalyzerCompatibilityChecker(ElasticsearchCompatibilityChecker analyzerCompatibilityChecker) {
		this.analyzerCompatibilityChecker = analyzerCompatibilityChecker;
	}
}
