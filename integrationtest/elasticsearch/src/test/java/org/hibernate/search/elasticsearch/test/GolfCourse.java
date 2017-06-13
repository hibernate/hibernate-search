/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class GolfCourse {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GOLF_COURSE_SEQ")
	@SequenceGenerator(name = "GOLF_COURSE_SEQ", sequenceName = "golfcourse_sequence", allocationSize = 20)
	@DocumentId
	private Long id;

	@Field
	private String name;

	@Field
	private double rating;

	@ContainedIn
	@ManyToMany
	private Set<GolfPlayer> playedBy = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL)
	@IndexedEmbedded
	@OrderColumn
	private List<Hole> holes = new ArrayList<>();

	GolfCourse() {
	}

	GolfCourse(String name, double rating, Hole... holes) {
		this.name = name;
		this.rating = rating;
		if ( holes != null ) {
			for ( Hole hole : holes ) {
				this.holes.add( hole );
			}
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public Set<GolfPlayer> getPlayedBy() {
		return playedBy;
	}

	public void setPlayedBy(Set<GolfPlayer> playedBy) {
		this.playedBy = playedBy;
	}

	public List<Hole> getHoles() {
		return holes;
	}

	public void setHoles(List<Hole> holes) {
		this.holes = holes;
	}
}
