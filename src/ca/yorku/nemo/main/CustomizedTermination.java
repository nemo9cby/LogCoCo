package ca.yorku.nemo.main;

import java.util.ArrayList;
import java.util.Arrays;

import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.TerminationCondition;

public class CustomizedTermination implements TerminationCondition {

	ArrayList<Solution> latestSolutions = null;
	ArrayList<Solution> currentSolutions = null;
	CustomizedLogSugComparator comparator = null;
	
	public CustomizedTermination(CustomizedLogSugComparator comparator) {
		this.comparator = comparator;
	}
	
	@Override
	public void initialize(Algorithm algorithm) {
		// do nothing
		
	}

	@Override
	public boolean shouldTerminate(Algorithm algorithm) {
		NSGAII nsgiiAlgo = (NSGAII) algorithm;
		NondominatedSortingPopulation currentPopulation = nsgiiAlgo.getPopulation();
		currentSolutions = new ArrayList<>();
		for (Solution s : currentPopulation) {
			currentSolutions.add(s);
		}
		if (currentPopulation.size() == 0) {
			return false;
		}
		if (latestSolutions == null) {
			latestSolutions = new ArrayList<>();
			
			for (Solution s : currentPopulation) {
				latestSolutions.add(s);
			}
			return false;
		} else {
			int sizeA = latestSolutions.size();
			int sizeB = currentSolutions.size();
			double MDR = (dom(currentSolutions,latestSolutions,algorithm) /(double)sizeB) 
					- (dom(latestSolutions,currentSolutions,algorithm) / (double)sizeA);
			
			if (Math.abs(MDR) <= 0.1) {
				System.out.println("CurrentMDR: " + MDR);
				return true;
			}
			if (algorithm.getNumberOfEvaluations() > 10000) {
				return true;
			}
			
			latestSolutions = new ArrayList<>(currentSolutions);
		}
		
		return false;
	}
	
	private int dom(ArrayList<Solution> popA, ArrayList<Solution> popB, Algorithm algorithm) {
		int number = 0;
		for (Solution solutionA : popA) {
			boolean dominated = false;
			for (Solution solutionB : popB) {
				if (comparator.compare(solutionA, solutionB) == 1) {
					dominated = true;
				}
			}
			if (dominated)
				number ++;
		}
		return number;
	}

}
