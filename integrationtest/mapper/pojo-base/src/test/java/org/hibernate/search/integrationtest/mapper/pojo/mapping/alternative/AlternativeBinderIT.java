/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.alternative;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.AlternativeDiscriminator;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test a particular use case for type bridges to provide a feature similar to
 * {@code @AnalyzerDiscriminator} from Hibernate Search 5.
 */
@TestForIssue(jiraKey = "HSEARCH-3311")
public class AlternativeBinderIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void smoke() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@AlternativeDiscriminator
			Language language;
			@MultiLanguageField(projectable = Projectable.YES)
			String title;
			@MultiLanguageField(name = "content")
			String text;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "title_en", String.class,
						b2 -> b2.analyzerName( "text_en" ).projectable( Projectable.YES ) )
				.field( "title_fr", String.class,
						b2 -> b2.analyzerName( "text_fr" ).projectable( Projectable.YES ) )
				.field( "title_de", String.class,
						b2 -> b2.analyzerName( "text_de" ).projectable( Projectable.YES ) )
				.field( "content_en", String.class,
						b2 -> b2.analyzerName( "text_en" ).projectable( Projectable.DEFAULT ) )
				.field( "content_fr", String.class,
						b2 -> b2.analyzerName( "text_fr" ).projectable( Projectable.DEFAULT ) )
				.field( "content_de", String.class,
						b2 -> b2.analyzerName( "text_de" ).projectable( Projectable.DEFAULT ) )
		);

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity entity1 = new IndexedEntity();
		entity1.id = 1;
		entity1.language = Language.ENGLISH;
		entity1.title = "A title in English";
		entity1.text = "Some content in English";
		IndexedEntity entity2 = new IndexedEntity();
		entity2.id = 2;
		entity2.language = Language.FRENCH;
		entity2.title = "Un titre en Français";
		entity2.text = "Du contenu en Français";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b
							.field( "title_en", entity1.title )
							.field( "content_en", entity1.text ) )
					.add( "2", b -> b
							.field( "title_fr", entity2.title )
							.field( "content_fr", entity2.text ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void discriminator_missing() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			Language language;
			@MultiLanguageField(projectable = Projectable.YES)
			String title;
			@MultiLanguageField(name = "content")
			String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.expectCustomBeans()
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "No property annotated with @Alternative(id = null).",
								"There must be exactly one such property in order to map property 'text' to multi-alternative fields." )
						.failure( "No property annotated with @Alternative(id = null).",
								"There must be exactly one such property in order to map property 'title' to multi-alternative fields." ) );
	}

	@Test
	public void discriminator_conflict() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@AlternativeDiscriminator
			Language language;
			@AlternativeDiscriminator
			Language otherLanguage;
			@MultiLanguageField(projectable = Projectable.YES)
			String title;
			@MultiLanguageField(name = "content")
			String text;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.expectCustomBeans()
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.failure( "Multiple properties annotated with @Alternative(id = null).",
								"There must be exactly one such property in order to map property 'text' to multi-alternative fields." )
						.failure( "Multiple properties annotated with @Alternative(id = null).",
								"There must be exactly one such property in order to map property 'title' to multi-alternative fields." ) );
	}
}
