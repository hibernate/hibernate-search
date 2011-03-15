/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		LegacyTirePK other = (LegacyTirePK) obj;
		if ( carId == null ) {
			if ( other.carId != null )
				return false;
		}
		else if ( !carId.equals( other.carId ) )
			return false;
		if ( tireId == null ) {
			if ( other.tireId != null )
				return false;
		}
		else if ( !tireId.equals( other.tireId ) )
			return false;
		return true;
	}

}
