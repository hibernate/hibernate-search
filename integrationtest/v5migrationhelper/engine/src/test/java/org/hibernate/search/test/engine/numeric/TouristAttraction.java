/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.engine.numeric;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.annotations.Store;

/**
 * @author Gunnar Morling
 */
@Indexed
class TouristAttraction {

	@DocumentId
	int id;

	@Fields({
			@Field(name = "scoreNumeric", store = Store.YES),
			@Field(name = "scoreString", store = Store.YES)
	})
	@NumericField(forField = "scoreNumeric")
	short score;

	@Fields({
			@Field(name = "ratingNumericPrecision1", store = Store.YES),
			@Field(name = "ratingNumericPrecision2", store = Store.YES),
			@Field
	})
	@NumericFields({
			@NumericField(forField = "ratingNumericPrecision1", precisionStep = 1),
			@NumericField(forField = "ratingNumericPrecision2", precisionStep = 2)
	})
	short rating;

	TouristAttraction() {
		// empty
	}

	TouristAttraction(int id, short score, short rating) {
		this.id = id;
		this.score = score;
		this.rating = rating;
	}
}
