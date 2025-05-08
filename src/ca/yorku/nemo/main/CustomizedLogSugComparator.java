package ca.yorku.nemo.main;

import java.util.Comparator;

import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.CrowdingComparator;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;

public class CustomizedLogSugComparator implements DominanceComparator,Comparator<Solution>{

	
	
	
	@Override
	public int compare(Solution solution1, Solution solution2) {
//		ChainedComparator chainedComparator = new ChainedComparator(new ParetoDominanceComparator(), new CrowdingComparator());
		
		
//		int chainedCompareResult = chainedComparator.compare(solution1, solution2);
//		if (solution1.getNumberOfObjectives() < 2) {
//			return chainedCompareResult;
//		} else {
			if (solution1.getObjective(0) < solution2.getObjective(0)) {
				return -1;
			} else if (solution1.getObjective(0) > solution2.getObjective(0)) {
				return 1;
			} else {
				if (solution1.getObjective(1) < solution2.getObjective(1)) {
					return -1;
				} else if (solution1.getObjective(1) > solution2.getObjective(1)) {
					return 1;
				} else {
					return 0;
				}
			}
//		}
//			if (solution1.getObjective(2) == 0) {
//				return 1;
//			} else if (solution2.getObjective(2) == 0 ) {
//				return -1;
//			}
//			
//			if (chainedCompareResult == 0) {
//				boolean dominate1 = false;
//				boolean dominate2 = false;
//				if (solution1.getObjective(0) < solution2.getObjective(0)) {
//					dominate1 = true;
//				} else if (solution1.getObjective(0) > solution2.getObjective(0)) {
//					dominate2 = true;
//				}
//				if (dominate1 == dominate2) {
//					return 0;
//				} else if (dominate1) {
//					return -1;
//				} else {
//					return 1;
//				}
//			}
//		}
//		return 0;
	}
	
}
