package ca.yorku.nemo.main;

import java.util.LinkedList;
import java.util.List;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;

public class TreeNode {
	private NodeData data;
	TreeNode parent;
	List<TreeNode> children;
	
	public TreeNode(NodeData data) {
		 this.data = data;
		 this.children = new LinkedList<TreeNode>();
	}
	
	public TreeNode addChild(NodeData data) {
		TreeNode childNode = new TreeNode(data);
		childNode.parent = this;
		this.children.add(childNode);
		return childNode;
	}
	
	@Override
	public String toString() {
		return data.astNode.getClass().getName() + "|||" +  data.astNode.toString();
	}
	
	public void dfsTraverse() {
		if (children.size() > 0) {
			for (int i = 0; i < children.size(); i ++) {
				TreeNode tNode =children.get(i);
				tNode.dfsTraverse();
			}
		} else {
			if (this.data.isLog) {
				ExpressionStatement logES = (ExpressionStatement) this.data.astNode;
				logES.getStartPosition();
				System.out.println(data.astNode.toString());
			} 
		}
	}
	
}