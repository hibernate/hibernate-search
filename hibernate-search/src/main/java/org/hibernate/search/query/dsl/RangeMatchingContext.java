package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface RangeMatchingContext extends FieldCustomization<RangeMatchingContext> {
	/**
	 * field / property the term query is executed on
	 */
	RangeMatchingContext andField(String field);

	//TODO what about numeric range query, I guess we can detect it automatically based on the field bridge
	//TODO get info on precisionStepDesc (index time info)
	//FIXME: Is <T> correct or should we specialize to String and Numeric (or all the numeric types?
	<T> FromRangeContext<T> from(T from);

	public interface FromRangeContext<T> {
		RangeTerminationExcludable to(T to);
		FromRangeContext<T> excludeLimit();
	}

	/**
	 * The field value must be below <code>below</code>
	 * You can exclude the value <code>below</code> by calling <code>.excludeLimit()</code>
	 */
	RangeTerminationExcludable below(Object below);

	/**
	 * The field value must be above <code>above</code>
	 * You can exclude the value <code>above</code> by calling <code>.excludeLimit()</code>
	 */
	RangeTerminationExcludable above(Object above);

}
