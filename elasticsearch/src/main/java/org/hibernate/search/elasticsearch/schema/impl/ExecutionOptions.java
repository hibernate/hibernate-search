/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;

/**
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public interface ExecutionOptions {

	/**
	 * @return the status the index needs to be at least in, otherwise we'll fail starting up.
	 */
	ElasticsearchIndexStatus getRequiredIndexStatus();

	/**
	 * @return the default Elasticsearch dynamic mapping behaviour.
	 */
	DynamicType getDynamicMapping();

	/**
	 * @return the time to wait for the {@link #getRequiredIndexStatus() required index status}, in milliseconds.
	 */
	int getIndexManagementTimeoutInMs();

	/**
	 * @return whether multitenancy is enabled ({@code true}) or not ({@code false}).
	 */
	boolean isMultitenancyEnabled();

}
