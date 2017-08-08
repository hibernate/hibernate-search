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

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
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
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( ElasticsearchDynamicIndexedValueHolder.class )
			.withProperty( Environment.ERROR_HANDLER, TestExceptionHandler.class.getName() )
			.withProperty( DYNAMIC_MAPPING, DynamicType.STRICT.name() );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testIndexingWithDynamicField() {
		// Store some not defined data:
		ElasticsearchDynamicIndexedValueHolder holder = new ElasticsearchDynamicIndexedValueHolder( "1" )
				.dynamicProperty( "age", "227" )
				.dynamicProperty( "name", "Thorin" )
				.dynamicProperty( "surname", "Oakenshield" )
				.dynamicProperty( "race", "dwarf" );

		helper.index( holder );

		helper.assertThat()
				.from( ElasticsearchDynamicIndexedValueHolder.class )
				.projecting( "dynamicField.name" )
				.matchesExactlySingleProjections( "Thorin" );
	}

	@Test
	public void testIndexingWithStrictField() {
		// This property should be mapped with dynamic: strict, this means that we cannot change the value adding new
		// properties and therefore an error should occurs
		ElasticsearchDynamicIndexedValueHolder holder = new ElasticsearchDynamicIndexedValueHolder( "2" )
				.strictProperty( "age", "64" )
				.strictProperty( "name", "Gimli" )
				.strictProperty( "race", "dwarf" );

		helper.index( holder );

		TestExceptionHandler errorHandler = getErrorHandler();

		assertThat( errorHandler.getHandleInvocations() ).hasSize( 1 );

		ErrorContext errorContext = errorHandler.getHandleInvocations().get( 0 );
		Throwable throwable = errorContext.getThrowable();

		assertThat( throwable ).isInstanceOf( SearchException.class );
		assertThat( throwable.getMessage() ).startsWith( "HSEARCH400007" );
		assertThat( throwable.getMessage() ).contains( "strict_dynamic_mapping_exception" );
		assertThat( throwable.getMessage() ).contains( "strictField" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2840")
	public void testProjectionWithDynamicField() {
		// Store some not defined data:
		ElasticsearchDynamicIndexedValueHolder holder = new ElasticsearchDynamicIndexedValueHolder( "1" )
				.dynamicProperty( "age", "227" )
				.dynamicProperty( "name", "Thorin" )
				.dynamicProperty( "surname", "Oakenshield" )
				.dynamicProperty( "race", "dwarf" );

		helper.index( holder );

		helper.assertThat()
				.from( ElasticsearchDynamicIndexedValueHolder.class )
				.projecting( "dynamicField.name" )
				.matchesExactlySingleProjections( "Thorin" );

		helper.assertThat()
				.from( ElasticsearchDynamicIndexedValueHolder.class )
				.projecting( "dynamicField" )
				.matchesExactlySingleProjections( holder.getDynamicFields() );
	}

	private TestExceptionHandler getErrorHandler() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		assertThat( errorHandler ).isInstanceOf( TestExceptionHandler.class );
		return (TestExceptionHandler) errorHandler;
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
