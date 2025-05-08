package ca.yorku.nemo.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.jgrapht.graph.DefaultEdge;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.tree.While;

import com.google.common.util.concurrent.Service.State;

public class LoggingAddProblem extends AbstractProblem{
	
	HashMap<ASTNode, String> finalStateASTMarkMap;
	
//	HashMap<String, HashMap<ASTNode, String>> localMaps;
	
	MethodNodeForOutput mdNodeInfo;
	
	ArrayList<ASTNode> possibleLogAddPointList = new ArrayList<>();
	
	ArrayList<ASTNode> allMayNodeList = new ArrayList<>();
	
	TreeMap<Integer, String> finalStateLineCoverageMap;

	HashMap<ASTNode, ArrayList<ArrayList<ASTNode>>> branchChoicesMap = new HashMap<>();
	
	HashSet<ArrayList<ASTNode>> traversePathSet = new HashSet<>();

	int possibleAddingPointCount;
	
	int branchSize = 0;
	
	public LoggingAddProblem(HashMap<ASTNode, String> finalStateASTMarkMap,
//			HashMap<String, HashMap<ASTNode, String>> localMaps,
			MethodNodeForOutput mdNodeInfo) {
		super(1, 2);
		this.finalStateASTMarkMap = finalStateASTMarkMap;
//		this.localMaps = localMaps;
		this.mdNodeInfo = mdNodeInfo;
		analyzeMap();
	}
	
	public int getTotalLineNumber() {
		return finalStateLineCoverageMap.size();
	}
	
	public int getTotalMayLineNumber() {
		int result = 0;
		for (String status : finalStateLineCoverageMap.values()) {
			if (status.equals("May")) {
				result ++;
			}
		}
		return result;
	}
	
	public int getTotalBranchNumber() { 
		int totalBranch = 0;
		for (ASTNode node : finalStateASTMarkMap.keySet()) {
			if ((node instanceof Block || node instanceof SwitchCase)
					&& !(node.getParent() instanceof TryStatement)){
				totalBranch ++;
			}
		}
		return totalBranch;
	}
	
	public int getMayBranchNumber() { 
		int totalMayBranch = 0;
		for (ASTNode node : finalStateASTMarkMap.keySet()) {
			if ((node instanceof Block || node instanceof SwitchCase)
					&& !(node.getParent() instanceof TryStatement)){
				if (node instanceof Block) {
					Block blk = (Block) node;
					if (isBlockContainThrowStmt(blk)) {
						continue;
					}
				}
				if (finalStateASTMarkMap.get(node).equals("May")) {
					totalMayBranch ++;
				}
			}
		}
		return totalMayBranch;
	}
	
	private boolean isBlockContainThrowStmt(Block blk) {
		List stmtList = blk.statements();
		for (int i =0; i < stmtList.size(); i ++) {
			if (stmtList.get(i) instanceof ThrowStatement)
				return true;
		}
		return false;
	}
	
