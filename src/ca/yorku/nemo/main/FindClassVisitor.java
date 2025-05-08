package ca.yorku.nemo.main;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class FindClassVisitor extends ASTVisitor{
	ArrayList<String> qualifyNameList = new ArrayList<>();
	
	@Override
	public boolean visit(TypeDeclaration node) {
		
		ITypeBinding binding = node.resolveBinding();
		if (binding != null) {
			if (!binding.getQualifiedName().equals("")) {
				qualifyNameList.add(binding.getQualifiedName());
			}
		}
		return true;
	}
}
