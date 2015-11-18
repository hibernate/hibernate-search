/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.test;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.ClassBridges;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.bridge.builtin.DoubleBridge;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed(index = "golfplayer")
@ClassBridges({
		@ClassBridge(name = "fullName", impl = NameConcatenationBridge.class),
		@ClassBridge(name = "age", impl = AgeBridge.class)
})
public class GolfPlayer {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GOLF_PLAYER_SEQ")
	@SequenceGenerator(
			name = "GOLF_PLAYER_SEQ",
			sequenceName = "golfplayer_sequence",
			allocationSize = 20
		)
	@DocumentId
	private Long id;

	@Field(indexNullAs = "<NULL>")
	private String firstName;

	@Field
	private String lastName;

	@Field(indexNullAs = "false")
	private Boolean active;

	@Field(indexNullAs = "1970-01-01")
	private Date dateOfBirth;

	@Field
	private double handicap;

	@Field(bridge = @FieldBridge(impl = DoubleBridge.class))
	private double puttingStrength;

	@Field(indexNullAs = "-1")
	private Integer driveWidth;

	@IndexedEmbedded
	private Ranking ranking;

	GolfPlayer() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public double getHandicap() {
		return handicap;
	}

	public void setHandicap(double handicap) {
		this.handicap = handicap;
	}

	public double getPuttingStrength() {
		return puttingStrength;
	}

	public void setPuttingStrength(double puttingStrength) {
		this.puttingStrength = puttingStrength;
	}

	public Integer getDriveWidth() {
		return driveWidth;
	}

	public void setDriveWidth(Integer driveWidth) {
		this.driveWidth = driveWidth;
	}

	public Ranking getRanking() {
		return ranking;
	}

	public void setRanking(Ranking ranking) {
		this.ranking = ranking;
	}

	public static class Builder {

		private String firstName;
		private String lastName;
		private boolean active;
		private Date dateOfBirth;
		private double handicap;
		private double puttingStrength;
		private Integer driveWidth;
		private Integer ranking;

		public Builder firstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public Builder lastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public Builder active(boolean active) {
			this.active = active;
			return this;
		}

		public Builder dateOfBirth(Date dateOfBirth) {
			this.dateOfBirth = dateOfBirth;
			return this;
		}

		public Builder handicap(double handicap) {
			this.handicap = handicap;
			return this;
		}

		public Builder driveWidth(int driveWidth) {
			this.driveWidth = driveWidth;
			return this;
		}

		public Builder ranking(int ranking) {
			this.ranking = ranking;
			return this;
		}

		public Builder puttingStrength(double puttingStrength) {
			this.puttingStrength = puttingStrength;
			return this;
		}

		GolfPlayer build() {
			GolfPlayer player = new GolfPlayer();

			player.setFirstName( firstName );
			player.setLastName( lastName );
			player.setActive( active );
			player.setDateOfBirth( dateOfBirth );
			player.setHandicap( handicap );
			player.setPuttingStrength( puttingStrength );
			player.setDriveWidth( driveWidth );
			if ( ranking != null ) {
				player.setRanking( new Ranking( BigInteger.valueOf( ranking ) ) );
			}
			return player;
		}
	}
}
