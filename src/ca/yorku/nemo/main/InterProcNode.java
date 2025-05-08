package ca.yorku.nemo.main;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Objects;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.internal.compiler.classfmt.MethodInfo;

public class InterProcNode {
	ASTNode astNode;
	
	CompilationUnit cu;
	
	boolean mayInTraverse = false;
	
	boolean isLog = false;
	
	String filePath;
	
	MethodInvocation invokeMD = null;
	
	String shortFilePath;
	
	public InterProcNode(CompilationUnit cu, String filePath, ASTNode astNode) {
		
		this.cu = cu;
		this.filePath = filePath;
		this.astNode = astNode;
		
		String splitRegex = "";
		if (File.separator.equals("\\")) {
			splitRegex = "\\\\";
		} else {
			splitRegex = "/";
		}
		
		this.shortFilePath = filePath.split(splitRegex)[filePath.split(splitRegex).length-1];
	}
	
	public int getStartLineOfNode() {
		return cu.getLineNumber(astNode.getStartPosition());
	}
	
	public int getEndLineOfNode() {
		return cu.getLineNumber(astNode.getStartPosition()+astNode.getLength()-1);
	}
	
	@Override
	public String toString() {
		
		String type = astNode.getClass().toString();
		int lineNumber = cu.getLineNumber(astNode.getStartPosition());
		int endLineNumber = cu.getLineNumber(astNode.getStartPosition()+astNode.getLength()-1);
		String special = astNode.toString().split("\\r?\\n")[0];
		
		String shortType = type.split("\\.")[type.split("\\.").length-1];
		
		return String.format("TYPE:%s,%s,%d:%d,%b", shortType, shortFilePath, lineNumber,endLineNumber,mayInTraverse);
		
	}
	
	
	@Override
	public int hashCode() {
		if (this.astNode instanceof MethodDeclaration && this.invokeMD != null) {
			return Objects.hash(this.invokeMD, this.astNode);
		}
		else {
			return this.astNode.hashCode();
		}
	}
	
	@Override
	public boolean equals(Object node) {
		
		if (node instanceof InterProcNode) {
			InterProcNode toCompare = (InterProcNode) node;
			
			if (this.astNode instanceof MethodDeclaration && 
					toCompare.astNode instanceof MethodDeclaration && this.invokeMD != null && toCompare.invokeMD != null) {
				return (this.astNode.equals(toCompare.astNode) && this.invokeMD.equals(toCompare.invokeMD));
			}
			
			return this.astNode.equals(toCompare.astNode);
		}
		return false;
	}
}
