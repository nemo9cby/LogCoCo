package ca.yorku.nemo.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.TryStatement;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

public class LoggingAddEvaluateByLineProblem  extends LoggingAddProblem{

	public LoggingAddEvaluateByLineProblem(HashMap<ASTNode, String> finalStateASTMarkMap,
			MethodNodeForOutput mdNodeInfo) {
		super(finalStateASTMarkMap,mdNodeInfo);
	}
	
	
	@Override
	public void evaluate(Solution solution) {
		boolean[] booleanTag = EncodingUtils.getBinary(solution.getVariable(0));
		
//		System.out.println(solution.getVariable(0));
		
		ArrayList<ASTNode> addedLogInThisSolution = new ArrayList<>();
		
		for (int i = 0; i < booleanTag.length; i ++) {
			if (booleanTag[i]) {
				addedLogInThisSolution.add(possibleLogAddPointList.get(i));
			}
		}
		
		HashMap<ASTNode, ArrayList<String>> nodeAndItsChoiceMap = new HashMap<>();
		ArrayList<HashMap<ASTNode, String>> nodeExeCombinationList = new ArrayList<>();
		
		for (ASTNode node : addedLogInThisSolution) {
			nodeAndItsChoiceMap.put(node, new ArrayList<>(Arrays.asList("Must", "MustNot")));
		}
		combine(0,new HashMap<ASTNode,String>(),nodeAndItsChoiceMap,nodeExeCombinationList);
		
//		if (nodeExeCombinationList.size() == 2)
//			System.out.println("one log add");
		
		HashMap<ASTNode, ArrayList<String>> mayNodeTransformMap = new HashMap<>();
		for(ASTNode node : allMayNodeList) {
			ArrayList<String> nList = new ArrayList<>();
			mayNodeTransformMap.put(node, nList);
		}
		
 		for(HashMap<ASTNode, String> nodeExeCombination : nodeExeCombinationList) {
			
//			for(ASTNode node : nodeExeCombination.keySet()) {
//				System.out.println(mdNodeInfo.cu.getLineNumber(node.getStartPosition()) + ":"+nodeExeCombination.get(node));
//			}
			
			HashMap<ASTNode, String> freshMarkMap = new HashMap<>(finalStateASTMarkMap);
			ArrayList<ArrayList<ASTNode>> fitPathList = verifyApplicable(nodeExeCombination, freshMarkMap);
			boolean ableToPrint = !fitPathList.isEmpty();
			
			if (ableToPrint) {
//				System.out.println("Able to print combination");
//				for (ASTNode node : nodeExeCombination.keySet()) {
//					System.out.println(mdNodeInfo.cu.getLineNumber(node.getStartPosition())+" " + nodeExeCombination.get(node));
//				}
				
				// intersection for the must node
				HashSet<ASTNode> mustNodeSet = new HashSet<>(fitPathList.get(0));
				for (int i = 1; i < fitPathList.size();i ++) {
					mustNodeSet.retainAll(fitPathList.get(i));
				}
				
				HashSet<ASTNode> unionNodeSet = new HashSet<>();
				for(ArrayList<ASTNode> fitPath : fitPathList) {
					unionNodeSet.addAll(fitPath);
				}
				HashSet<ASTNode> mustNotNodeSet = new HashSet<>();
				
				for (ASTNode key : finalStateASTMarkMap.keySet()) {
					if (!unionNodeSet.contains(key)) {
						mustNotNodeSet.add(key);
					}
				}
				
				for (ASTNode node : mayNodeTransformMap.keySet()) {
					if (mustNodeSet.contains(node)) {
						mayNodeTransformMap.get(node).add("Must");
					}
					else if (mustNotNodeSet.contains(node)) {
						mayNodeTransformMap.get(node).add("MustNot");
					} else {
						mayNodeTransformMap.get(node).add("May");
					}
				}
			}
		}
		
		HashMap<ASTNode, String> transformMap = new HashMap<>(finalStateASTMarkMap);
		for (ASTNode node : mayNodeTransformMap.keySet()) {
			if (mayNodeTransformMap.get(node).contains("May")) {
				continue;
			} else {
				if (mayNodeTransformMap.get(node).contains("Must") && !mayNodeTransformMap.get(node).contains("MustNot")) {
//					allMustNot = false;
					transformMap.put(node, "Must");
				} else if (mayNodeTransformMap.get(node).contains("MustNot") && !mayNodeTransformMap.get(node).contains("Must")) {
//					allMust = false ;
					transformMap.put(node, "MustNot");
				} else {
					transformMap.put(node, "Known");
				}
			}
		}
		
		int transformLine = 0;
		TreeMap<Integer, String> transformCoverageMap = calculateCoverageMap(transformMap);
		for (Integer line : finalStateLineCoverageMap.keySet()) {
			if (transformCoverageMap.containsKey(line)) {
				if (!transformCoverageMap.get(line).equals("May")
						&& finalStateLineCoverageMap.get(line).equals("May")) {
					transformLine ++;
				}
			}
		}
		
		int transformBlock = 0;
		for (ASTNode transformNode : transformMap.keySet()) {
			if ((transformNode instanceof Block || transformNode instanceof SwitchCase) && 
				!(transformNode.getParent() instanceof TryStatement)) {
				String transformStatus = transformMap.get(transformNode);
				String beforeLogSugStatus = finalStateASTMarkMap.get(transformNode);
				if (beforeLogSugStatus.equals("May") && !transformStatus.equals("May")) {
					transformBlock ++;
				}
			}
		}
		
		if (addedLogInThisSolution.size() == 0) {
			solution.setObjective(0, 0);
			solution.setObjective(1, 0);
//			solution.setObjective(2, 0);
		}
		else {
			solution.setObjective(0, -transformLine);
			solution.setObjective(1, addedLogInThisSolution.size());
//			solution.setObjective(2, addedLogInThisSolution.size());
		}
//		solution.setObjective(1, addedLogInThisSolution.size());
		
	}
	

}
