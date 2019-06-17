/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchIndexLifecycleExecutionOptions {

	private final ElasticsearchIndexStatus requiredStatus;

	private final int requiredStatusTimeoutInMs;

	public ElasticsearchIndexLifecycleExecutionOptions(
			ElasticsearchIndexStatus requiredStatus, int requiredStatusTimeoutInMs) {
		this.requiredStatus = requiredStatus;
		this.requiredStatusTimeoutInMs = requiredStatusTimeoutInMs;
	}

	/**
	 * @return the status the index needs to be at least in, otherwise we'll fail starting up.
	 */
	public ElasticsearchIndexStatus getRequiredStatus() {
		return requiredStatus;
	}

	/**
	 * @return the time to wait for the {@link #getRequiredStatus() required index status}, in milliseconds.
	 */
	public int getRequiredStatusTimeoutInMs() {
		return requiredStatusTimeoutInMs;
	}

}
