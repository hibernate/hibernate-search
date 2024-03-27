/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchIndexLifecycleExecutionOptions {

	private final IndexStatus requiredStatus;

	private final int requiredStatusTimeoutInMs;

	public ElasticsearchIndexLifecycleExecutionOptions(
			IndexStatus requiredStatus, int requiredStatusTimeoutInMs) {
		this.requiredStatus = requiredStatus;
		this.requiredStatusTimeoutInMs = requiredStatusTimeoutInMs;
	}

	/**
	 * @return the status the index needs to be at least in, otherwise we'll fail starting up.
	 */
	public IndexStatus getRequiredStatus() {
		return requiredStatus;
	}

	/**
	 * @return the time to wait for the {@link #getRequiredStatus() required index status}, in milliseconds.
	 */
	public int getRequiredStatusTimeoutInMs() {
		return requiredStatusTimeoutInMs;
	}

}
