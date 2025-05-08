package ca.yorku.nemo.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class FindLogVisitor extends ASTVisitor {
	ArrayList<ASTNode> logPrintAndMIStmt = new ArrayList<>();
	
	
	public HashMap<String, Boolean> methodContainInternalCall = new HashMap<>();

	public HashMap<String, Boolean> methodContainLog = new HashMap<>();
	
	public HashSet<String> methodSet = new HashSet<>();
	
//	public HashMap<String, HashSet<String>> methodInternalCalls = new HashMap<>();
	
	String invokeHeuristics = "";
	
	public FindLogVisitor(String invokeHeuristics) {
		super();
		this.invokeHeuristics = invokeHeuristics;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		IMethodBinding binding = node.resolveBinding();
		String key = binding.getDeclaringClass().getQualifiedName() + "|||||" + binding.toString();
		
		methodSet.add(key);
		
		return true;
	}
	
	@Override
	public boolean visit(ExpressionStatement node) {
		if(FileUtils.ifLogPrinting(node.toString())) {
			logPrintAndMIStmt.add(node);
			MethodDeclaration md = FileUtils.getDeclarationMethodNode(node);
			String defineClass = md.resolveBinding().getDeclaringClass().getQualifiedName();
			String methodBindingStr = md.resolveBinding().toString();
			
			String key = defineClass + "|||||" + methodBindingStr;
			methodContainLog.put(key, true);
			
			return false;
		}
		
		
		
		return true;
	}
	
	public boolean visit(MethodInvocation mi) {
		
		MethodDeclaration md = FileUtils.getDeclarationMethodNode(mi);
		if(md == null) {
//			System.out.println(mi);
			return false;
		}
		if (md.resolveBinding() == null) {
			System.out.println(md);
		}
		
		String callerMethodKey = md.resolveBinding().getDeclaringClass().getQualifiedName() + "|||||" + md.resolveBinding().toString();
		
		
		IMethodBinding binding = mi.resolveMethodBinding();
		if (binding == null) {
//			System.out.printf("%s\n%s\n", callerMethodKey, mi);
			return false;
		}
		if (binding.getDeclaringClass().getQualifiedName().startsWith(invokeHeuristics)) {
//			String calleeMethodKey = binding.getDeclaringClass().getQualifiedName() + "|||||" + binding.toString();
			methodContainInternalCall.put(callerMethodKey, true);
		}
		
		return true;
	}
}
