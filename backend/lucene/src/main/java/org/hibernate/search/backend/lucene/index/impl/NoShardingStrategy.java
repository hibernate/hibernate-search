/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.index.spi.ShardingStrategy;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategyInitializationContext;
import org.hibernate.search.util.common.AssertionFailure;

public class NoShardingStrategy implements ShardingStrategy {

	public static final String NAME = "none";

	@Override
	public void initialize(ShardingStrategyInitializationContext context) {
		context.disableSharding();
	}

	@Override
	public String toShardIdentifier(String documentId, String routingKey) {
		throw shouldNotHappen();
	}

	@Override
	public Set<String> toShardIdentifiers(Set<String> routingKeys) {
		throw shouldNotHappen();
	}

	private AssertionFailure shouldNotHappen() {
		return new AssertionFailure( "This should not be called" );
	}
}
