/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author Bayo Erinle
 */
@Embeddable
public class LegacyTirePK implements Serializable {

	private String carId;
	private String tireId;

	@Column(name = "CAR_ID")
	public String getCarId() {
		return carId;
	}

	public void setCarId(String carId) {
		this.carId = carId;
	}

	@Column(name = "TIRE_ID")
	public String getTireId() {
		return tireId;
	}

	public void setTireId(String tireId) {
		this.tireId = tireId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( carId == null ) ? 0 : carId.hashCode() );
		result = prime * result + ( ( tireId == null ) ? 0 : tireId.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		LegacyTirePK other = (LegacyTirePK) obj;
		if ( carId == null ) {
			if ( other.carId != null ) {
				return false;
			}
		}
		else {
			if ( !carId.equals( other.carId ) ) {
				return false;
			}
		}
		if ( tireId == null ) {
			if ( other.tireId != null ) {
				return false;
			}
		}
		else {
			if ( !tireId.equals( other.tireId ) ) {
				return false;
			}
		}
		return true;
	}

}
