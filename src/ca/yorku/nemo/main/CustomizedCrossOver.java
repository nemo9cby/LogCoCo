package ca.yorku.nemo.main;

import org.moeaframework.core.FrameworkException;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.BinaryVariable;

public class CustomizedCrossOver implements Variation{
	
	private final double probability;
	
	public CustomizedCrossOver(double probability) {
		super();
		this.probability = probability;
	}
	
	@Override
	public int getArity() {
		return 2;
	}

	@Override
	public Solution[] evolve(Solution[] parents) {
		Solution result1 = parents[0].copy();
		Solution result2 = parents[1].copy();

		for (int i = 0; i < result1.getNumberOfVariables(); i++) {
			Variable variable1 = result1.getVariable(i);
			Variable variable2 = result2.getVariable(i);

			if ((PRNG.nextDouble() <= probability)
					&& (variable1 instanceof BinaryVariable)
					&& (variable2 instanceof BinaryVariable)) {
				evolve((BinaryVariable)variable1, (BinaryVariable)variable2);
			}
		}
		return new Solution[] { result1, result2};
	}
	
	public static void evolve(BinaryVariable v1, BinaryVariable v2) {
		if (v1.getNumberOfBits() != v2.getNumberOfBits()) {
			throw new FrameworkException("binary variables not same length");
		}

		for (int i = 0; i < v1.getNumberOfBits(); i++) {
			boolean value = v1.get(i);

			if ((value != v2.get(i)) && PRNG.nextBoolean()) {
				v1.set(i, !value);
				v2.set(i, value);
			}
		}
	}

}
