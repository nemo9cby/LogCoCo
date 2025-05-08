package ca.yorku.nemo.main;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodSummaryVisitor extends ASTVisitor {
	
	ArrayList<MethodDeclaration> methodNodeList = new ArrayList<>();
	
	
	
	@Override
	public boolean visit(MethodDeclaration node) {
//		IMethodBinding binding = node.resolveBinding();
		if (!node.isConstructor()) {
			methodNodeList.add(node);
		}	
		return false;
	}
	
}
