/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.timeout;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Clock {

	public static final List<String> FIELD_NAMES = Collections.unmodifiableList(
			IntStream.range( 0 , 20 )
					.mapToObj( i -> "description_" + i )
					.collect( Collectors.toList() )
	);
	// Taken from our current documentation (https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/):
	public static final String[] TEXTS = {
			"Fine-grained dirty checking consists in keeping track of which properties are dirty in a given entity, so as to only reindex"
					+ "\"containing\" entities that actually use at least one of the dirty properties.",
			"Whenever we create a type node in the reindexing resolver building tree, we take care to determine all the possible concrete "
					+ "entity types for the considered type, and create one reindexing resolver type node builder per possible entity type.",
			"The only thing left to do is register the path that is depended on (in our example, longField). With this path registered, "
					+ "we will be able to build a PojoPathFilter, so that whenever SecondLevelEmbeddedEntityClass changes, we will walk through the tree, but not all the tree: "
					+ "if at some point we notice that a node is relevant only if longField changed, but the \"dirtiness state\" tells us that longField did not change, "
					+ "we can skip a whole branch of the tree, avoiding useless lazy loading and reindexing.",
			// William Blake's poetries:
			"O Rose thou art sick. The invisible worm, That flies in the night In the howling storm: Has found out thy bed Of crimson joy: And his dark secret love Does thy life destroy.",
			"Piping down the valleys wild, Piping songs of pleasant glee, On a cloud I saw a child, And he laughing said to me: "
					+ " 'Pipe a song about a Lamb!'So I piped with merry cheer. 'Piper, pipe that song again.'So I piped: he wept to hear. "
					+ " 'Drop thy pipe, thy happy pipe; Sing thy songs of happy cheer!'So I sung the same again, While he wept with joy to hear. "
					+ " 'Piper, sit thee down and write In a book, that all may read.'So he vanished from my sight; And I plucked a hollow reed, "
					+ " And I made a rural pen, And I stained the water clear, And I wrote my happy songs Every child may joy to hear.",
			// Lord Byron Childe Harold's Pilgrimage:
			" There is a pleasure in the pathless woods, There is a rapture on the lonely shore, There is society where none intrudes,"
					+ " By the deep Sea, and music in its roar: I love not Man the less, but Nature more, From these our interviews, "
					+ " in which I steal From all I may be, or have been before, To mingle with the Universe, "
					+ " and feel What I can ne'er express, yet cannot all conceal. Roll on, thou deep and dark blue Ocean--roll! "
					+ " Ten thousand fleets sweep over thee in vain; Man marks the earth with ruin--his control Stops with the shore; "
					+ "--upon the watery plain The wrecks are all thy deed, nor doth remain A shadow of man's ravage, save his own, "
					+ " When for a moment, like a drop of rain, He sinks into thy depths with bubbling groan, Without a grave, unknelled, "
					+ " uncoffined, and unknown. His steps are not upon thy paths,--thy fields Are not a spoil for him,--thou dost "
					+ "arise And shake him from thee; the vile strength he wields For earth's destruction thou dost all despise, "
					+ "Spurning him from thy bosom to the skies, And send'st him, shivering in thy playful spray And howling, to his gods,"
					+ "where haply lies His petty hope in some near port or bay, And dashest him again to earth: —there let him lay."
	};

	public static final String BUZZ_WORDS = "solar rose system house family random art painting modern ocean discovery butterfly violet ignore spelling custom legacy tree search avoid nested reference thread concurrency scaling reindexing node track";

	private Long id;
	private String model;
	private String brand;
	private Long durability;
	private String description;

	public Clock() {
	}

	public Clock(Long id, String model, String brand, Long durability, String description) {
		this.id = id;
		this.model = model;
		this.brand = brand;
		this.durability = durability;
		this.description = description;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	@Field
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}


	@Field
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	@Field
	public Long getDurability() {
		return durability;
	}

	public void setDurability(Long durability) {
		this.durability = durability;
	}

	@Field(name = "description_0")
	@Field(name = "description_1")
	@Field(name = "description_2")
	@Field(name = "description_3")
	@Field(name = "description_4")
	@Field(name = "description_5")
	@Field(name = "description_6")
	@Field(name = "description_7")
	@Field(name = "description_8")
	@Field(name = "description_9")
	@Field(name = "description_10")
	@Field(name = "description_11")
	@Field(name = "description_12")
	@Field(name = "description_13")
	@Field(name = "description_14")
	@Field(name = "description_15")
	@Field(name = "description_16")
	@Field(name = "description_17")
	@Field(name = "description_18")
	@Field(name = "description_19")
	@Column(length = 2000)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
