/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.facet;

import java.util.Collections;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@RunWith(BMUnitRunner.class)
public class FacetIndexingFailureTest {
	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Car.class ).enableJPAAnnotationsProcessing( true );


	@Test
	@TestForIssue(jiraKey = "HSEARCH-809")
	@BMRule(targetClass = "org.apache.lucene.facet.FacetsConfig",
			targetMethod = "build(Document)",
			action = "throw new IOException(\"Byteman here!\")",
			name = "buildError")
	public void testFailureToIndexFacetThrowsSearchException() throws Exception {
		DocumentBuilderIndexedEntity documentBuilder = factoryHolder.getSearchFactory()
				.getIndexBindings().get( Car.class ).getDocumentBuilder();
		try {
			Car car = new Car( "Honda", "yellow", 2407 );
			documentBuilder.getDocument(
					null,
					car,
					1,
					Collections.<String, String>emptyMap(),
					DefaultInstanceInitializer.DEFAULT_INITIALIZER,
					new ContextualExceptionBridgeHelper(),
					null
			);
			fail( "IOException during facet indexing should throw exception" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000265" ) );
		}
	}
}
