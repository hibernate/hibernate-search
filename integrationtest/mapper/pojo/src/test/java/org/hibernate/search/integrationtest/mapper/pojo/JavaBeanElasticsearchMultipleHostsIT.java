/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.integrationtest.util.common.StubClientElasticsearchBackendFactory;
import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient;
import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient.Request;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.json.JSONException;

import static org.hibernate.search.integrationtest.util.common.StubAssert.assertDropAndCreateIndexRequests;
import static org.hibernate.search.integrationtest.util.common.StubAssert.assertIndexDocumentRequest;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanElasticsearchMultipleHostsIT {

	private SearchMappingRepository mappingRepository;

	private static final String HOST_1 = "http://es1.mycompany.com:9200/";
	private static final String HOST_2 = "http://es2.mycompany.com:9200/";
	private static final String HOSTS = HOST_1 + " " + HOST_2;

	@Before
	public void setup() throws JSONException {
	}

	@After
	public void cleanup() {
		StubElasticsearchClient.drainRequestsByIndex();
		if ( mappingRepository != null ) {
			mappingRepository.close();
		}
	}

	@Test
	public void index() throws JSONException {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.elasticsearchBackend_1.type", StubClientElasticsearchBackendFactory.class.getName() )
				.setProperty( "backend.elasticsearchBackend_1.host", HOSTS )
				.setProperty( "index.default.backend", "elasticsearchBackend_1" );

		JavaBeanMappingContributor contributor = new JavaBeanMappingContributor( mappingRepositoryBuilder );

		ProgrammaticMappingDefinition mappingDefinition = contributor.programmaticMapping();
		mappingDefinition.type( IndexedEntity.class )
				.indexed( IndexedEntity.INDEX )
				.property( "id" )
				.documentId();

		mappingRepository = mappingRepositoryBuilder.build();
		JavaBeanMapping mapping = contributor.getResult();

		Map<String, List<Request>> requests = StubElasticsearchClient.drainRequestsByIndex();

		assertDropAndCreateIndexRequests( requests, IndexedEntity.INDEX, HOST_1, HOST_2,
				"{"
				+ "}" );

		try ( PojoSearchManager manager = mapping.createSearchManager() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			manager.getMainWorker().add( entity1 );
		}

		requests = StubElasticsearchClient.drainRequestsByIndex();
		assertIndexDocumentRequest( requests, IndexedEntity.INDEX, 0, HOST_1, "1", "{}" );
	}


	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

	}

}
