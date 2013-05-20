/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