	private void analyzeMap() {
		finalStateLineCoverageMap = calculateCoverageMap(finalStateASTMarkMap);
//		System.out.println("Final state of the line coverage");
//		for (Integer lineNumber : finalStateLineCoverageMap.keySet()) {
//			System.out.println(lineNumber + "," + finalStateLineCoverageMap.get(lineNumber));
//		}
		
		for (ASTNode node : finalStateASTMarkMap.keySet()) {
			if (finalStateASTMarkMap.get(node).equals("May")) {
				allMayNodeList.add(node);
			}
		}
		
		HashMap<Block, ASTNode> mustBlockMayNodeMap = new HashMap<>();
		
		for (ASTNode node : finalStateASTMarkMap.keySet()) {
			if (finalStateASTMarkMap.get(node).equals("May")) {
				if (node instanceof Block) {
					if (node.getParent() instanceof TryStatement) {
						continue;
					} else {
						List stmtList = ((Block) node).statements();
						possibleLogAddPointList.add((ASTNode) stmtList.get(stmtList.size()-1));
					}
				} else {
					if (node.getParent() instanceof Block) {
						boolean parentBlockCovered = false;
						if (finalStateASTMarkMap.get(node.getParent()).equals("Must")) {
							parentBlockCovered = true;
						}
						if (!parentBlockCovered) {
							continue;
						} else {
							if (mustBlockMayNodeMap.containsKey((Block)node.getParent())) {
								if (mustBlockMayNodeMap.get(node.getParent()).getStartPosition() < node.getStartPosition()) {
									mustBlockMayNodeMap.put((Block)node.getParent(), node);
								}
							} else {
								mustBlockMayNodeMap.put((Block)node.getParent(), node);
							}
						}
					} else {
						if (node.getParent() instanceof IfStatement) {
							continue;
						} else if (node.getParent() instanceof SwitchStatement) {
							if (node instanceof SwitchCase) {
								possibleLogAddPointList.add(node);
							}
						}
					}
				}
			}
		}
		for (Block blk : mustBlockMayNodeMap.keySet()) {
			possibleLogAddPointList.add(mustBlockMayNodeMap.get(blk));
		}
		
		ArrayList<ASTNode> toBeRemoved = new ArrayList<>();
		
		for (ASTNode node : possibleLogAddPointList) {
			if (node instanceof ThrowStatement) {
				toBeRemoved.add(node);
			}
		}
		
		possibleLogAddPointList.removeAll(toBeRemoved);
		possibleAddingPointCount = possibleLogAddPointList.size();
		
		Collections.sort(possibleLogAddPointList, new Comparator<ASTNode>() {
		    @Override
		    public int compare(ASTNode o1, ASTNode o2) {
		        return o1.getStartPosition() - o2.getStartPosition();
		    }
		});

//		
//		for (ASTNode addLogPoint : possibleLogAddPointList) {
//			System.out.printf("Log add point: %s:%d\n",addLogPoint.getClass().getSimpleName(),
//					mdNodeInfo.cu.getLineNumber(addLogPoint.getStartPosition()));
//		}
		ArrayList<HashMap<ASTNode, ArrayList<ASTNode>>> branchChoiceList = new ArrayList<>();
		extractBranchVisitPossibility();
		
		branchSize = branchChoicesMap.keySet().size();
		if (branchSize < 20) {
			combineASTNodePossibility(0, new HashMap<ASTNode, ArrayList<ASTNode>>(), branchChoicesMap, branchChoiceList);
		}
		
		for(HashMap<ASTNode, ArrayList<ASTNode>> branchChoice : branchChoiceList) {
			ArrayList<ASTNode> traversePath = new ArrayList<>();
			traverseASTNode(branchChoice, traversePath, mdNodeInfo.md);
			traversePathSet.add(traversePath);
		}
		
//		for (int i = 0; i < this.possibleAddingPointCount; i ++) {
//			System.out.println(this.mdNodeInfo.cu.getLineNumber(this.possibleLogAddPointList.get(i).getStartPosition())
//					+ ":" + this.possibleLogAddPointList.get(i).getClass().getName());
//		}
	}
	
