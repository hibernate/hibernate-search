/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public interface ExecutionOptions {

	/**
	 * Status the index needs to be at least in, otherwise we'll fail starting up.
	 */
	ElasticsearchIndexStatus getRequiredIndexStatus();

	/**
	 * Time to wait for the {@link #getRequiredIndexStatus() required index status}, in milliseconds.
	 */
	int getIndexManagementTimeoutInMs();

}
