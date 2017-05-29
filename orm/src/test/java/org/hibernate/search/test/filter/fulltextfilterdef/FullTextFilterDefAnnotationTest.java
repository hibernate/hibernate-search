/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter.fulltextfilterdef;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.impl.SearchFactoryState;
import org.hibernate.search.test.filter.RoleFilterFactory;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test the use of {@link org.hibernate.search.annotations.FullTextFilterDef} annotation can be read by the engine
 * in all the valid locations.
 *
 * @author Davide D'Alto
 */
public class FullTextFilterDefAnnotationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class );

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void shouldBePossibleToAnnotatePackage() throws Exception {
		assertThatFilterExists( "package-filter" );
	}

	@Test
	public void shouldBePossibleToAnnotateClass() throws Exception {
		assertThatFilterExists( "class-filter" );
	}

	@Test
	public void shouldContainsOnlyTheDefinedFilters() throws Exception {
		Map<String, FilterDef> filterDefinitions = ( (SearchFactoryState) sfHolder.getSearchFactory() ).getFilterDefinitions();
		assertThat( filterDefinitions.keySet() ).containsOnly( "package-filter", "class-filter" );
	}

	@Test
	public void shouldNotBePossibleToHaveTwoFilterDefsWithTheSameName() throws Exception {
		thrown.expect( SearchException.class );

		HibernateManualConfiguration cfg = new HibernateManualConfiguration();
		cfg.addClass( SampleWithError.class );
		integratorResource.create( cfg );
	}

	private void assertThatFilterExists(String filterName) {
		FilterDef filterDefinition = sfHolder.getSearchFactory().getFilterDefinition( filterName );
		assertThat( filterDefinition ).isNotNull();
		assertThat( filterDefinition.getImpl() ).isEqualTo( RoleFilterFactory.class );
	}

	@Indexed
	@FullTextFilterDef(
			name = "class-filter",
			impl = RoleFilterFactory.class
	)
	static class Sample {

		@DocumentId
		long id;

		@Field
		String description;
	}

	@Indexed
	@FullTextFilterDef(
			name = "package-filter",
			impl = RoleFilterFactory.class
	)
	static class SampleWithError {

		@DocumentId
		final long id = 1L;

		@Field
		final String description = "";
	}
}
