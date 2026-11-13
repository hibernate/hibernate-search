package org.hibernate.search.engine.search.reference.scorefunction;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface TypedScoreFunctionFieldReference<SR, T> extends ScoreFunctionFieldReference<SR> {

	Class<T> scoreFunctionType();

	default ValueModel valueModel() {
		return ValueModel.MAPPING;
	}
}
