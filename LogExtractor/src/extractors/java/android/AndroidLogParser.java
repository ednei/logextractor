package extractors.java.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

import data.DbUtils;
import data.ElementList;
import extractors.LogParser;
import extractors.MethodCallDetectorVisitor;
import extractors.StringExprExpandVisitor;
import extractors.Utils;
import extractors.java.android.LogMethodDetectorVisitor.LogMethodDescription;

public class AndroidLogParser extends ASTVisitor implements LogParser{
	private static Logger log = Logger.getLogger(AndroidLogParser.class.getName());	
	private static ArrayList<LogClassParser> loggerClassParsers;
	
	private CompilationUnit compilationUnit;
	private String compilationUnitName;
	private int inloop = 0;
	private LogClassParser loggerClassParser;
	private LogMethodDetectorVisitor logMethodDetector;
	private StringExprExpandVisitor stringExprExpandVisitor;
	private String currentClass;
	private String currentMethod;
	private TagDetectorVisitor tagDetector;
	
	public AndroidLogParser() {
		if(loggerClassParsers == null){
			loggerClassParsers = new ArrayList<LogClassParser>(); 
			loggerClassParsers.add(new RlogClassParser());
		}
		stringExprExpandVisitor = new StringExprExpandVisitor();
	}
	
	public void processSourceFile(IJavaProject javaproject, IType tp){
		ICompilationUnit cpunit = tp.getCompilationUnit();
		@SuppressWarnings("deprecation")
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setSource(cpunit);
		CompilationUnit ast = (CompilationUnit) parser.createAST(null);
		
		this.compilationUnit = ast;
		this.compilationUnitName = tp.getFullyQualifiedName();
		this.currentClass = this.compilationUnitName;  
		this.currentMethod = "static";
		stringExprExpandVisitor.setJavaProject(javaproject);
		logMethodDetector = new LogMethodDetectorVisitor(this);
		tagDetector = new TagDetectorVisitor(this);
		ast.accept(new MethodCallDetectorVisitor(javaproject, ast,tp.getFullyQualifiedName()));
		ast.accept(tagDetector);
		ast.accept(logMethodDetector);
		ast.accept(this);
	}
		
	public boolean isMethodInvocationOnLoggerClass(MethodInvocation node){
		for (LogClassParser parser: loggerClassParsers) {
			if (parser.isMethodInvocationOnLoggerClass(node)){
				this.loggerClassParser = parser;
				return true;
			}
		}
		return false;
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding mb = node.resolveBinding();
		if (mb==null) {
			log.finer("failed resolving binding for method declaration "+node.getName());
			return true;
		}
		String classname = mb.getDeclaringClass().getQualifiedName();
		if (classname.length()==0) {
			//TODO add support for annonymous class if needed.
			//log.info("hit annonymous class definition.. skiping " + node.getName());
			return true;
		}
		currentClass = classname;
		currentMethod = mb.getName();
		//log.info("in method " + currentClass +"." + currentMethod);
		return true;
	}
	
	public boolean visit(MethodInvocation mi) {
		
		if (logMethodDetector.isInvocatedByLogMethod(mi)){
			return true; //this method invocation was already processed by LogMethodDetector class
		}
		
		ElementList result;
		Level level;
		
		if (logMethodDetector.isLogMethodInvocation(mi)){
			LogMethodDescription logMethodDescription = logMethodDetector.getLogMethodDescription(mi);
			
			@SuppressWarnings("rawtypes")
			List args = mi.arguments();
			if(args.size() > 1){
				//throw new UnsupportedOperationException("Handle log method with more then one argument is not suported yet");
				log.info("Handle log method with more then one argument is not suported yet");
			}
		 
			Optional<ASTNode> arg;
			if(args.size()<1){
				arg = Optional.empty();
			}else{
				arg = Optional.of((ASTNode)args.get(0));
			}
			
			result = tagDetector.expandElementList(
					logMethodDescription.expandElementList(getElementList(arg,mi)));
			level = logMethodDescription.getLevel();
			processOneLoggerCall(mi, result, level);
			return true;
		}
		
		if (isMethodInvocationOnLoggerClass(mi)) {
			result = tagDetector.expandElementList(getLogMessageElementList(mi));
			//this.expandString(node, args);
			level = loggerClassParser.getLevel();
			processOneLoggerCall(mi, result, level);
			return true;
		}
		return true;
	}
	
	public ElementList getLogMessageElementList(MethodInvocation mi) {
		ASTNode arg = loggerClassParser.getMessage();
		return getElementList(Optional.ofNullable(arg), mi);
	}
	
	public ElementList getElementList(Optional<ASTNode> arg,MethodInvocation mi) {
		ElementList result = getElementList(arg);
		result.line = compilationUnit.getLineNumber(mi.getStartPosition());
		return result;
	}
	
	public ElementList getElementList(Optional<ASTNode> arg) {
		ElementList result = new ElementList(compilationUnitName,0);
		if(arg.isPresent()){
			//if (arg instanceof Expression) {
			stringExprExpandVisitor.expandElementList(arg.get(),result);
			//log.info("result: " + result.toString() );
			//log.info("regexpr: " + result.toRegExpr(true));
			//log.info("namemap: " + result.getNameMapString());
			//log.info("typemap: " + result.getTypeMapString());
			//}
		}
		return result;
	}
		
	protected void processOneLoggerCall(MethodInvocation node, ElementList result, Level level) {
		//log.info("result: " + result.toString());
		log.info("regexpr: " + result.toRegExpr(true));
		//log.info("namemap: " + result.getNameMapString());
		//log.info("typemap: " + result.getTypeMapString());
				
		int methodid = DbUtils.findMethodId(this.currentClass,
				this.currentMethod, "");
		if (methodid == -1) {
			log.warning("FAILED TO FIND METHOD:: "
					+ this.currentClass + " " + this.currentMethod);
		}
		DbUtils.insertLogEntry(this.compilationUnitName + "-" + result.line,
				result, level, inloop > 0, methodid);
		Statement codeblock = (Statement) Utils.findBlockStatement(node);
		@SuppressWarnings("unused")
		int blockline = compilationUnit.getLineNumber(codeblock.getStartPosition());	
	}

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

	public Level getLevel() {
		return this.loggerClassParser.getLevel();
	}	
}
