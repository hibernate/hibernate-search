/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.spi.IdentifierCriteriaProvider;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test that illustrates how to specify {@link DetachedCriteria} filters for
 * index types by calling {@link MassIndexer#registerCriteriaProvider(Class, IdentifierCriteriaProvider)}.
 *
 * @author Chris Cranford
 */
public class IdentifierFilterTest extends SearchTestBase {

	@Test
	public void testMassIndexerFilterSingleType() throws InterruptedException {
		final FullTextSession fullTextSession = openAndPopulate();

		getMassIndexer( fullTextSession, Clock.class )
				.registerCriteriaProvider( Clock.class, new IdentifierCriteriaProvider() {
					@Override
					public Criterion getIdentifierCriteria(Class<?> indexedType) {
						return Restrictions.idEq( 2 );
					}
				} )
				.startAndWait();

		fullTextSession.close();

		assertEquals( 1, getNumberOfDocumentsInIndex( Clock.class ) );
	}

	@Test
	public void testMassIndexerFilterMultipleTypes() throws InterruptedException {
		final FullTextSession fullTextSession = openAndPopulate();

		getMassIndexer( fullTextSession, Clock.class, IssueEntity.class )
				.registerCriteriaProvider( Clock.class, new IdentifierCriteriaProvider() {
					@Override
					public Criterion getIdentifierCriteria(Class<?> indexedType) {
						return Restrictions.idEq( 1 );
					}
				} )
				.registerCriteriaProvider( IssueEntity.class, new IdentifierCriteriaProvider() {
					@Override
					public Criterion getIdentifierCriteria(Class<?> indexedType) {
						return Restrictions.idEq( "HHH-2" );
					}
				} )
				.startAndWait();

		fullTextSession.close();

		assertEquals( 1, getNumberOfDocumentsInIndex( Clock.class ) );
		assertEquals( 1, getNumberOfDocumentsInIndex( IssueEntity.class ) );
	}

	@Test
	public void testMassIndexerFilterCompositeIdType() throws InterruptedException {
		final FullTextSession fullTextSession = openAndPopulate();

		getMassIndexer( fullTextSession, CompositeKeyEntity.class )
				.registerCriteriaProvider( CompositeKeyEntity.class, new IdentifierCriteriaProvider() {
					@Override
					public Criterion getIdentifierCriteria(Class<?> indexedType) {
						final CompositeKeyEntity.Id id = new CompositeKeyEntity.Id( 1, 2 );
						return Restrictions.idEq( id );
					}
				} )
				.startAndWait();

		fullTextSession.close();

		assertEquals( 1, getNumberOfDocumentsInIndex( CompositeKeyEntity.class ) );
	}

	@Test
	public void testMassIndexerWithNoFilters() throws InterruptedException {
		final FullTextSession fullTextSession = openAndPopulate();
		getMassIndexer( fullTextSession, Clock.class, IssueEntity.class ).startAndWait();
		fullTextSession.close();
		assertEquals( 2, getNumberOfDocumentsInIndex( Clock.class ) );
		assertEquals( 2, getNumberOfDocumentsInIndex( IssueEntity.class ) );
	}

	@Override
	public void configure(Map<String, Object> settings) {
		super.configure( settings );
		settings.put( AvailableSettings.SHOW_SQL, "true" );
		settings.put( AvailableSettings.FORMAT_SQL, "true" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Clock.class, IssueEntity.class, CompositeKeyEntity.class };
	}

	private FullTextSession openAndPopulate() {

		// construct full text session
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		// save data
		fullTextSession.beginTransaction();

		Clock ace = new Clock( 1, "ACE" );
		fullTextSession.save( ace );

		Clock acme = new Clock( 2, "ACME" );
		fullTextSession.save( acme );

		IssueEntity issue1 = new IssueEntity();
		issue1.jiraCode = "HHH-1";
		issue1.jiraDescription = "Issue #1";
		fullTextSession.save( issue1 );

		IssueEntity issue2 = new IssueEntity();
		issue2.jiraCode = "HHH-2";
		issue2.jiraDescription = "Issue #2";
		fullTextSession.save( issue2 );

		CompositeKeyEntity composite1 = new CompositeKeyEntity();
		composite1.setId( new CompositeKeyEntity.Id( 1, 1 ) );
		composite1.setName( "Composite1" );
		fullTextSession.save( composite1 );

		CompositeKeyEntity composite2 = new CompositeKeyEntity();
		composite2.setId( new CompositeKeyEntity.Id( 1, 2 ) );
		composite2.setName( "Composite2" );
		fullTextSession.save( composite2 );

		fullTextSession.getTransaction().commit();

		return fullTextSession;
	}

	private MassIndexer getMassIndexer(FullTextSession fullTextSession, Class<?>... types) {
		final MassIndexer massIndexer = fullTextSession.createIndexer( types )
				.threadsToLoadObjects( 2 )
				.batchSizeToLoadObjects( 2 )
				.purgeAllOnStart( true )
				.optimizeOnFinish( true );
		return massIndexer;
	}
}
