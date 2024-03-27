/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.highlighting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HighlightingDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
		initData();
	}

	@Test
	void entryPoint() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// tag::basics[]
			SearchSession searchSession = /* ... */ // <1>
					// end::basics[]
					Search.session( entityManager );
			// tag::basics[]

			List<List<String>> result = searchSession.search( Book.class ) // <2>
					.select( f -> f.highlight( "title" ) ) // <3>
					.where( f -> f.match().field( "title" ).matching( "mystery" ) ) // <4>
					.fetchHits( 20 ); // <5>
			// end::basics[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList( "The Boscombe Valley <em>Mystery</em>" )
			);
		} );
	}

	@Test
	void composite() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::composite[]
			List<List<?>> result = searchSession.search( Book.class ) // <1>
					.select( f -> f.composite().from(
							f.id(), // <2>
							f.field( "title", String.class ), // <3>
							f.highlight( "description" ) // <4>
					).asList() )
					.where( f -> f.match().fields( "title", "description" ).matching( "scandal" ) ) // <5>
					.fetchHits( 20 ); // <6>
			// end::composite[]
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							1,
							"A Scandal in Bohemia",
							Collections.emptyList()
					)
			);
		} );
	}

	@Test
	void veryBasicConfig() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::very-basic-config[]
			List<List<?>> result = searchSession.search( Book.class )
					.select( f -> f.composite().from(
							f.highlight( "title" ),
							f.highlight( "description" )
					).asList() )
					.where( f -> f.match().fields( "title", "description" ).matching( "scandal" ) ) // <1>
					.highlighter( f -> f.plain().noMatchSize( 100 ) ) // <2>
					.fetchHits( 20 ); // <3>
			// end::very-basic-config[]
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							Collections.singletonList( "A <em>Scandal</em> in Bohemia" ),
							Collections.singletonList(
									"The King of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned" )
					)
			);
		} );
	}

	@Test
	void basicConfig() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::basic-config[]
			List<List<?>> result = searchSession.search( Book.class )
					.select( f -> f.composite().from(
							f.highlight( "title" ), // <1>
							f.highlight( "description" ).highlighter( "customized-plain-highlighter" ) // <2>
					).asList() )
					.where( f -> f.match().fields( "title", "description" ).matching( "scandal" ) )
					.highlighter( f -> f.plain().tag( "<b>", "</b>" ) ) // <3>
					.highlighter( "customized-plain-highlighter", f -> f.plain().noMatchSize( 100 ) ) // <4>
					.fetchHits( 20 ); // <5>
			// end::basic-config[]
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							Collections.singletonList( "A <b>Scandal</b> in Bohemia" ),
							Collections.singletonList(
									"The King of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned" )
					)
			);
		} );
	}

	@Test
	void variousHighlighterTypesPlain() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<List<String>> result =
					// tag::various-highlighter-types-plain[]
					searchSession.search( Book.class )
							.select( f -> f.highlight( "title" ) )
							.where( f -> f.match().fields( "title", "description" ).matching( "scandal" ) )
							.highlighter( f -> f.plain() /* ... */ ) // <1>
							.fetchHits( 20 );
			// end::various-highlighter-types-plain[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList( "A <em>Scandal</em> in Bohemia" )
			);
		} );
	}

	@Test
	void variousHighlighterTypesUnified() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<List<String>> result =
					// tag::various-highlighter-types-unified[]
					searchSession.search( Book.class )
							.select( f -> f.highlight( "title" ) )
							.where( f -> f.match().fields( "title", "description" ).matching( "scandal" ) )
							.highlighter( f -> f.unified() /* ... */ ) // <1>
							.fetchHits( 20 );
			// end::various-highlighter-types-unified[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList( "A <em>Scandal</em> in Bohemia" )
			);
		} );
	}

	@Test
	void variousHighlighterTypesFvh() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<List<String>> result =
					// tag::various-highlighter-types-fvh[]
					searchSession.search( Book.class )
							.select( f -> f.highlight( "description" ) )
							.where( f -> f.match().fields( "title", "description" ).matching( "scandal" ) )
							.highlighter( f -> f.fastVector() /* ... */ ) // <1>
							.fetchHits( 20 );
			// end::various-highlighter-types-fvh[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.emptyList()
			);

		} );
	}

	@Test
	void basicTags() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::basic-tags[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "title" ) )
					.where( f -> f.match().fields( "title" ).matching( "scandal" ) )
					.highlighter( f -> f.unified().tag( "<strong>", "</strong>" ) ) // <1>
					.fetchHits( 20 );
			// end::basic-tags[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList(
							"A <strong>Scandal</strong> in Bohemia"
					)
			);
		} );
	}

	@Test
	void fvhTags() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<List<String>> result;
			// tag::fvh-tags[]
			result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "scandal" ) )
					.highlighter( f -> f.fastVector()
							.tags( // <1>
									Arrays.asList( "<em class=\"class1\">", "<em class=\"class2\">" ),
									"</em>"
							) )
					.fetchHits( 20 );
			// end::fvh-tags[]
			assertThat( result ).containsExactlyInAnyOrder();

			// tag::fvh-tags[]
			result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "scandal" ) )
					.highlighter( f -> f.fastVector()
							.tags( // <2>
									Arrays.asList( "<em>", "<strong>" ),
									Arrays.asList( "</em>", "</strong>" )
							) )
					.fetchHits( 20 );
			// end::fvh-tags[]
			assertThat( result ).containsExactlyInAnyOrder();
		} );
	}

	@Test
	void fvhSchema() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::fvh-schema[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "scandal" ) )
					.highlighter( f -> f.fastVector()
							.tagSchema( HighlighterTagSchema.STYLED ) // <1>
					)
					.fetchHits( 20 );
			// end::fvh-schema[]
			assertThat( result ).containsExactlyInAnyOrder();
		} );
	}

	@Test
	void fvhSchemaAlternative() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::fvh-schema-alternative[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "scandal" ) )
					.highlighter( f -> f.fastVector()
							.tags( Arrays.asList(
									"<em class=\"hlt1\">",
									"<em class=\"hlt2\">",
									"<em class=\"hlt3\">",
									"<em class=\"hlt4\">",
									"<em class=\"hlt5\">",
									"<em class=\"hlt6\">",
									"<em class=\"hlt7\">",
									"<em class=\"hlt8\">",
									"<em class=\"hlt9\">",
									"<em class=\"hlt10\">"
							), "</em>" ) // <1>
					)
					.fetchHits( 20 );
			// end::fvh-schema-alternative[]
			assertThat( result ).containsExactlyInAnyOrder();
		} );
	}

	@Test
	void encoder() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::basic-encoder[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "title" ) )
					.where( f -> f.match().fields( "title" ).matching( "scandal" ) )
					.highlighter( f -> f.unified().encoder( HighlighterEncoder.HTML ) ) // <1>
					.fetchHits( 20 );
			// end::basic-encoder[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList(
							"A <em>Scandal</em> in Bohemia"
					)
			);
		} );
	}

	@Test
	void noMatchSize() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::no-match-size[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.bool()
							.must( f.match().fields( "title" ).matching( "scandal" ) ) // <1>
							.should( f.match().fields( "description" ).matching( "scandal" ) ) // <2>
					)
					.highlighter( f -> f.fastVector().noMatchSize( 100 ) ) // <3>
					.fetchHits( 20 );
			// end::no-match-size[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList(
							"The King of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned"
					)
			);
		} );
	}

	@Test
	void fragmentSize() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::fragment-size[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "king" )
					)
					.highlighter( f -> f.fastVector()
							.fragmentSize( 50 ) // <1>
							.numberOfFragments( 2 ) // <2>
					)
					.fetchHits( 20 );
			// end::fragment-size[]
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							"The <em>King</em> of Bohemia engages Holmes to recover an indiscreet",
							"marriage to a daughter of the <em>King</em> of Scandinavia. In disguise"
					)
			);
		} );
	}

	@Test
	void order() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::basic-order[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.bool() // <1>
							.should( f.match().fields( "description" ).matching( "king" ) )
							.should( f.match().fields( "description" ).matching( "souvenir" ).boost( 10.0f ) )
					)
					.highlighter( f -> f.fastVector().orderByScore( true ) ) // <2>
					.fetchHits( 20 );
			// end::basic-order[]
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							"of herself for the <em>King</em>. The <em>king</em> allows Holmes to retain the portrait as a <em>souvenir</em>.",
							"The <em>King</em> of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned",
							"which would derail his marriage to a daughter of the <em>King</em> of Scandinavia. In disguise, Holmes witnesses Adler",
							"photograph's hiding place. But when Holmes and the <em>king</em> return to retrieve the photo, they find Adler has"
					)
			);
		} );
	}

	@Test
	void fragmenter() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::basic-fragmenter[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "souvenir" ) )
					.highlighter( f -> f.plain().fragmenter( HighlighterFragmenter.SIMPLE ) ) // <1>
					.fetchHits( 20 );
			// end::basic-fragmenter[]
			assertThat( result ).containsExactlyInAnyOrder(
					Collections.singletonList(
							" retain the portrait as a <em>souvenir</em>."
					)
			);
		} );
	}

	@Test
	void scannerDsl() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// @formatter:off
			// tag::scanner-dsl[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "king" ) )
					.highlighter( f -> f.fastVector()
							.boundaryScanner() // <1>
									.word() // <2>
									.locale( Locale.ENGLISH ) // <3>
									.end() // <4>
							/* ... */ // <5>
					)
					.fetchHits( 20 );
			// end::scanner-dsl[]
			// @formatter:on
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							"The <em>King</em> of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned ",
							"which would derail his marriage to a daughter of the <em>King</em> of Scandinavia. In disguise, Holmes witnesses Adler",
							"photograph's hiding place. But when Holmes and the <em>king</em> return to retrieve the photo, they find Adler has",
							"for Holmes and a portrait of herself for the <em>King</em>. The <em>king</em> allows Holmes to retain the portrait as a souvenir"
					)
			);
		} );
	}

	@Test
	void scannerLambda() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// @formatter:off
			// tag::scanner-lambda[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "king" ) )
					.highlighter( f -> f.fastVector()
							.boundaryScanner(
									bs -> bs.word() // <1>
							)
							/* ... */ // <2>
					)
					.fetchHits( 20 );
			// end::scanner-lambda[]
			// @formatter:on
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							"The <em>King</em> of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned ",
							"which would derail his marriage to a daughter of the <em>King</em> of Scandinavia. In disguise, Holmes witnesses Adler",
							"photograph's hiding place. But when Holmes and the <em>king</em> return to retrieve the photo, they find Adler has",
							"for Holmes and a portrait of herself for the <em>King</em>. The <em>king</em> allows Holmes to retain the portrait as a souvenir"
					)
			);
		} );
	}

	@Test
	void scannerCharacters() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// @formatter:off
			// tag::scanner-char[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "scene" ) )
					.highlighter( f -> f.fastVector()
							.boundaryScanner() // <1>
									.chars() // <2>
									.boundaryChars( "\n" ) // <3>
									.boundaryMaxScan( 1000 ) // <4>
									.end() // <5>
							/* ... */ // <6>
					)
					.fetchHits( 20 );
			// end::scanner-char[]
			// @formatter:on
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							"McCarthy, and another local landowner, John Turner, are both Australian expatriates, and Lestrade was originally engaged by Turner's daughter, Alice, who believes James is innocent. Holmes interviews James, and then inspects the <em>scene</em> of the murder, deducing a third man was present."
					)
			);
		} );
	}

	@Test
	void phraseLimit() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::phrase-limit[]
			List<List<String>> result = searchSession.search( Book.class )
					.select( f -> f.highlight( "description" ) )
					.where( f -> f.match().fields( "description" ).matching( "bank" ) )
					.highlighter( f -> f.fastVector()
							.phraseLimit( 1 ) // <1>
					)
					.fetchHits( 20 );
			// end::phrase-limit[]
			assertThat( result ).containsExactlyInAnyOrder(
					Arrays.asList(
							// note that there's a second "bank" occurrence, but since we only wanted one phrase highlighted it is ignored:
							"contacts a police inspector and the manager of a nearby <em>bank</em>. With Watson, they hide in the bank vault and catch"
					)
			);
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {

			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "A Scandal in Bohemia" );
			book1.setDescription(
					"The King of Bohemia engages Holmes to recover an indiscreet photograph showing him with the renowned beauty, "
							+ "adventuress and opera singer Irene Adler - the revelation of which would derail his marriage to a daughter of "
							+ "the King of Scandinavia. In disguise, Holmes witnesses Adler marry the man she truly loves,"
							+ " then by means of an elaborate stratagem discovers the photograph's hiding place. "
							+ "But when Holmes and the king return to retrieve the photo, they find Adler has fled the country with it, "
							+ "leaving behind a letter for Holmes and a portrait of herself for the King. "
							+ "The king allows Holmes to retain the portrait as a souvenir." );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Red-Headed League" );
			book2.setDescription(
					"Jabez Wilson, a pawnbroker, consults Holmes about a job, gained only because of his red hair, "
							+ "which took him away from his shop for long periods each day; the job for to simply copy the Encyclopædia Britannica. "
							+ "After eight weeks, he was suddenly informed that the job ended. "
							+ "After some investigation at Wilson's shop, Holmes contacts a police inspector and the manager of a nearby bank. "
							+ "With Watson, they hide in the bank vault and catch two thieves who had dug a tunnel from the shop "
							+ "while Wilson was at the decoy copying job." );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "A Case of Identity" );
			book3.setDescription(
					"Against the wishes of her stepfather, Mary Sutherland has become engaged to Hosmer Angel. "
							+ "On the morning of their wedding Hosmer elicits a promise that Mary will remain faithful to him "
							+ "\"even if something quite unforeseen\" occurs, then mysteriously disappears en route to the church. "
							+ "Holmes deduces that Hosmer was Mary's stepfather in disguise, the charade a bid to keep "
							+ "Mary a spinster and thus maintain access to her inheritance." );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Boscombe Valley Mystery" );
			book4.setDescription(
					"Inspector Lestrade asks for Holmes's help after Charles McCarthy is murdered, and his son, James, is implicated.\n"
							+ "McCarthy, and another local landowner, John Turner, are both Australian expatriates, "
							+ "and Lestrade was originally engaged by Turner's daughter, Alice, who believes James is innocent. "
							+ "Holmes interviews James, and then inspects the scene of the murder, deducing a third man was present.\n"
							+ "Realising Holmes has solved the case, Turner confesses to the crime, "
							+ "revealing that McCarthy was blackmailing him due to Turner's criminal past.\n" );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}
}
