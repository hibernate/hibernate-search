/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Immutable;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;


// This does not work because Hibernate ORM doesn't support instantiating records
// for entities/embeddables at the moment.
@Ignore
public class IndexedEmbeddedRecordIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "myRecord", b2 -> b2
						.field( "text", String.class, b3 -> b3.analyzerName( AnalyzerNames.DEFAULT ) )
						.field( "integer", Integer.class )
						.field( "keywords", String.class, b3 -> b3.multiValued( true ) )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			MyRecord myRecord = new MyRecord( "someText", 42, List.of( "someText2", "someText3" ) );

			entity1.myRecord = myRecord;

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "myRecord", b2 -> b2
									.field( "text", myRecord.text )
									.field( "integer", myRecord.integer )
									.field( "keywords", myRecord.keywords.get( 0 ) )
									.field( "keywords", myRecord.keywords.get( 1 ) )
							)
					);
		} );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Embedded
		@IndexedEmbedded
		private MyRecord myRecord;
	}

	@Immutable
	@Embeddable
	public record MyRecord(@FullTextField String text, @GenericField Integer integer,
							@ElementCollection @KeywordField List<String> keywords) {
	}

}
