/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.setup;

import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.impl.MutableSearchFactoryState;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.spi.impl.PolymorphicIndexHierarchy;
import org.junit.Before;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractAnnotationMetadataTest {

	protected AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		ConfigContext configContext = new ConfigContext(
				searchConfiguration,
				new BuildContextForTest( searchConfiguration ) );

		MutableSearchFactoryState factoryState = new MutableSearchFactoryState();
		factoryState.setIndexHierarchy( new PolymorphicIndexHierarchy() );
		factoryState.setDocumentBuildersIndexedEntities( new ConcurrentHashMap<Class<?>, EntityIndexBinding>() );

		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext, factoryState );
	}

}
