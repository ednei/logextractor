package extractors;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.*;

import data.MethodDesc;
import data.MethodEdge;

public class MethodCallDetectorVisitor extends ASTVisitor {

	private static Logger log = Logger.getLogger(MethodCallDetectorVisitor.class.getName());

	IJavaProject javaproject;

	//Set<MethodEdge> invokemap;
	
	String compilationUnitName;
	CompilationUnit compilationunit;
	

	public MethodCallDetectorVisitor(IJavaProject javaproj, CompilationUnit compilationunit, String compliationUnitName) {
		this.javaproject = javaproj;
		//this.invokemap = new TreeSet<MethodEdge>();
		this.compilationunit = compilationunit;
		this.compilationUnitName = compliationUnitName;
	}
	
	
	int inloop = 0;
	
	public boolean visit(ForStatement node) {
		inloop+=1;
		return true;
	}
	
	public boolean visit(WhileStatement node) {
		inloop+=1;
		return true;
	}
	
	public boolean visit(DoStatement node) {
		inloop+=1;
		return true;
	}
	
	public void endVisit(ForStatement node) {
		inloop-=1;
	}
	
	public void endVisit(WhileStatement node) {
		inloop-=1;
	}
	
	public void endVisit(DoStatement node) {
		inloop-=1;
	}
	
	
	public boolean visit(ClassInstanceCreation node) {
		//log.info("CONSTRUCTOR CALL: " + node);
		IMethodBinding mb = node.resolveConstructorBinding();
		return processMethod(node, mb);
	}
	
	public boolean visit(MethodInvocation node) {	
		/*
		Method invocation expression AST node type. For JLS2:
			 MethodInvocation:
			     [ Expression . ] Identifier
			         ( [ Expression { , Expression } ] )
			 
			For JLS3, type arguments are added:
			 MethodInvocation:
			     [ Expression . ]
			         [ < Type { , Type } > ]
			         Identifier ( [ Expression { , Expression } ] )
		 */
		//log.info("CALL: " + node);
		//log.info("NOME: " + node.getName().getIdentifier());
		/*
		Expression expression = node.getExpression();
		if (expression == null){
			log.info("Expression not found");
		}else{
			log.info("EXPRESSION: "+expression.toString()) ;
		}*/
		//TypeBinding is the type definition for what Method invocation expression is resolved to see JLS above.
		/*
		ITypeBinding typeBinding = node.resolveTypeBinding();
		if (typeBinding==null){
			log.info("TypeBinding not found");
		}else{
			log.info("Expression TYPE:"+typeBinding.getName());
			//log.info("Expression TYPE:"+typeBinding.toString());
		}
		*/
		//This is the method been called
		IMethodBinding methodInvocationBinding = node.resolveMethodBinding();
		return processMethod(node, methodInvocationBinding);
	}
	
	private boolean processMethod(ASTNode node, IMethodBinding calleeMethodBinding) {
		
		if (calleeMethodBinding==null) {
			//log.severe("cannot resolve callee method binding ");
			return true;
		}
		//log.info("MethodBinding:" + calleeMethodBinding.toString());
		
		String calleeReturnType = Utils.safeResolveType(javaproject, calleeMethodBinding.getReturnType());
		
		ITypeBinding calleeMethodClass = calleeMethodBinding.getDeclaringClass();
		if (calleeMethodClass == null){
			//TODO check what generates this situation
			//log.severe("Cannot resolve binding to Class or Interface where method." + calleeMethodBinding);
			return true;
		}
		MethodDesc callee = new MethodDesc(calleeMethodClass.getTypeDeclaration().getQualifiedName(), 
				calleeMethodBinding.getName(), "", calleeReturnType);
		
		//This step suppose that the MethodInvocation happens on the body of a MethodDeclaration
		//which is the parent node on the AST tree of this MethodInvocation.      
		MethodDeclaration callerMethod = (MethodDeclaration)findInvoker(node);
		if (callerMethod == null) {
			//TODO check what generates this situation
			//log.severe("cannot resolve caller method");
			return true;
		} 
		
		//This step suppose that the MethodInvocation happens on the body of a class from which
		//MethodDeclaration invoker belongs to, this is the caller class.
		TypeDeclaration callerMethodClassDef;
		try {
			callerMethodClassDef = (TypeDeclaration)findClassDef(callerMethod);
		} catch (ClassCastException e) {
			// invoker is in a anonymous class definition
			//TODO check what generates this situation
			//log.severe("cannot find caller method class");
			return true;
		}
		
		if (callerMethodClassDef == null){
			//TODO check what generates this situation
			//log.severe("cannot find caller method class");
			return true;
		}
		ITypeBinding callerMethodClass = (ITypeBinding) callerMethodClassDef.resolveBinding();
		
		IMethodBinding callerMethodBinding = callerMethod.resolveBinding();
		if (callerMethodBinding == null){
			//TODO check what generates this situation
			//log.severe("cannot find caller method binding - " + callerMethod.getName());
			return true;
		}
		String callerReturnType = Utils.safeResolveType(javaproject, callerMethodBinding.getReturnType());
		MethodDesc caller = new MethodDesc(callerMethodClass.getTypeDeclaration().getQualifiedName(),
				callerMethodBinding.getName(), "", callerReturnType);
				
		int line = this.compilationunit.getLineNumber(node.getStartPosition());
				
		try {
			MethodEdge edge = new MethodEdge(caller, callee, line, inloop>0);
			edge.writeToDB();
					
			//int edgeid = edge.writeToDB();
			/*
			if (invokemap.contains(edge)) {
				;
			} else {
				invokemap.add(edge);
			}
			*/
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Statement codeblock = (Statement) Utils.findBlockStatement(node);
		if (codeblock instanceof Block) {
			Block blk = (Block) codeblock;
			for (Object st : blk.statements()) {
				if (log.isLoggable(Level.FINER)){
					log.fine("STATMENT:: " + st + st.getClass());
				}
				if (st instanceof ExpressionStatement) {
					Expression expr = ((ExpressionStatement) st)
							.getExpression();
					if (expr instanceof MethodInvocation) {
						MethodInvocation logcall = (MethodInvocation) expr;
						IMethodBinding logmb = logcall.resolveMethodBinding();
						if (logmb == null) {
							log.fine("cannot resolve binding " + logcall);
							continue;
						}
						if (logmb.getDeclaringClass().getName().toString().equals(
								"Logger")) {
							int logstart = logcall.getStartPosition();
							int logline = compilationunit.getLineNumber(logstart);
							if (logstart <= node
									.getStartPosition()) {
								if (log.isLoggable(Level.FINER)) log.finer("PRE::" + logcall +" Line:" + logline);
								//DbUtils.insertPreLog(compilationUnitName+"-"+logline,
								//		edgeid);
							} else {
								log.finer("POST::" + logcall +" Line:" + logline);
								//if (log.isLoggable(Level.FINER)) DbUtils.insertPostLog(compilationUnitName+"-"+logline,
								//		edgeid);
							}
						}
					}
				}
			}
		}
		return true;		
	}
	

	public ASTNode findInvoker(ASTNode node){
		ASTNode p = node.getParent();
		if (p ==null || p instanceof MethodDeclaration)
			return p;
		else {
			//LOG.finer( p.getClass() );
			return findInvoker(p);
		}
	}

	public ASTNode findClassDef(ASTNode node) {
		ASTNode p = node.getParent();
		if (p ==null || p instanceof TypeDeclaration || p instanceof AnonymousClassDeclaration )
			return p;
		else {
			//LOG.finer( p.getClass() );
			return findInvoker(p);
		}
	}



}
