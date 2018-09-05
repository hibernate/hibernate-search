/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HSEARCH-2909")
public class NoPackageAnnotationTest {

	private static final IndexedTypeIdentifier TYPE_ID = PojoIndexedTypeIdentifier.convertFromLegacy( Entity.class );

	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void initializationSucceedsWithNotPackagedAnnotation() throws Exception {
		SearchConfigurationForTest config = new SearchConfigurationForTest()
				.addClasses( Entity.class, ClassWithNotPackagedAnnotation.class );
		// Just check that initialization succeeds, and that the configuration was taken into account
		ExtendedSearchIntegrator integrator = integratorResource.create( config );
		assertNotNull( integrator.getIndexedTypeDescriptor( TYPE_ID ) );
	}

	@Indexed
	private static class Entity {
		@DocumentId
		Integer id;

		@Field
		String title;
	}

	/*
	 * The bug only occurs if no Search annotation is inspected before the not-packaged annotation,
	 * so the simplest solution is to use a class with no Search annotation at all...
	 */
	@NotPackagedAnnotation
	private static class ClassWithNotPackagedAnnotation {
	}
}
