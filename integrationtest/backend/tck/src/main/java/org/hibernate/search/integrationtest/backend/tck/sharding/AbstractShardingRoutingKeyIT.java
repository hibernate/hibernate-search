/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * An abstract base for sharding tests with explicit routing keys.
 */
public abstract class AbstractShardingRoutingKeyIT extends AbstractShardingIT {

	@Rule
	public final SearchSetupHelper setupHelper;

	private final Set<String> routingKeys;
	private final int documentCountPerRoutingKey;
	private final int totalDocumentCount;

	private final Map<String, List<String>> docIdByRoutingKey = new HashMap<>();

	public AbstractShardingRoutingKeyIT(Function<TckBackendHelper, TckBackendSetupStrategy<?>> setupStrategyFunction,
			Set<String> routingKeys) {
		super( RoutingMode.EXPLICIT_ROUTING_KEYS );
		this.setupHelper = new SearchSetupHelper( setupStrategyFunction );
		this.routingKeys = routingKeys;
		documentCountPerRoutingKey = 100;
		totalDocumentCount = routingKeys.size() * documentCountPerRoutingKey;
	}

	@Before
	public void setup() {
		SearchSetupHelper.SetupContext setupContext = setupHelper.start();
		configure( setupContext );
		setupContext.withIndex( index ).setup();

		// Provide explicit routing keys when indexing
		for ( String routingKey : routingKeys ) {
			for ( int documentIdAsIntegerForRoutingKey = 0;
					documentIdAsIntegerForRoutingKey < documentCountPerRoutingKey;
					documentIdAsIntegerForRoutingKey++ ) {
				// Just make sure document IDs are unique across all routing keys
				String documentId = routingKey + "_" + documentIdAsIntegerForRoutingKey;
				docIdByRoutingKey.computeIfAbsent( routingKey, ignored -> new ArrayList<>() )
						.add( documentId );
			}
		}

		initData( docIdByRoutingKey );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3314")
	public void search() {
		// No routing key => all documents should be returned
		assertThatQuery( index.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) );

		// Now test with a specific routing key
		Iterator<String> iterator = docIdByRoutingKey.keySet().iterator();
		String someRoutingKey = iterator.next();
		String someOtherRoutingKey = iterator.next();

		/*
		 * One routing key => all documents indexed with that routing key should be returned,
		 * and only those documents.
		 */
		assertThatQuery(
				index.createScope().query()
						.where( f -> f.matchAll() )
						.routing( someRoutingKey )
						.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( documentCountPerRoutingKey )
				.containsExactlyInAnyOrder( docRefsForRoutingKey( someRoutingKey, docIdByRoutingKey ) );

		/*
		 * Two routing keys => all documents indexed with these routing keys should be returned,
		 * and only those documents.
		 */
		List<String> twoRoutingKeys = Arrays.asList( someRoutingKey, someOtherRoutingKey );
		assertThatQuery(
				index.createScope().query()
						.where( f -> f.matchAll() )
						.routing( twoRoutingKeys )
						.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( documentCountPerRoutingKey * 2 )
				.containsExactlyInAnyOrder( docRefsForRoutingKeys( twoRoutingKeys, docIdByRoutingKey ) );

		// All routing keys => all documents should be returned
		assertThatQuery( index.createScope().query()
				.where( f -> f.matchAll() )
				.routing( docIdByRoutingKey.keySet() )
				.toQuery()
		)
				.hits().asNormalizedDocRefs()
				.hasSize( totalDocumentCount )
				.containsExactlyInAnyOrder( allDocRefs( docIdByRoutingKey ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3824")
	public void purge_noRoutingKey() {
		index.createWorkspace().purge( Collections.emptySet(), OperationSubmitter.blocking() ).join();

		// No routing key => all documents should be purged
		index.createWorkspace().refresh( OperationSubmitter.blocking() ).join();
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasNoHits();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3824")
	public void purge_oneRoutingKey() {
		Iterator<String> iterator = docIdByRoutingKey.keySet().iterator();
		String someRoutingKey = iterator.next();

		Set<String> otherRoutingKeys = new LinkedHashSet<>( routingKeys );
		otherRoutingKeys.remove( someRoutingKey );

		index.createWorkspace().purge( Collections.singleton( someRoutingKey ), OperationSubmitter.blocking() ).join();

		/*
		 * One routing key => all documents indexed with that routing key should be purged,
		 * and only those documents.
		 */
		index.createWorkspace().refresh( OperationSubmitter.blocking() ).join();
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hits().asNormalizedDocRefs()
				.containsExactlyInAnyOrder( docRefsForRoutingKeys( otherRoutingKeys, docIdByRoutingKey ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3824")
	public void purge_twoRoutingKeys() {
		Iterator<String> iterator = docIdByRoutingKey.keySet().iterator();
		Set<String> twoRoutingKeys = CollectionHelper.asImmutableSet( iterator.next(), iterator.next() );

		Set<String> otherRoutingKeys = new LinkedHashSet<>( routingKeys );
		otherRoutingKeys.removeAll( twoRoutingKeys );

		index.createWorkspace().purge( twoRoutingKeys, OperationSubmitter.blocking() ).join();

		/*
		 * Two routing keys => all documents indexed with these routing keys should be returned,
		 * and only those documents.
		 */
		index.createWorkspace().refresh( OperationSubmitter.blocking() ).join();
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hits().asNormalizedDocRefs()
				.containsExactlyInAnyOrder( docRefsForRoutingKeys( otherRoutingKeys, docIdByRoutingKey ) );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3824")
	public void purge_allRoutingKeys() {
		index.createWorkspace().purge( routingKeys, OperationSubmitter.blocking() ).join();

		// All routing keys => all documents should be purged
		index.createWorkspace().refresh( OperationSubmitter.blocking() ).join();
		assertThatQuery( index.createScope().query().where( f -> f.matchAll() ).toQuery() )
				.hasNoHits();
	}

	protected void configure(SearchSetupHelper.SetupContext setupContext) {
	}

	protected static Set<String> generateRoutingKeys(int routingKeyCount) {
		Set<String> routingKeys = new LinkedHashSet<>();
		for ( int routingKeyAsInteger = 0; routingKeyAsInteger < routingKeyCount; routingKeyAsInteger++ ) {
			// Turn into actual text, to check that support is not limited to just numbers
			String routingKey = "someText_" + routingKeyAsInteger;
			routingKeys.add( routingKey );
		}
		return routingKeys;
	}
}
