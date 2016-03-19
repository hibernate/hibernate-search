/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.genericjpa.factory.StandaloneSearchConfiguration;
import org.hibernate.search.genericjpa.metadata.impl.ExtendedTypeMetadata;
import org.hibernate.search.genericjpa.metadata.impl.MetadataExtender;
import org.hibernate.search.genericjpa.metadata.impl.MetadataUtil;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Braun
 */
public class MetadataExtenderTest {

	private MetadataProvider metadataProvider;
	private MetadataExtender metadataExtender;

	@Before
	public void setup() {
		SearchConfiguration searchConfiguration = new StandaloneSearchConfiguration();
		metadataProvider = MetadataUtil.getDummyMetadataProvider( searchConfiguration );
		this.metadataExtender = new MetadataExtender();
	}

	@Test
	public void testBasic() {
		TypeMetadata fromRoot = this.metadataProvider.getTypeMetadataFor( RootEntity.class );
		ExtendedTypeMetadata fromRootRehashed = this.metadataExtender.rehash(
				Collections.singletonList( fromRoot ),
				new HashSet<>()
		).get( 0 );
		{

			assertEquals( fromRoot, fromRootRehashed.getOriginalTypeMetadata() );

			// THE ID FIELD NAMES
			{
				Map<Class<?>, Set<String>> idFieldNamesForType = fromRootRehashed.getIdFieldNamesForType();

				assertEquals( 3, idFieldNamesForType.get( RootEntity.class ).size() );
				assertTrue( idFieldNamesForType.get( RootEntity.class ).contains( "MAYBE_ROOT_NOT_NAMED_ID" ) );
				assertTrue(
						idFieldNamesForType.get( RootEntity.class ).contains(
								"recursiveSelf.MAYBE_ROOT_NOT_NAMED_ID"
						)
				);
				assertTrue(
						idFieldNamesForType.get( RootEntity.class ).contains(
								"recursiveSelf.recursiveSelf.MAYBE_ROOT_NOT_NAMED_ID"
						)
				);

				assertEquals( 2, idFieldNamesForType.get( SubEntity.class ).size() );
				assertTrue( idFieldNamesForType.get( SubEntity.class ).contains( "otherEntity.SUB_NOT_NAMED_ID" ) );
				assertTrue(
						idFieldNamesForType.get( SubEntity.class ).contains(
								"recursiveSelf.otherEntity.SUB_NOT_NAMED_ID"
						)
				);
			}

			// THE ID PROPERTY NAMES
			{
				assertEquals( "rootId", fromRootRehashed.getIdPropertyNameForType().get( RootEntity.class ) );
				assertEquals( "subId", fromRootRehashed.getIdPropertyNameForType().get( SubEntity.class ) );
			}

			// THE DOCUMENT_FIELD_META_DATA
			{
				assertEquals( 5, fromRootRehashed.getFieldBridgeForIdFieldName().size() );
			}
		}

		TypeMetadata fromAnotherRoot = this.metadataProvider.getTypeMetadataFor( AnotherRootEntity.class );
		ExtendedTypeMetadata fromAnotherRootRehashed = this.metadataExtender.rehash(
				Collections.singletonList(
						fromAnotherRoot
				),
				Collections.emptySet()
		).get( 0 );

		Set<Class<?>> indexRelevantEntities = MetadataUtil.calculateIndexRelevantEntities(
				Arrays.asList(
						fromRootRehashed,
						fromAnotherRootRehashed
				), Collections.emptySet()
		);
		assertEquals( 3, indexRelevantEntities.size() );
		assertTrue( indexRelevantEntities.contains( RootEntity.class ) );
		assertTrue( indexRelevantEntities.contains( AnotherRootEntity.class ) );
		assertTrue( indexRelevantEntities.contains( SubEntity.class ) );

		Map<Class<?>, Set<Class<?>>> inIndexOf = MetadataUtil.calculateInIndexOf(
				Arrays.asList(
						fromRootRehashed,
						fromAnotherRootRehashed
				), new HashSet<>()
		);
		assertEquals( 1, inIndexOf.get( RootEntity.class ).size() );
		assertTrue( inIndexOf.get( RootEntity.class ).contains( RootEntity.class ) );

		assertEquals( 1, inIndexOf.get( AnotherRootEntity.class ).size() );
		assertTrue( inIndexOf.get( AnotherRootEntity.class ).contains( AnotherRootEntity.class ) );

		assertEquals( 2, inIndexOf.get( SubEntity.class ).size() );
		assertTrue( inIndexOf.get( SubEntity.class ).contains( RootEntity.class ) );
		assertTrue( inIndexOf.get( SubEntity.class ).contains( AnotherRootEntity.class ) );

		this.assertStringDeletion( fromRootRehashed, "MAYBE_ROOT_NOT_NAMED_ID" );
		this.assertStringDeletion( fromRootRehashed, "recursiveSelf.MAYBE_ROOT_NOT_NAMED_ID" );
		this.assertStringDeletion( fromRootRehashed, "recursiveSelf.recursiveSelf.MAYBE_ROOT_NOT_NAMED_ID" );
		this.assertStringDeletion( fromRootRehashed, "otherEntity.SUB_NOT_NAMED_ID" );
		this.assertStringDeletion( fromRootRehashed, "recursiveSelf.otherEntity.SUB_NOT_NAMED_ID" );

		//well, we don't have to check this, but we can do it at least for the root
		//default id name is relevant as well
		this.assertStringDeletion( fromAnotherRootRehashed, "id" );

		// FIXME: unit-test the TermDeletion stuff for Numeric ids?
		//... nvm. as of Hibernate Search 5.3.0.Beta1 all ids are Strings in the document
	}

