/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.DefaultStringBridge;
import org.hibernate.search.bridge.util.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.test.metadata.DescriptorTestHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1756")
public class DefaultStringBridgeTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		ConfigContext configContext = new ConfigContext(
				searchConfiguration,
				new BuildContextForTest( searchConfiguration )
		);
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testUsageOfDefaultStringBridgeInFieldBridgeAnnotation() throws Exception {
		String fieldName = "foo";
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Foo.class, fieldName );

		FieldBridge fieldBridge = fieldDescriptor.getFieldBridge();
		assertTrue(
				"The DefaultStringBridge should be wrapped in String2FieldBridgeAdaptor",
				fieldBridge instanceof String2FieldBridgeAdaptor
		);

		// need to use reflection :-(
		java.lang.reflect.Field field = fieldBridge.getClass().getDeclaredField( "stringBridge" );
		field.setAccessible( true );
		StringBridge stringBridge = (StringBridge) field.get( fieldBridge );

		assertTrue(
				"There should only be a single instance of DefaultStringBridge",
				DefaultStringBridge.INSTANCE == stringBridge
		);
	}

	private FieldDescriptor getFieldDescriptor(Class<?> clazz, String fieldName) {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, clazz );
		return typeDescriptor.getIndexedField( fieldName );
	}

	@Indexed
	public static class Foo {

		@DocumentId
		private long id;

		@Field(bridge = @org.hibernate.search.annotations.FieldBridge(impl = DefaultStringBridge.class))
		private String foo;
	}
}


