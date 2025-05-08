package ca.yorku.nemo.main;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.variable.BinaryVariable;;

public class CustomizedInitilization extends RandomInitialization{
	
//	private List<Solution> injectedSolutions;
	
	private int possibleLogAddPoints = 0;

	public CustomizedInitilization(Problem problem, int populationSize,
			 int possibleLogAddPoints) {
		super(problem, populationSize);
		this.possibleLogAddPoints = possibleLogAddPoints;
	}
	
	@Override
	public Solution[] initialize() {
		
		Solution[] initialPopulation = new Solution[populationSize];
		
		if (Math.pow(2, possibleLogAddPoints) == populationSize) {
			for (int i = 0; i < populationSize; i ++) {
				Solution solution = composeSolutionByInteger(i);
				initialPopulation[i] = solution;
			}
		} else {
			Solution solutionAllZero = composeSolutionByInteger(0);
			initialPopulation[0] = solutionAllZero;
			Solution solutionAllOne = composeSolutionByInteger((int)Math.pow(2, possibleLogAddPoints) - 1);
			initialPopulation[1] = solutionAllOne;
			
			int[] possibleCandidates = range(1, (int)Math.pow(2, possibleLogAddPoints) - 1);
			PRNG.shuffle(possibleCandidates);
			for (int i = 2; i < populationSize; i ++) {
				Solution solution = composeSolutionByInteger(i);
				initialPopulation[i] = solution;
			}
		}
		
		return initialPopulation;
		
//		for (int i = 0; i < populationSize; i ++) {
//			initialPopulation[i] = injectedSolutions.get(i);
//		}
//		
//		
//		for (int i = injectedSolutions.size(); i < populationSize; i++) {
//			Solution solution = problem.newSolution();
//
//			for (int j = 0; j < solution.getNumberOfVariables(); j++) {
//				solution.getVariable(j).randomize();
//			}
//			
//			initialPopulation[i] = solution;
//		}
//
//		return initialPopulation;
		
		
	}
	
	private int[] range(int start, int end) {
	    int[] a = new int[end-start];
	    int startNum = start;
	    for (int i = 0; i < a.length; i++) {
	        a[i] = startNum;
	        startNum++;
	    }
	    return a;
	}
	
	private Solution composeSolutionByInteger(int integer) {
		Solution solution = problem.newSolution();
		BinaryVariable bv = (BinaryVariable) solution.getVariable(0);
		Boolean[] boolSet = convertIntegerToBoolArray(integer);
		for(int i = 0; i < boolSet.length; i ++) {
			bv.set(i, boolSet[i]);
		}
		return solution;
	}
	
	private Boolean[] convertIntegerToBoolArray(int integer) {
		Boolean[] results = new Boolean[possibleLogAddPoints];
		String binaryIntInStr = Integer.toBinaryString(integer);
		
		String t = StringUtils.leftPad(binaryIntInStr, possibleLogAddPoints, "0");
		for(int i = 0; i < t.length(); i ++) {
			if (t.charAt(i) == '1') {
				results[i] = true;
			} else {
				results[i] = false;
			}
		}
		return results;
	}
}
