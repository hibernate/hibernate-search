/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.entity.Author;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;

public class AuthorService implements AutoCloseable {

	private static final AtomicInteger ID_PROVIDER = new AtomicInteger( 1 );
	private final Map<Integer, Author> datastore = new HashMap<>();
	private final CloseableSearchMapping mapping;

	public AuthorService() {
		mapping = createSearchMapping();
	}

	private CloseableSearchMapping createSearchMapping() {
		return SearchMapping.builder()
				.property( "hibernate.search.backend.log.json_pretty_printing", true )
				.property( "hibernate.search.backend.analysis.configurer",
						"org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.config.MyElasticsearchAnalysisConfigurer" )
				.addEntityType(
						Author.class,
						context -> context.selectionLoadingStrategy(
								SelectionLoadingStrategy.fromMap( datastore )
						)
				)
				.build();
	}

	public void add(String name) {
		try ( SearchSession session = mapping.createSessionWithOptions()
				.refreshStrategy( DocumentRefreshStrategy.FORCE )
				.build() ) {

			Author author = new Author( ID_PROVIDER.getAndIncrement(), name );
			session.indexingPlan().add( author );
			datastore.put( author.getId(), author );
		}
	}

	public List<Author> search(String term) {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<Author> query = session.search( Author.class )
					.where( f -> f.match().field( "name" ).matching( term ) )
					.toQuery();

			return query.fetchAllHits();
		}
	}

	@Override
	public void close() {
		datastore.clear();
		mapping.close();
	}
}
