package ca.yorku.nemo.main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Population;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Selection;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.CrowdingComparator;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.TournamentSelection;

public class CustomizedNSGAII extends NSGAII{
	
	private final Selection selection;
	
	private final Variation variation;
	
	public CustomizedNSGAII(Problem problem, NondominatedSortingPopulation population,
			EpsilonBoxDominanceArchive archive, Selection selection, Variation variation,
			Initialization initialization) {
		super(problem, population, archive, selection, variation, initialization);
		this.selection = selection;
		this.variation = variation;
	}
	
	@Override
	public void iterate() {
		NondominatedSortingPopulation population = getPopulation();
		EpsilonBoxDominanceArchive archive = getArchive();
		Population offspring = new Population();
		int populationSize = population.size();

		
		// recreate the original NSGA-II implementation using binary
		// tournament selection without replacement; this version works by
		// maintaining a pool of candidate parents.
		LinkedList<Solution> pool = new LinkedList<Solution>();
		
//		DominanceComparator comparator = new ChainedComparator(
//				new ParetoDominanceComparator(),
//				new CrowdingComparator());
		
		while (offspring.size() < 0.5 * populationSize) {
			// ensure the pool has enough solutions
			while (pool.size() < 2*variation.getArity()) {
				List<Solution> poolAdditions = new ArrayList<Solution>();
				
				for (Solution solution : population) {
					poolAdditions.add(solution);
				}
				
				PRNG.shuffle(poolAdditions);
				pool.addAll(poolAdditions);
			}
			
			// select the parents using a binary tournament
			Solution[] parents = new Solution[variation.getArity()];
			
			for (int i = 0; i < parents.length; i++) {
				parents[i] = (TournamentSelection.binaryTournament(
						pool.removeFirst(),
						pool.removeFirst(),
						((TournamentSelection)selection).getComparator()));
			}
			
			// evolve the children
			offspring.add(variation.evolve(parents)[0]);
		}
		

		evaluateAll(offspring);

		if (archive != null) {
			archive.addAll(offspring);
		}

		population.addAll(offspring);
		population.truncate(populationSize, (CustomizedLogSugComparator)((TournamentSelection)selection).getComparator());
	}
	
	
	@Override
	public NondominatedPopulation getResult() {
		Population population = getPopulation();
		NondominatedPopulation archive = getArchive();
		NondominatedPopulation result = new NondominatedPopulation(((TournamentSelection)selection).getComparator());

		result.addAll(population);

		if (archive != null) {
			result.addAll(archive);
		}

		return result;
	}
	
	
}
