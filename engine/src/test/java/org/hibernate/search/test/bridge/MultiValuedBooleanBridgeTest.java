/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;

@TestForIssue(jiraKey = "HSEARCH-3407")
public class MultiValuedBooleanBridgeTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( ParentEntity.class, ChildEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void test() {
		ChildEntity childEntity1 = new ChildEntity( 1L, true );
		ChildEntity childEntity2 = new ChildEntity( 2L, false );
		ParentEntity yourEntity1 = new ParentEntity( 1L, Arrays.asList( childEntity1, childEntity2 ) );

		helper.add( yourEntity1 );

		QueryBuilder qb = helper.queryBuilder( ParentEntity.class );

		// finding ParentEntities with a children having boolean property "true" should match 1L
		Query query = qb.keyword().onField( "children.booleanProperty" ).matching( true ).createQuery();

		helper.assertThat( query )
				.from( ParentEntity.class )
				.matchesExactlyIds( 1L );

		// finding ParentEntities with a children having boolean property "false" should also match 1L
		query = qb.keyword().onField( "children.booleanProperty" ).matching( false ).createQuery();
		helper.assertThat( query )
				.from( ParentEntity.class )
				.matchesExactlyIds( 1L );
	}

	@Indexed
	public static class ParentEntity {

		@DocumentId
		private Long id;

		@IndexedEmbedded
		private List<ChildEntity> children = new ArrayList<>();

		protected ParentEntity() {
		}

		public ParentEntity(Long id, List<ChildEntity> childEntities) {
			this.id = id;
			children.addAll( childEntities );
		}

		public Long getId() {
			return id;
		}

		public List<ChildEntity> getChildren() {
			return Collections.unmodifiableList( children );
		}

		public void addChildEntity(ChildEntity child) {
			child.setParent( this );
			this.children.add( child );
		}

	}

	@Indexed
	public static class ChildEntity {

		@DocumentId
		private Long id;

		@ContainedIn
		private ParentEntity parent;

		@Field
		private boolean booleanProperty;

		protected ChildEntity() {
		}

		public ChildEntity(Long id, boolean booleanProperty) {
			this.id = id;
			this.booleanProperty = booleanProperty;
		}

		public Long getId() {
			return id;
		}

		public boolean getBooleanProperty() {
			return booleanProperty;
		}

		public void setBooleanProperty(boolean booleanProperty) {
			this.booleanProperty = booleanProperty;
		}

		protected void setParent(ParentEntity parent) {
			this.parent = parent;
		}

	}
}
