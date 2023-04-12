/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.highlighting;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::basics[]
@Entity(name = BookWithHighlights.NAME)
@Indexed
public class BookWithHighlights {

	public static final String NAME = "Book";

	@Id
	private Integer id;

	@FullTextField(analyzer = "english") // <1>
	private String title;

	@FullTextField(analyzer = "english", highlightable = Highlightable.ANY) // <2>
	@Column(length = 10000)
	private String description;

	@FullTextField(analyzer = "english", projectable = Projectable.YES, termVector = TermVector.WITH_POSITIONS_OFFSETS) // <3>
	@Column(length = 10000)
	@ElementCollection
	private List<String> text;

	@GenericField // <4>
	@Column(length = 10000)
	@ElementCollection
	private List<String> keywords;

	@FullTextField(analyzer = "english", highlightable = { Highlightable.PLAIN, Highlightable.UNIFIED }) // <5>
	private String author;
}
// end::basics[]
