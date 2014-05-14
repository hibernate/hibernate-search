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
public class LegacyCarPlantPK implements Serializable {

	private String plantId;
	private String carId;

	@Column(name = "PLANT_ID")
	public String getPlantId() {
		return plantId;
	}

	public void setPlantId(String plantId) {
		this.plantId = plantId;
	}

	@Column(name = "CAR_ID")
	public String getCarId() {
		return carId;
	}

	public void setCarId(String carId) {
		this.carId = carId;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		LegacyCarPlantPK that = (LegacyCarPlantPK) o;
		if ( carId != null ? !carId.equals( that.carId ) : that.carId != null ) {
			return false;
		}
		if ( plantId != null ? !plantId.equals( that.plantId ) : that.plantId != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = plantId != null ? plantId.hashCode() : 0;
		result = 31 * result + ( carId != null ? carId.hashCode() : 0 );
		return result;
	}
}