	private String traverseASTNode(HashMap<ASTNode, ArrayList<ASTNode>> branchChoice, ArrayList<ASTNode> traversePath, ASTNode node) {
		String result = "";
		if (node == null)
			return "";
		if (node instanceof MethodDeclaration) {
			result += traverseASTNode(branchChoice, traversePath, ((MethodDeclaration) node).getBody());
		} else if (node instanceof Block) {
			traversePath.add(node);
			Block blk = (Block) node;
			for (int i = 0; i < blk.statements().size(); i ++ ) {
				Statement stmt = (Statement) blk.statements().get(i);
				result += traverseASTNode(branchChoice, traversePath, stmt);
				if (result.contains("RETURN"))
					return result;
				else if (result.contains("BREAK"))
					break;
			}
		} else if (node instanceof ReturnStatement) {
			traversePath.add(node);
			return "RETURN";
		} else if (branchChoice.containsKey(node)) {
			traversePath.add(node);
			ArrayList<ASTNode> choice = branchChoice.get(node);
			if (choice != null) {
				for (int j = 0; j < choice.size(); j ++) {
					ASTNode next = choice.get(j);
					result += traverseASTNode(branchChoice, traversePath, next);
					if (result.contains("RETURN"))
						return result;
				}
			}
		} else if(node instanceof TryStatement) {
			traversePath.add(node);
			result += traverseASTNode(branchChoice, traversePath, ((TryStatement) node).getBody());
			result += traverseASTNode(branchChoice, traversePath, ((TryStatement) node).getFinally());
			if (result.contains("RETURN"))
				return result;
		} else if (node instanceof BreakStatement) {
			traversePath.add(node);
			return "BREAK";
		} else {
			traversePath.add(node);
		}
		return result;
	}
	
