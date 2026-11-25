package org.hibernate.search.processor.model;

import java.math.BigDecimal;

import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;

@Indexed
public class ScorableEntity {

	@DocumentId
	private Integer id;

	@GenericField(scorable = Scorable.YES)
	private Integer enabled;

	@GenericField(scorable = Scorable.NO)
	private Integer disabled;

	@GenericField
	private Integer useDefault;

	@ScaledNumberField(decimalScale = 2, scorable = Scorable.YES)
	private BigDecimal scaledEnabled;

	@ScaledNumberField(decimalScale = 2, scorable = Scorable.NO)
	private BigDecimal scaledDisabled;

	@ScaledNumberField(decimalScale = 2)
	private BigDecimal scaledDefault;
}
