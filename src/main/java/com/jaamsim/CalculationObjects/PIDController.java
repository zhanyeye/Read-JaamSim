/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.CalculationObjects;

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * The PIDController simulates a Proportional-Integral-Differential type Controller.
 * Error = SetPoint - ProcessVariable
 * Output =  (ProportionalGain / ProcessVariableScale) * [ Error + (Integral/IntegralTime) + (DerivativeTime*Derivative) ]
 * @author Harry King
 *
 */
public class PIDController extends DoubleCalculation {

	@Keyword(description = "The set point for the PID controller. The unit type for the set point "
	                     + "is given by the UnitType keyword.\n"
	                     + "The input can be a number or an entity that returns a number, such as "
	                     + "a CalculationObject, an Expression, a ProbabilityDistribution, or a "
	                     + "TimeSeries.",
	         exampleList = {"1.2 m", "TimeSeries1", "'1[m] + 2*[TimeSeries1].Value'"})
	private final SampleInput setPoint;

	@Keyword(description = "The process variable feedback to the PID controller. The unit type "
	                     + "for the process variable is given by the UnitType keyword.\n"
	                     + "The input can be a number or an entity that returns a number, such as "
	                     + "a CalculationObject, an Expression, a ProbabilityDistribution, or a "
	                     + "TimeSeries.",
	         exampleList = {"Process", "'1[m] + [Process].Value'"})
	private final SampleInput processVariable;

	@Keyword(description = "A constant with the same unit type as the process variable and the "
	                     + "set point. The difference between the process variable and the set "
	                     + "point is divided by this quantity to make a dimensionless variable.",
	         exampleList = {"1.0 kg"})
	private final ValueInput processVariableScale;

	@Keyword(description = "The unit type for the output from the PID controller.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput outputUnitType;

	@Keyword(description = "The coefficient applied to the proportional feedback loop. "
	                     + "Its unit type must be the same as the controller's output.",
	         exampleList = {"1.3 m"})
	private final ValueInput proportionalGain;

	@Keyword(description = "The time scale applied to the integral feedback loop.",
	         exampleList = {"1.0 s"})
	private final ValueInput integralTime;

	@Keyword(description = "The time scale applied to the differential feedback loop.",
	         exampleList = {"1.0 s"})
	private final ValueInput derivativeTime;

	@Keyword(description = "The lower limit for the output signal.",
	         exampleList = {"0.0 m"})
	private final ValueInput outputLow;

	@Keyword(description = "The upper limit for the output signal.",
	         exampleList = {"1.0 m"})
	private final ValueInput outputHigh;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastError;  // The previous value for the error signal
	private double integral;  // The integral of the error signal
	private double derivative;  // The derivative of the error signal

	{
		inputValue.setHidden(true);

		setPoint = new SampleInput("SetPoint", "Key Inputs", null);
		setPoint.setUnitType(UserSpecifiedUnit.class);
		setPoint.setEntity(this);
		setPoint.setRequired(true);
		this.addInput(setPoint);

		processVariable = new SampleInput("ProcessVariable", "Key Inputs", null);
		processVariable.setUnitType(UserSpecifiedUnit.class);
		processVariable.setEntity(this);
		processVariable.setRequired(true);
		this.addInput(processVariable);

		processVariableScale = new ValueInput("ProcessVariableScale", "Key Inputs", 1.0d);
		processVariableScale.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		processVariableScale.setUnitType(UserSpecifiedUnit.class);
		this.addInput(processVariableScale);

		outputUnitType = new UnitTypeInput("OutputUnitType", "Key Inputs", UserSpecifiedUnit.class);
		outputUnitType.setRequired(true);
		this.addInput(outputUnitType);

		proportionalGain = new ValueInput("ProportionalGain", "Key Inputs", 1.0d);
		proportionalGain.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		proportionalGain.setUnitType(UserSpecifiedUnit.class);
		this.addInput(proportionalGain);

		integralTime = new ValueInput("IntegralTime", "Key Inputs", 1.0d);
		integralTime.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		integralTime.setUnitType(TimeUnit.class );
		this.addInput(integralTime);

		derivativeTime = new ValueInput("DerivativeTime", "Key Inputs", 1.0d);
		derivativeTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		derivativeTime.setUnitType(TimeUnit.class );
		this.addInput(derivativeTime);

		outputLow = new ValueInput("OutputLow", "Key Inputs", Double.NEGATIVE_INFINITY);
		outputLow.setUnitType(UserSpecifiedUnit.class);
		this.addInput(outputLow);

		outputHigh = new ValueInput("OutputHigh", "Key Inputs", Double.POSITIVE_INFINITY);
		outputHigh.setUnitType(UserSpecifiedUnit.class);
		this.addInput(outputHigh);
	}

	public PIDController() {}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == outputUnitType) {
			outUnitType = outputUnitType.getUnitType();
			outputLow.setUnitType(outUnitType);
			outputHigh.setUnitType(outUnitType);
			proportionalGain.setUnitType(outUnitType);
			return;
		}
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		setPoint.setUnitType(ut);
		processVariable.setUnitType(ut);
		processVariableScale.setUnitType(ut);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastError = 0.0;
		integral = 0.0;
		derivative = 0.0;
		lastUpdateTime = 0.0;
	}

	public double getError(double simTime) {
		return setPoint.getValue().getNextSample(simTime) - processVariable.getValue().getNextSample(simTime);
	}

	@Override
	protected double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal) {

		// Calculate the elapsed time
		double dt = simTime - lastTime;

		// Calculate the error signal
		double error = this.getError(simTime);

		// Calculate integral and differential terms
		double intgrl = integral + error*dt;
		double deriv = 0.0;
		if (dt > 0.0)
			deriv = (error - lastError)/dt;

		// Calculate the output value
		double val = error;
		val += intgrl / integralTime.getValue();
		val += derivativeTime.getValue() * deriv;
		val *= proportionalGain.getValue() / processVariableScale.getValue();

		// Condition the output value
		val = Math.max(val, outputLow.getValue());
		val = Math.min(val, outputHigh.getValue());

		return val;
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);
		double dt = simTime - lastUpdateTime;
		double error = this.getError(simTime);
		integral += error * dt;
		lastError = error;
		lastUpdateTime = simTime;
		return;
	}

	@Output(name = "ProportionalValue",
	 description = "The proportional component of the output value.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 0)
	public double getProportionalValue(double simTime) {
		return this.getError(simTime) * proportionalGain.getValue() / processVariableScale.getValue();
	}

	@Output(name = "IntegralValue",
	 description = "The integral component of the output value.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 1)
	public double getIntegralValue(double simTime) {
		return integral / integralTime.getValue()
				* proportionalGain.getValue() / processVariableScale.getValue();
	}

	@Output(name = "DerivativeValue",
	 description = "The derivative component of the output value.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 2)
	public double getDifferentialValue(double simTime) {
		return derivativeTime.getValue() * derivative
				* proportionalGain.getValue()/ processVariableScale.getValue();
	}

}
