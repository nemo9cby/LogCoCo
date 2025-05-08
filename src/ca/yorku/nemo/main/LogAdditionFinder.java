package ca.yorku.nemo.main;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.jgrapht.graph.DefaultEdge;

public class LogAdditionFinder {
	
	CompilationUnit cu;
	String filePath;
	MethodDeclaration methodToMakeSuggestion;
	HashMap<ASTNode, String> originalMarkMap = new HashMap<>();
	
	public LogAdditionFinder(HashMap<ASTNode, String> originalMarkMap, MethodNodeForOutput methodNodeForOutput) {
		this.originalMarkMap.putAll(originalMarkMap);
		cu = methodNodeForOutput.cu;
		filePath = methodNodeForOutput.filePath;
		methodToMakeSuggestion = methodNodeForOutput.md;
	}
	
	public void provideSuggestion() {
		
		ArrayList<ASTNode> mayNodeToLogList = new ArrayList<>();
		ArrayList<ASTNode> uniqueLogAddPointList = new ArrayList<>();
		
		for (ASTNode node : originalMarkMap.keySet()) {
			if (originalMarkMap.get(node).equals("May")) {
				mayNodeToLogList.add(node);
			}
		}
		for(ASTNode node: mayNodeToLogList) {
//			boolean dup = false;
			if (node instanceof Block) {
				if (node.getParent() instanceof TryStatement)
					continue;
				uniqueLogAddPointList.add(node);
			}
//				mayNodeToLogList
//			for (ASTNode logAddNodeCandidate: uniqueLogAddPointList) {
//				
//					
//				if (node.getParent().equals(logAddNodeCandidate.getParent()) && (node.getParent() instanceof Block)) {
//					dup = true;
//				}
//			}
//			if (!dup) {
//				uniqueLogAddPointList.add(node);
//			}
		}
		
		int countOfAddingPoints =(int)Math.pow(2, uniqueLogAddPointList.size());
		ArrayList<ArrayList<ASTNode>> addNodeScenarioList = new ArrayList<>();
		for (int c = 0; c < countOfAddingPoints; c ++) {
			ArrayList<Boolean> logAddOption = new ArrayList<>(); 
			for (int i = 0; i < uniqueLogAddPointList.size(); i ++) {
				int b = ((c >> i) & 1);
				if (b == 0) {
					logAddOption.add(false);
				} else {
					logAddOption.add(true);
				}
			}
			ArrayList<ASTNode> addedNode = new ArrayList<>();
			for (int i = 0; i < logAddOption.size(); i ++) {
				if (logAddOption.get(i)) {
					addedNode.add(uniqueLogAddPointList.get(i));
				}
			}
			addNodeScenarioList.add(addedNode);
		}
		
		for (ArrayList<ASTNode> addNodeList : addNodeScenarioList) {
			
			for(ASTNode node : addNodeList) {
				HashMap<ASTNode, String> testMap = new HashMap<>();
				testMap.putAll(originalMarkMap);
				
				MainParser.markMustNodeCoverage(testMap, node, methodToMakeSuggestion);
				
				countLineCoverage(testMap);
				countLineCoverage(originalMarkMap);
				
				// compare the may node turned into
			}
//			HashMap<ASTNode, ArrayList<String>> nodeExeMap = new HashMap<>();
//			for (ASTNode node : addNodeList) {
//				ArrayList<String> exePossibility = new ArrayList<>(Arrays.asList("Must", "MustNot"));
//				nodeExeMap.put(node, exePossibility);
//			}
//			ArrayList<HashMap<ASTNode, String>> listOfPossibleExeScenarios = new ArrayList<>();
//			combine(0, new HashMap<ASTNode,String>(), nodeExeMap, listOfPossibleExeScenarios);
			
			
			
		}
		
		
		
	}
	
	
	TreeMap<Integer, String> countLineCoverage(HashMap<ASTNode, String> markMap) {
		TreeMap<Integer, String> lineCoverageMap = new TreeMap<>();
		for (ASTNode astNode : markMap.keySet()) {
			String coverStatus = markMap.get(astNode);
			if (astNode instanceof IfStatement || astNode instanceof SwitchStatement || astNode instanceof ForStatement
					|| astNode instanceof EnhancedForStatement || astNode instanceof WhileStatement || astNode instanceof TryStatement) {
				lineCoverageMap.put(cu.getLineNumber(astNode.getStartPosition()), coverStatus);
			} else if (astNode instanceof Block) {
				if (coverStatus.equals("May")) {
					int startLine = cu.getLineNumber(astNode.getStartPosition());
					int endLine = cu.getLineNumber(astNode.getStartPosition() + astNode.getLength() - 1);
					for (int lineNumber = startLine; lineNumber <= endLine; lineNumber ++) {
						if (!lineCoverageMap.containsKey(lineNumber)) {
							lineCoverageMap.put(lineNumber, coverStatus);
						} else  {
							if (!lineCoverageMap.get(lineNumber).equals("Must")) {
								lineCoverageMap.put(lineNumber, coverStatus);
							}
						}
					}
				}
			} else {
				int startLine = cu.getLineNumber(astNode.getStartPosition());
				int endLine = cu.getLineNumber(astNode.getStartPosition() + astNode.getLength() - 1);
				for (int lineNumber = startLine; lineNumber <= endLine; lineNumber ++) {
					lineCoverageMap.put(lineNumber, coverStatus);
				}
			}
		}
		return lineCoverageMap;
	}

	
	void combine(int index, HashMap<ASTNode, String> current, HashMap<ASTNode,ArrayList<String>> map, List<HashMap<ASTNode, String>> list) {
		
		if (index == map.size()) {
			HashMap<ASTNode, String> newMap = new HashMap<>();
//			System.out.println(current);
			for(ASTNode key : current.keySet()) {
				newMap.put(key, current.get(key));
			}
//			System.out.println("mapfinished");
			list.add(newMap);
		} else {
			Object currentKey = map.keySet().toArray()[index];
			for (String value : map.get(currentKey)) {
				current.put((ASTNode) currentKey, value);
				combine(index+1, current, map, list);
				current.remove(currentKey);
			}
		}
		
	}
}