	private void extractBranchVisitPossibility() {
		for (ASTNode node :finalStateASTMarkMap.keySet()) {
			if (node instanceof IfStatement) {
				ASTNode thenStmt = ((IfStatement) node).getThenStatement();
				ASTNode elseStmt = ((IfStatement) node).getElseStatement();
				ArrayList<ArrayList<ASTNode>> choiceList = new ArrayList<>();
				if (hasLoopParent(node)) {
					ArrayList<ASTNode> nChoice = new ArrayList<>();
					nChoice.add(thenStmt);
					choiceList.add(nChoice);
					if (elseStmt == null) {
						choiceList.add(null);
					} else {
						nChoice = new ArrayList<>();
						nChoice.add(elseStmt);
						choiceList.add(nChoice);
						nChoice = new ArrayList<>();
						nChoice.add(thenStmt);
						nChoice.add(elseStmt);
						choiceList.add(nChoice);
					}
				} else {
					ArrayList<ASTNode> nChoice = new ArrayList<>();
					nChoice.add(thenStmt);
					choiceList.add(nChoice);
					if (elseStmt == null) {
						choiceList.add(null);
					} else {
						nChoice = new ArrayList<>();
						nChoice.add(elseStmt);
						choiceList.add(nChoice);
					}
				}
				branchChoicesMap.put(node,choiceList);
			} else if (node instanceof WhileStatement || node instanceof ForStatement
					|| node instanceof EnhancedForStatement) {
				ArrayList<ArrayList<ASTNode>> choiceList = new ArrayList<>();
				ArrayList<ASTNode> nChoice = new ArrayList<>();
				if (node instanceof WhileStatement) {
					nChoice.add(((WhileStatement) node).getBody());
				}
				if (node instanceof ForStatement) {
					nChoice.add(((ForStatement) node).getBody());
				}
				if (node instanceof EnhancedForStatement) {
					nChoice.add(((EnhancedForStatement) node).getBody());
				}
				choiceList.add(nChoice);
				choiceList.add(null);
				branchChoicesMap.put(node,choiceList);
			} else if (node instanceof SwitchStatement) {
				
				SwitchStatement switchNode  = (SwitchStatement) node;
				List stmtList = ((SwitchStatement) node).statements();
				
				HashMap<ASTNode, ArrayList<ASTNode>> caseStmtAndItsChildren = new HashMap<>();
				ArrayList<ASTNode> caseStmtList = new ArrayList<>();
				ArrayList<ASTNode> otherStmt = new ArrayList<>();
				for (int i = 0; i < stmtList.size(); i ++) {
					if (stmtList.get(i) instanceof SwitchCase) {
						ArrayList<ASTNode> childrenNodes= new ArrayList<>();
						caseStmtAndItsChildren.put((ASTNode)stmtList.get(i), childrenNodes);
						caseStmtList.add((ASTNode)stmtList.get(i));
					}
					else {
						otherStmt.add((ASTNode)stmtList.get(i));
					}
				}
				
				for (int i = 0; i < caseStmtList.size(); i ++) {
					if (i == caseStmtList.size() -1) {
						for (int j = 0; j < otherStmt.size(); j ++) {
							if (otherStmt.get(j).getStartPosition() > caseStmtList.get(i).getStartPosition()) {
								caseStmtAndItsChildren.get(caseStmtList.get(i)).add(otherStmt.get(j));
							}
						}
					} else {
						ASTNode thisCase = caseStmtList.get(i);
						ASTNode nextCase = caseStmtList.get(i+1);
						for (int j = 0; j < otherStmt.size(); j ++) {
							if (otherStmt.get(j).getStartPosition() > thisCase.getStartPosition()
									&& otherStmt.get(j).getStartPosition() < nextCase.getStartPosition()) {
								caseStmtAndItsChildren.get(thisCase).add(otherStmt.get(j));
							}
						}
					}
				}
				
				
				ArrayList<ArrayList<ASTNode>> choiceList = new ArrayList<>();
				ArrayList<ASTNode> allOption = new ArrayList<>();
				
				for (ASTNode caseNode : caseStmtAndItsChildren.keySet()) {
					ArrayList<ASTNode> nChoice = new ArrayList<>();
					nChoice.add(caseNode);
					allOption.add(caseNode);
					for (ASTNode caseChildrenNode : caseStmtAndItsChildren.get(caseNode)) {
						nChoice.add(caseChildrenNode);
						allOption.add(caseChildrenNode);
					}
					choiceList.add(nChoice);
				}
//				for(int i = 0; i< switchNode.statements().size();i++) {
//					
//					if (switchNode.statements().get(i) instanceof SwitchCase) {
//						ArrayList<ASTNode> nChoice = new ArrayList<>();
//						allOption.add((ASTNode) switchNode.statements().get(i));
//						nChoice.add((ASTNode) switchNode.statements().get(i));
//						choiceList.add(nChoice);
//					}
//				}
				if (hasLoopParent(node)) {
					choiceList.add(allOption);
				}
				branchChoicesMap.put(node,choiceList);
			}
		}
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
					
//					System.out.println("Transform Branch: " + mdNodeInfo.cu.getLineNumber(transformNode.getStartPosition()));
					
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
			solution.setObjective(0, -transformBlock/(double)addedLogInThisSolution.size());
			solution.setObjective(1, -transformLine/(double)addedLogInThisSolution.size());
//			solution.setObjective(2, addedLogInThisSolution.size());
		}
//		solution.setObjective(1, addedLogInThisSolution.size());
		
	}
	
	public TreeMap<Integer, String> calculateCoverageMap(HashMap<ASTNode, String> markMap) {
		TreeMap<Integer, String> lineCoverageMap = new TreeMap<>();
		CompilationUnit cu = mdNodeInfo.cu;
		for (ASTNode astNode : markMap.keySet()) {
			String coverStatus = markMap.get(astNode);
			if (astNode instanceof IfStatement || astNode instanceof SwitchStatement || astNode instanceof ForStatement
					|| astNode instanceof EnhancedForStatement || astNode instanceof WhileStatement || astNode instanceof TryStatement) {
				int startPositionOfBlock = astNode.getStartPosition() + astNode.toString().indexOf("{");
				int blockLine = cu.getLineNumber(startPositionOfBlock);
				int statLine = cu.getLineNumber(astNode.getStartPosition());
				for (int i = statLine; i <= blockLine; i ++) {
					lineCoverageMap.put(i, coverStatus);
				}
				
			} else if (astNode instanceof Block) {
				if (astNode.getParent() instanceof TryStatement) {
					TryStatement parentTry = (TryStatement) astNode.getParent();
					if (parentTry.getFinally() != null && parentTry.getFinally().equals(astNode)) {
						int startLine = cu.getLineNumber(astNode.getStartPosition());
						lineCoverageMap.put(startLine, coverStatus);
					}
				}
//				if (coverStatus.equals("May")) {
//					int startLine = cu.getLineNumber(astNode.getStartPosition());
//					int endLine = cu.getLineNumber(astNode.getStartPosition() + astNode.getLength() - 1);
//					
//					if (markMap.containsKey(astNode.getParent())) {
//						int parentStartLine = cu.getLineNumber(astNode.getParent().getStartPosition());
//						if (parentStartLine != startLine) {
//							lineCoverageMap.put(startLine, coverStatus);
//						}
//					}
//					
//					for (int lineNumber = startLine; lineNumber <= endLine; lineNumber ++) {
//						if (!lineCoverageMap.containsKey(lineNumber)) {
//							lineCoverageMap.put(lineNumber, coverStatus);
//						} else  {
//							if (!lineCoverageMap.get(lineNumber).equals("Must") && !lineCoverageMap.get(lineNumber).equals("Known")) {
//								lineCoverageMap.put(lineNumber, coverStatus);
//							}
//						}
//					}
//				}
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

	@Override
	public Solution newSolution() {
		Solution solution = new Solution(1, 2);
		solution.setVariable(0, EncodingUtils.newBinary(possibleAddingPointCount));
		return solution;
	}
	
	public ArrayList<ArrayList<ASTNode>> verifyApplicable(HashMap<ASTNode, String> nodeExeCombination, HashMap<ASTNode, String> freshMarkMap) {
		
		HashSet<ASTNode> mustNodeSet = new HashSet<>();
		HashSet<ASTNode> mustNotNodeSet = new HashSet<>();
		
		for(ASTNode key: freshMarkMap.keySet()) {
			if (freshMarkMap.get(key).equals("Must")) {
				mustNodeSet.add(key);
			} else if (freshMarkMap.get(key).equals("MustNot")) {
				mustNotNodeSet.add(key);
			}
		}
		
		ArrayList<ArrayList<ASTNode>> fitPathList = new ArrayList<>();
		
		for (ArrayList<ASTNode> path : traversePathSet) {
			
			
			HashSet<ASTNode> allNodes = new HashSet<>(path);
			HashSet<ASTNode> intersection = new HashSet<>(mustNotNodeSet);
			intersection.retainAll(allNodes);
			if(intersection.size() > 0)
				continue;
			
			boolean fitPath = true;
			for (ASTNode node : nodeExeCombination.keySet()) {
				if (nodeExeCombination.get(node).equals("Must")) {
					if (!path.contains(node)) {
						fitPath = false;
						break;
					}
				} else {
					if(path.contains(node)) {
						fitPath = false;
						break;
					}
				}
			}
			if (fitPath) {
				fitPathList.add(path);
			}
		}
		
		
		
//		ArrayList<ASTNode> exeReturn = new ArrayList<>();
		return fitPathList;
//		boolean conflict = false;
//		ASTNode[] mayNodeList = nodeExeCombination.keySet().toArray(new ASTNode[0]);
//		for (int i = 0; i < mayNodeList.length; i ++) {
//			for (int j = i; j < mayNodeList.length; j ++) {
//				ASTNode node1 = mayNodeList[i];
//				ASTNode node2 = mayNodeList[j];
//				
//				// case 1 two nodes are under the same if, one is then, the other is else
//				if (node1.getParent().equals(node2.getParent()) && node1.getParent() instanceof IfStatement) {
//					if (!hasLoopParent(node1)) {
//						conflict = nodeExeCombination.get(node1).equals(nodeExeCombination.get(node2));
//					}
//				}
//			}
//		}
	
		
		/*
		for (ASTNode node : nodeExeCombination.keySet()) {
			if (nodeExeCombination.get(node).equals("Must")) {
				if (node instanceof Block) {
					Block blk = (Block) node;
//					freshMarkMap.put(node, "Must");
//					freshMarkMap.put(node.getParent(), "Must");
					int length = blk.statements().size();
					for (int i = 0; i < length; i ++) {
						freshMarkMap.put((ASTNode) blk.statements().get(i),"Must");
					}
					
					if (blk.statements().get(length-1) instanceof ReturnStatement) {
						exeReturn.add((ASTNode) blk.statements().get(length-1));
					}
				}
			}
		}
		
		for (ASTNode node : exeReturn) {
			for (ASTNode key: freshMarkMap.keySet()) {
				if (key.getStartPosition() > node.getStartPosition()) {
					if (freshMarkMap.get(key).equals("Must")) {
						return false;
					}
					else {
						freshMarkMap.put(key, "MustNot");
					}
				}
			}
		}
		
		for (ASTNode node : nodeExeCombination.keySet()) {
			if (nodeExeCombination.get(node).equals("Must")) {
				if (!(node instanceof Block)) {
					continue;
				}
				if (node.getParent() instanceof IfStatement) {
					markMustNode(freshMarkMap, node, mdNodeInfo.md);
					if (!hasLoopParent(node)) {
						IfStatement parentIf = (IfStatement) node.getParent();
						boolean conflict = false;
						if (parentIf.getThenStatement().equals(node)) {
							if (parentIf.getElseStatement() != null) {
								conflict = markMustNotNode(freshMarkMap, parentIf.getElseStatement(), mdNodeInfo.md);
							}
						} else {
							conflict = markMustNotNode(freshMarkMap, parentIf.getThenStatement(), mdNodeInfo.md);
						}
						if (conflict)
							return false;
					}
				}
			} else {
				if (node instanceof Block) {
					if (!hasLoopParent(node)) {
						IfStatement parentIf = (IfStatement) node.getParent();
						boolean conflict = false;
						conflict = markMustNotNode(freshMarkMap, node, mdNodeInfo.md);
						if (conflict)
							return false;
						if (parentIf.getThenStatement().equals(node)) {
							if (parentIf.getElseStatement() != null) {
								conflict = markMustNode(freshMarkMap, parentIf.getElseStatement(), mdNodeInfo.md);
							}
						} else {
							conflict = markMustNode(freshMarkMap, parentIf.getThenStatement(), mdNodeInfo.md);
						}
						if (conflict) {
							return false;
						}
					}
				}
			}
		}
		
		
			/*
			if (nodeExeCombination.get(node).equals("Must")) {
				if (node instanceof Block) {
					if (!freshMarkMap.get(node.getParent()).equals("MustNot")) {
						freshMarkMap.put(node.getParent(), "Must");
					}
					else {
						return false;
					}
					Block blk = (Block) node;
					List stmtList = blk.statements();
					// process the block's parent logic
					if (blk.getParent() instanceof IfStatement) {
						if (!hasLoopParent(blk)) {
							IfStatement parentIf = (IfStatement) blk.getParent();
							if (parentIf.getThenStatement().equals(node)) {
								if (parentIf.getElseStatement() != null) {
									markMustNotNode(freshMarkMap, parentIf.getElseStatement(), mdNodeInfo.md);
								}
							} else {
								markMustNotNode(freshMarkMap, parentIf.getThenStatement(), mdNodeInfo.md);
							}
						}
					}
					
					// process the block's children logic
					for( int i = 0; i < stmtList.size(); i ++) {
						Statement stmtInBlock = (Statement) stmtList.get(i);
						if (!freshMarkMap.get(stmtInBlock).equals("MustNot")) {
							freshMarkMap.put(stmtInBlock, "Must");
						}
						else {
							return false;
						}
						
						ReturnVisitor visitor = new ReturnVisitor();
						stmtInBlock.accept(visitor);
						if (visitor.containReturn) {
							for (ASTNode retNode : visitor.returnList) {
								boolean conflict = markMustNotNode(freshMarkMap, retNode, mdNodeInfo.md);
								if (conflict) {
									return false;
								}
							}
						}
						// what about break?
						
//						if (stmtInBlock instanceof IfStatement) {
//							ReturnVisitor visitor = new ReturnVisitor();
//							stmtInBlock.accept(visitor);
//							if (visitor.containReturn) {
//								for (ASTNode retNode : visitor.returnList) {
//									boolean conflict = markMustNotNode(freshMarkMap, retNode, mdNodeInfo.md);
//									if (conflict)
//										return false;
//								}
//							}
//						}
//						if (stmtInBlock instanceof ReturnStatement) {
//							for(ASTNode key: freshMarkMap.keySet()) {
//								if (key.getStartPosition() > stmtInBlock.getStartPosition()) {
//									if (!freshMarkMap.get(key).equals("Must")) {
//										freshMarkMap.put(key, "MustNot");
//									}
//									else {
//										return false;
//									}
//								}
//							}
//						}
					}
				} else {
					boolean conflict = markMustNodeCoverage(freshMarkMap, node, mdNodeInfo.md);
					if (conflict)
						return false;
				}
			} else {
				boolean conflict = markMustNotNode(freshMarkMap, node, mdNodeInfo.md);
				if (conflict)
					return false;
			}
		}
		
		return true;
		*/
	}
	
	
	private boolean markMustNotNode(HashMap<ASTNode, String> astNodeMarkMap,
							ASTNode markNode, ASTNode rootEntryMethodNode) {
		
		if (markNode instanceof Block) {
			int startLocationOfBlock = markNode.getStartPosition();
			int endLocationOfBlock = markNode.getStartPosition()+markNode.getLength()-1;
			for (ASTNode node : astNodeMarkMap.keySet()) {
				if(node.getStartPosition() >= startLocationOfBlock &&
						node.getStartPosition() < endLocationOfBlock) {
					if (astNodeMarkMap.get(markNode).equals("Must"))
						return true;
					astNodeMarkMap.put(node, "MustNot");
				}
			}
		} else {
			if (markNode.getParent() instanceof Block) {
				// find the index of this node
				// go backwards to mark as MustNot, until get a if with return;
				
			}
		}
		
		return false;
	}
	
	private boolean markMustNodeToTop(HashMap<ASTNode, String> astNodeMarkMap,
			ASTNode markNode, ASTNode rootEntryMethodNode) {

		if (markNode instanceof Block) {
			astNodeMarkMap.put(markNode, "Must");
			ASTNode parentNode = markNode.getParent();
			
		}	
		return false;
	}
	/*
	private boolean markMustNodeCoverage(HashMap<ASTNode, String> astNodeMarkMap, ASTNode markNode, ASTNode rootEntryMethodNode) {
		ASTNode parent = markNode.getParent();
		
		if (parent instanceof Block) {
			Block blk = (Block) parent;
			List stmtList = blk.statements();
			
		} else if(parent instanceof SwitchStatement) {
			
				if (!astNodeMarkMap.get(markNode).equals("MustNot")) {
					astNodeMarkMap.put(markNode, "Must");
				}
				else {
					return true;
				}
				
				astNodeMarkMap.put(markNode,"Must");
				SwitchStatement switchStatement = (SwitchStatement) parent;
				List stmtList = switchStatement.statements();
				HashMap<ASTNode, ArrayList<ASTNode>> caseStmtAndItsChildren = new HashMap<>();
				ArrayList<ASTNode> caseStmtList = new ArrayList<>();
				ArrayList<ASTNode> otherStmt = new ArrayList<>();
				for (int i = 0; i < stmtList.size(); i ++) {
					if (stmtList.get(i) instanceof SwitchCase) {
						ArrayList<ASTNode> childrenNodes= new ArrayList<>();
						caseStmtAndItsChildren.put((ASTNode)stmtList.get(i), childrenNodes);
						caseStmtList.add((ASTNode)stmtList.get(i));
					}
					else {
						otherStmt.add((ASTNode)stmtList.get(i));
					}
				}
				
				for (int i = 0; i < caseStmtList.size(); i ++) {
					if (i == caseStmtList.size() -1) {
						for (int j = 0; j < otherStmt.size(); j ++) {
							if (otherStmt.get(j).getStartPosition() > caseStmtList.get(i).getStartPosition()) {
								caseStmtAndItsChildren.get(caseStmtList.get(i)).add(otherStmt.get(j));
							}
						}
					} else {
						ASTNode thisCase = caseStmtList.get(i);
						ASTNode nextCase = caseStmtList.get(i+1);
						for (int j = 0; j < otherStmt.size(); j ++) {
							if (otherStmt.get(j).getStartPosition() > thisCase.getStartPosition()
									|| otherStmt.get(j).getStartPosition() < nextCase.getStartPosition()) {
								caseStmtAndItsChildren.get(thisCase).add(otherStmt.get(j));
							}
						}
					}
				}
				
				ArrayList<ASTNode> markMustNodeList = new ArrayList<>();
				markMustNodeList.add(markNode);
				for(ASTNode exeNodeafterCase : caseStmtAndItsChildren.get(markNode)) {
					markMustNodeList.add(exeNodeafterCase);
				}
				
				
				if(!hasLoopParent(markNode)) {
					for (int i = 0; i < stmtList.size(); i ++) {
						ASTNode toMarkNode = (ASTNode)stmtList.get(i);
						if (markMustNodeList.contains(toMarkNode)) {
							
						} else {
							
						}
					}
				}
			
		} else {
			System.out.println("Exceptional log add point!");
		}
		return false;
	}
	*/
	
	private int getIndexOfRelatedStmtFromMarkNode(List stmtList, ASTNode markNode) {
		for (int i = 0; i < stmtList.size(); i ++) {
			Statement currentStmt = (Statement) stmtList.get(i);
			int startPos = currentStmt.getStartPosition();
			int endPos = currentStmt.getStartPosition() + currentStmt.getLength() -1;
			if (markNode.getStartPosition() >= startPos &&
					markNode.getStartPosition() + markNode.getLength() - 1 <= endPos) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean isStmtContainReturnStmt(ASTNode node) {
		ReturnVisitor visitor = new ReturnVisitor();
		node.accept(visitor);
		return visitor.containReturn;
	}
	
	protected void combine(int index, HashMap<ASTNode, String> current, HashMap<ASTNode,ArrayList<String>> map, List<HashMap<ASTNode, String>> list) {
		
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
	
	static void combineASTNodePossibility(int index, HashMap<ASTNode, ArrayList<ASTNode>> current
			, HashMap<ASTNode,ArrayList<ArrayList<ASTNode>>> map, List<HashMap<ASTNode, ArrayList<ASTNode>>> list) {
//		System.out.println(index);
		if (index == map.size()) {
			HashMap<ASTNode, ArrayList<ASTNode>> newMap = new HashMap<>();
			for(ASTNode key : current.keySet()) {
				newMap.put(key, current.get(key));
			}
			list.add(newMap);
		} else {
			Object currentKey = map.keySet().toArray()[index];
			for (ArrayList<ASTNode> edge : map.get(currentKey)) {
				current.put((ASTNode) currentKey, edge);
				combineASTNodePossibility(index+1, current, map, list);
				current.remove(currentKey);
			}
		}
		
	}
	
	private boolean hasLoopParent(ASTNode node) {
		ASTNode current = node;
		while (!(current instanceof MethodDeclaration)) {
			if (current instanceof ForStatement || current instanceof WhileStatement || current instanceof EnhancedForStatement) {
				return true;
			}
			current = current.getParent();
		}
		
		return false;
	}
	
}
