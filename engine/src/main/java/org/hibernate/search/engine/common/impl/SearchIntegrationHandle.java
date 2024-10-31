/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.logging.impl.CommonFailureLog;

final class SearchIntegrationHandle implements SearchIntegration.Handle {

	private SearchIntegration integration;

	void initialize(SearchIntegration integration) {
		this.integration = integration;
	}

	@Override
	public SearchIntegration getOrFail() {
		if ( integration == null ) {
			throw CommonFailureLog.INSTANCE.noIntegrationBecauseInitializationNotComplete();
		}
		return integration;
	}

	@Override
	public SearchIntegration getOrNull() {
		return integration;
	}
}
