/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jgroups.master;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.ShardIdentifierProviderTemplate;
import org.hibernate.search.testsupport.TestForIssue;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-1886")
public class JGroupsMasterDynamicShardingTest extends JGroupsMasterTest {

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );
		cfg.put( "hibernate.search.default.sharding_strategy", TShirtShardIdentifierProvider.class.getName() );
	}

	@Override
	protected String getIndexName() {
		return super.getIndexName() + ".11";
	}

	public static class TShirtShardIdentifierProvider extends ShardIdentifierProviderTemplate {

		@Override
		public String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document) {
			if ( entityType.equals( TShirt.class ) ) {
				final Double length = document.getField( "length" ).numericValue().doubleValue();
				String shardId = String.valueOf( length.intValue() / 2 );
				addShard( shardId );
				return shardId;
			}
			throw new RuntimeException( "TShirt expected but found " + entityType );
		}

		@Override
		protected Set<String> loadInitialShardNames(Properties properties, BuildContext buildContext) {
			return Collections.emptySet(); // We don't test restart anyway
		}
	}
}
