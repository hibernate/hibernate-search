/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

public class LuceneScopedIndexFieldComponent<T> {

	private T component;
	private LuceneCompatibilityChecker converterCompatibilityChecker = new LuceneSucceedingCompatibilityChecker();
	private LuceneCompatibilityChecker analyzerCompatibilityChecker = new LuceneSucceedingCompatibilityChecker();

	public T getComponent() {
		return component;
	}

	public LuceneCompatibilityChecker getConverterCompatibilityChecker() {
		return converterCompatibilityChecker;
	}

	public LuceneCompatibilityChecker getAnalyzerCompatibilityChecker() {
		return analyzerCompatibilityChecker;
	}

	void setComponent(T component) {
		this.component = component;
	}

	void setConverterCompatibilityChecker(LuceneCompatibilityChecker converterCompatibilityChecker) {
		this.converterCompatibilityChecker = converterCompatibilityChecker;
	}

	void setAnalyzerCompatibilityChecker(LuceneCompatibilityChecker analyzerCompatibilityChecker) {
		this.analyzerCompatibilityChecker = analyzerCompatibilityChecker;
	}
}
