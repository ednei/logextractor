package extractors;

import java.util.ArrayList;
import java.util.logging.Logger;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.*;

import extractors.java.android.RlogParser;

public class LogPrintStatementDetector extends ASTVisitor{

	public static final int LOGTYPE_LOG4J =0;
	public static final int LOGTYPE_JDKLOG =1;
	public static final int LOGTYPE_METHODNAME =2;
	public static final int LOGTYPE_UNKNOWN =3;
	public static final int LOGTYPE_ANDROID = 4;
	
	private static Logger log = Logger.getLogger(LogPrintStatementDetector.class.getName());	
	private static ArrayList<LogParser> loggerClassParsers;
	
	
	public LogPrintStatementDetector(IJavaProject javaproj, CompilationUnit compilationunit, 
			String compilationUnitName, String currentClass, String currentMethod) {
		if(loggerClassParsers==null){
			loggerClassParsers = new ArrayList<LogParser>();
			loggerClassParsers.add(new RlogParser(javaproj,compilationunit,compilationUnitName));
		}
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding mb = node.resolveBinding();
		if (mb==null) {
			log.info("failed resolving binding for method declaration "+node.getName());
			return true;
		}
		String classname = mb.getDeclaringClass().getQualifiedName();
		if (classname.length()==0) {
			// annonymous class
			log.info("hit annonymous class definition.. skiping " + node.getName());
			return true;
		}
		return true;
	}
	
	int inloop = 0;
	private LogParser loggerClassParser;
	
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
	
	private boolean isLoggerClass(String classname) {
		for (LogParser parser: loggerClassParsers) {
			if (parser.canParseClass(classname)){
				this.loggerClassParser = parser;
				return true;
			}
		}
		return false;
	}
	
	public boolean visit(MethodInvocation node) {

		// System.err.println (node.getName());

		IMethodBinding mb = node.resolveMethodBinding();
		if (mb == null) {
			log.info("cannot resolve binding " + node);
			return true;
		}

		@SuppressWarnings("unused")
		ITypeBinding returntype = mb.getReturnType();
		// System.err.println(mb.getDeclaringClass().getName() +"."
		// +mb.getName());
		
		//System.err.println( mb.getDeclaringClass().getName() );
		//LOG.info("Method Call: " + mb.getDeclaringClass().getName() + " " +node.getName());
	
		ITypeBinding declaringClass =  mb.getDeclaringClass();
		
		if (declaringClass == null){
			log.info("declaring class not found. " + node.getName());
			return true;
		}
		
		if (isLoggerClass(mb.getDeclaringClass().getName().toString())) {
			loggerClassParser.visit(node);
		} 
		// String typestring = Utils.safeResolveType(javaproject, returntype);
		//		System.err.println(typestring);
		return true;
	}
}
