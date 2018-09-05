/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

/**
 * When using this class in @FullTextFilterDef.impl, Hibernate Search
 * considers the filter to be only influencing the sharding strategy.
 *
 * This filter is not applied on the results of the Lucene query.
 *
 * @author Emmanuel Bernard
 */
public interface ShardSensitiveOnlyFilter {
}
