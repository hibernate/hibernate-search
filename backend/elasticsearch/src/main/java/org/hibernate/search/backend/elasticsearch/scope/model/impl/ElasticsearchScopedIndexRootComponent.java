/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.scope.model.impl;

public class ElasticsearchScopedIndexRootComponent<T> {

	private T component;
	private ElasticsearchCompatibilityChecker idConverterCompatibilityChecker = new ElasticsearchSucceedingCompatibilityChecker();

	public T getComponent() {
		return component;
	}

	public ElasticsearchCompatibilityChecker getIdConverterCompatibilityChecker() {
		return idConverterCompatibilityChecker;
	}

	void setComponent(T component) {
		this.component = component;
	}

	void setIdConverterCompatibilityChecker(ElasticsearchCompatibilityChecker idConverterCompatibilityChecker) {
		this.idConverterCompatibilityChecker = idConverterCompatibilityChecker;
	}
}
