/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.SingularTermQuery;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.impl.LuceneWorkSerializerImpl;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DeleteByQueryTest {

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( Entity.class );

	private SerializationProvider serializationProvider;

	@Before
	public void setUp() {
		ServiceManager serviceManager = new StandardServiceManager( new SearchConfigurationForTest(), null );

		serializationProvider = serviceManager.requestService( SerializationProvider.class );
		assertTrue( "Wrong serialization provider", serializationProvider instanceof NativeJavaSerializationProvider );
	}

	@Test
	public void testAvroSerialization() throws Exception {

		LuceneWorkSerializer converter = new LuceneWorkSerializerImpl( serializationProvider, searchFactoryHolder.getSearchFactory() );
		List<LuceneWork> works = buildWorks();

		byte[] bytes = converter.toSerializedModel( works );
		List<LuceneWork> copyOfWorks = converter.toLuceneWorks( bytes );

		assertEquals( copyOfWorks.size(), works.size() );
		for ( int i = 0; i < works.size(); ++i ) {
			LuceneWork work = works.get( i );
			if ( work instanceof DeleteByQueryLuceneWork ) {
				DeleteByQueryLuceneWork original = (DeleteByQueryLuceneWork) work;
				DeleteByQueryLuceneWork copy = (DeleteByQueryLuceneWork) copyOfWorks.get( i );
				assertEquals( original.getDeletionQuery(), copy.getDeletionQuery() );
				assertEquals( original.getEntityClass(), copy.getEntityClass() );
			}
		}
	}

	private static List<LuceneWork> buildWorks() {
		List<LuceneWork> list = new ArrayList<>();
		list.add( new DeleteByQueryLuceneWork( Entity.class, new SingularTermQuery( "id", "1" ) ) );
		return list;
	}

	/**
	 * @author Martin Braun
	 */
	@Indexed
	public class Entity {

		@DocumentId
		private String id;
		@Field
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

}
