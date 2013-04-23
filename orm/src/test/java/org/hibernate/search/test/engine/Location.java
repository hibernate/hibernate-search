/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.CacheFromIndex;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;

import static org.hibernate.search.annotations.FieldCacheType.CLASS;
import static org.hibernate.search.annotations.FieldCacheType.ID;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Gustavo Fernandes
 */
@Entity
@Indexed(index = "numeric_field_test")
@CacheFromIndex({ CLASS, ID })
public class Location {

	@Id
	@DocumentId(name = "overriddenFieldName")
	@NumericField
	private int id;

	public int getId() {
		return id;
	}

	@IndexedEmbedded
	private Country country;

	@Field(name = "myCounter")
	@NumericField(forField = "myCounter")
	private Long counter;

	@Field(store = Store.YES)
	@NumericField(forField = "latitude", precisionStep = 1)
	private double latitude;

	@Field(store = Store.YES)
	@NumericField(forField = "longitude")
	private Double longitude;

	@Field
	@NumericField
	private Integer ranking;

	@Field
	@NumericField
	@FieldBridge(impl = BigDecimalNumericFieldBridge.class)
	private BigDecimal visibleStars;

	@Field(store = Store.YES)
	@NumericField
	@FieldBridge(impl = CoordinatesPairFieldBridge.class)
	private String coordinatePair = "1;2";

	@Field
	private String description;

	@OneToMany(mappedBy = "location", cascade = { CascadeType.ALL })
	@IndexedEmbedded
	private Collection<PinPoint> pinPoints = new ArrayList<PinPoint>();

	@Fields({
			@Field(name = "strMultiple"),
			@Field
	})
	@NumericFields({
			@NumericField(forField = "strMultiple")
	})
	private Double multiple;

	public Location() {
	}

	public Location(int id, Long counter, double latitude, Double longitude,
					Integer ranking, String description, Double multiple, Country country, BigDecimal visibleStars) {
		this.id = id;
		this.counter = counter;
		this.longitude = longitude;
		this.latitude = latitude;
		this.ranking = ranking;
		this.description = description;
		this.multiple = multiple;
		this.country = country;
		this.visibleStars = visibleStars;
	}

	public void addPinPoints(PinPoint... pinPoints) {
		for ( int i = 0; i < pinPoints.length; i++ ) {
			pinPoints[i].setLocation( this );
			this.pinPoints.add( pinPoints[i] );
		}
	}

	public String getDescription() {
		return description;
	}

}
