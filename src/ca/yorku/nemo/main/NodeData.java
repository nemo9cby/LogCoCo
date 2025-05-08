package ca.yorku.nemo.main;

import org.eclipse.jdt.core.dom.ASTNode;

public class NodeData {
	ASTNode astNode;
	
	boolean isLog;
	
	public NodeData(ASTNode astNode) {
		this.astNode = astNode;
		isLog = false;
	}
	
	public NodeData(ASTNode astNode, boolean isLog) {
		this.astNode = astNode;
		this.isLog = isLog;
	}
}