	@Test
	public void testInheritanceIndexRelevancy() {
		TypeMetadata typeMetadata = this.metadataProvider.getTypeMetadataFor( Root.class );
		ExtendedTypeMetadata rehashed = this.metadataExtender.rehash(
				Collections.singletonList( typeMetadata ),
				new ArrayList<>( Collections.singletonList( Sub.class ) )
		).get( 0 );


		Set<Class<?>> indexRelevantEntities = MetadataUtil.calculateIndexRelevantEntities(
				Collections.singletonList(
						rehashed
				), new HashSet<>( Collections.singletonList( Sub.class ) )
		);
		assertEquals( 2, indexRelevantEntities.size() );
	}

	@Test
	public void testInheritanceInIndex() {
		TypeMetadata typeMetadata = this.metadataProvider.getTypeMetadataFor( Root.class );
		ExtendedTypeMetadata rehashed = this.metadataExtender.rehash(
				Collections.singletonList( typeMetadata ),
				new ArrayList<>( Collections.singletonList( Sub.class ) )
		).get( 0 );

		Map<Class<?>, Set<Class<?>>> inIndexOf = MetadataUtil.calculateInIndexOf(
				Collections.singletonList( rehashed ),
				new HashSet<>( Arrays.asList( Sub.class, Useless.class ) )
		);
		assertEquals( 2, inIndexOf.size() );
		assertTrue( inIndexOf.get( Root.class ).contains( Root.class ) );
		assertTrue( inIndexOf.get( Sub.class ).contains( Root.class ) );
	}

	@Test
	public void testInheritanceCopyProperties() {
		TypeMetadata typeMetadata = this.metadataProvider.getTypeMetadataFor( Root.class );
		ExtendedTypeMetadata rehashed = this.metadataExtender.rehash(
				Collections.singletonList( typeMetadata ),
				new ArrayList<>( Collections.singletonList( Sub.class ) )
		).get( 0 );

		assertEquals(
				rehashed.getIdFieldNamesForType().get( Root.class ),
				rehashed.getIdFieldNamesForType().get( Sub.class )
		);
		assertEquals(
				rehashed.getIdPropertyNameForType().get( Root.class ),
				rehashed.getIdPropertyNameForType().get( Sub.class )
		);
		assertEquals(
				rehashed.getIdPropertyAccessorForType().get( Root.class ),
				rehashed.getIdPropertyAccessorForType().get( Sub.class )
		);
	}

	@Test
	public void testInheritanceRelevantSubclasses() {
		Map<Class<?>, Set<Class<?>>> map = this.metadataExtender.calculateRelevantSubclasses(
				Arrays.asList(
						Root.class,
						Sub.class
				)
		);
		assertEquals( 2, map.size() );
		assertEquals( 2, map.get( Root.class ).size() );
		assertTrue( map.get( Root.class ).contains( Root.class ) );
		assertTrue( map.get( Root.class ).contains( Sub.class ) );
		assertEquals( 1, map.get( Sub.class ).size() );
		assertTrue( map.get( Sub.class ).contains( Sub.class ) );
	}

	@Indexed
	public static class Root {

		@DocumentId
		private Integer id;

		@Field
		private String field;

	}

	public static class Sub extends Root {

	}

	public static class Useless {

	}

	private void assertStringDeletion(ExtendedTypeMetadata rehashed, String fieldName) {
		assertEquals(
				SingularTermDeletionQuery.Type.STRING,
				rehashed.getSingularTermDeletionQueryTypeForIdFieldName()
						.get(
								fieldName
						)
		);
	}

}
