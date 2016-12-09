/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.bridge;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Checks that there is a way to create fields in an Elasticsearch indexes having dynamic attributes even when the
 * global option is set to strict.
 *
 * @author Davide D'Alto
 */
public class DynamicMappingIT {

	private static final String DYNAMIC_MAPPING = "hibernate.search." + ElasticsearchDynamicIndexedValueHolder.INDEX_NAME + "."
			+ ElasticsearchEnvironment.DYNAMIC_MAPPING;

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( ElasticsearchDynamicIndexedValueHolder.class )
			.withProperty( Environment.ERROR_HANDLER, TestExceptionHandler.class.getName() )
			.withProperty( DYNAMIC_MAPPING, DynamicType.STRICT.name() );

	@Test
	public void testIndexingWithDynamicField() {
		// Store some not defined data:
		ElasticsearchDynamicIndexedValueHolder holder = new ElasticsearchDynamicIndexedValueHolder( "1" )
				.dynamicProperty( "age", "227" )
				.dynamicProperty( "name", "Thorin" )
				.dynamicProperty( "surname", "Oakenshield" )
				.dynamicProperty( "race", "dwarf" );

		index( holder );
		List<EntityInfo> fieldValue = searchField( "dynamicField.name" );

		assertThat( fieldValue ).hasSize( 1 );
		assertThat( fieldValue.get( 0 ).getProjection()[0] ).isEqualTo( "Thorin" );
	}

	@Test
	public void testIndexingWithStrictField() {
		// This property should be mapped with dynamic: strict, this means that we cannot change the value adding new
		// properties and therefore an error should occurs
		ElasticsearchDynamicIndexedValueHolder holder = new ElasticsearchDynamicIndexedValueHolder( "2" )
				.strictProperty( "age", "64" )
				.strictProperty( "name", "Gimli" )
				.strictProperty( "race", "dwarf" );

		index( holder );

		TestExceptionHandler errorHandler = getErrorHandler();

		assertThat( errorHandler.getHandleInvocations() ).hasSize( 1 );

		ErrorContext errorContext = errorHandler.getHandleInvocations().get( 0 );
		Throwable throwable = errorContext.getThrowable();

		assertThat( throwable ).isInstanceOf( SearchException.class );
		assertThat( throwable.getMessage() ).startsWith( "HSEARCH400007" );
		assertThat( throwable.getMessage() ).contains( "strict_dynamic_mapping_exception" );
		assertThat( throwable.getMessage() ).contains( "strictField" );
	}

	private TestExceptionHandler getErrorHandler() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		assertThat( errorHandler ).isInstanceOf( TestExceptionHandler.class );
		return (TestExceptionHandler) errorHandler;
	}

	private void index(ElasticsearchDynamicIndexedValueHolder holder) {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Work work = new Work( holder, holder.id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	private List<EntityInfo> searchField(String field) {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		QueryBuilder guestQueryBuilder = searchFactory.buildQueryBuilder().forEntity( ElasticsearchDynamicIndexedValueHolder.class ).get();
		Query queryAllGuests = guestQueryBuilder.all().createQuery();

		return searchFactory.createHSQuery( queryAllGuests, ElasticsearchDynamicIndexedValueHolder.class )
				.projection( field )
				.queryEntityInfos();
	}

	public static class TestExceptionHandler implements ErrorHandler {

		private List<ErrorContext> handleInvocations = new ArrayList<>();

		@Override
		public void handle(ErrorContext context) {
			handleInvocations.add( context );
		}

		@Override
		public void handleException(String errorMsg, Throwable exception) {
		}

		public List<ErrorContext> getHandleInvocations() {
			return handleInvocations;
		}

		public void reset() {
			handleInvocations.clear();
		}
	}
}
