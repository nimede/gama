/*********************************************************************************************
 *
 * 'FloatEditor.java, in plugin gama.ui.base.shared, is part of the source code of the GAMA modeling and simulation
 * platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.base.parameters;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import gama.common.interfaces.IAgent;
import gama.common.interfaces.experiment.IParameter;
import gama.kernel.experiment.InputParameter;
import gama.runtime.exceptions.GamaRuntimeException;
import gama.runtime.scope.IScope;
import gama.ui.base.interfaces.EditorListener;
import gaml.operators.Cast;
import gaml.types.IType;
import gaml.types.Types;

public class FloatEditor extends NumberEditor<Double> {

	FloatEditor(final IScope scope, final IAgent agent, final IParameter param, final boolean canBeNull,
			final EditorListener<Double> l) {
		super(scope, agent, param, l, canBeNull);
	}

	FloatEditor(final IScope scope, final Composite parent, final String title, final Double value, final Double min,
			final Double max, final Double step, final boolean canBeNull, final EditorListener<Double> whenModified) {
		// Convenience method
		super(scope, new InputParameter(title, value, min, max, step), whenModified, canBeNull);
		if (step != null) {
			stepValue = step;
		}
		this.createComposite(parent);
	}

	@Override
	protected void computeStepValue() {
		stepValue = param.getStepValue(getScope());
		if (stepValue == null) {
			stepValue = 0.1;
		}
	}

	@Override
	protected void modifyValue(final Object val) throws GamaRuntimeException {
		Double i = Cast.asFloat(getScope(), val);
		if (acceptNull && val == null) {
			i = null;
		} else {
			if (minValue != null && i < minValue.doubleValue())
				throw GamaRuntimeException.error("Value " + i + " should be greater than " + minValue, getScope());
			if (maxValue != null && i > maxValue.doubleValue())
				throw GamaRuntimeException.error("Value " + i + " should be smaller than " + maxValue, getScope());
		}
		super.modifyValue(i);
	}

	// @Override
	// protected void setOriginalValue(final Double val) {
	// // if ( acceptNull && val == null ) {
	// // super.setOriginalValue(val);
	// // }
	// super.setOriginalValue(val);
	// }

	@Override
	protected Double normalizeValues() throws GamaRuntimeException {
		final Double valueToConsider = getOriginalValue() == null ? 0.0 : Cast.asFloat(getScope(), getOriginalValue());
		currentValue = getOriginalValue() == null ? null : valueToConsider;
		minValue = minValue == null ? null : minValue.doubleValue();
		maxValue = maxValue == null ? null : maxValue.doubleValue();
		return valueToConsider;
	}

	@Override
	public IType<Double> getExpectedType() {
		return Types.FLOAT;
	}

	@Override
	protected Double applyPlus() {
		if (currentValue == null)
			return 0.0;
		final Double i = currentValue;
		final Double newVal = i + stepValue.doubleValue();
		return newVal;
	}

	@Override
	protected Double applyMinus() {
		if (currentValue == null)
			return 0.0;
		final Double i = currentValue;
		final Double newVal = i - stepValue.doubleValue();
		return newVal;
	}

	@Override
	protected void checkButtons() {
		super.checkButtons();
		final Button plus = items[PLUS];
		if (plus != null && !plus.isDisposed()) {
			plus.setEnabled(param.isDefined() && (maxValue == null || applyPlus() < maxValue.doubleValue()));
		}
		final Button minus = items[MINUS];
		if (minus != null && !minus.isDisposed()) {
			minus.setEnabled(param.isDefined() && (minValue == null || applyMinus() > minValue.doubleValue()));
		}
	}

}
