package extractors.java.android;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class RlogClassParser implements LogClassParser{
	private static Logger log = Logger.getLogger(RlogClassParser.class.getName());
	private static Level levelMap[] = {Level.WARNING,Level.SEVERE,Level.INFO,Level.FINER,Level.FINER};
	

	private static List<String> loggerMethods;
	
	//private AndroidLogParser logParser;

	private IMethodBinding mb;

	private MethodInvocation methodInvocation;

	private ITypeBinding declaringClass;

	private Level level;

	private ASTNode message;
	
	//TODO we should parse logs with throwable param
	@SuppressWarnings("unused")
	private ASTNode thr;

	private static List<String> getLoggerMethods(){
		if(RlogClassParser.loggerMethods==null){
			String[] loggerMethods = {"w","e","i","v","d"};
			RlogClassParser.loggerMethods = Arrays.asList(loggerMethods);	
		}
		return RlogClassParser.loggerMethods;
	}

	public boolean isMethodInvocationOnLoggerClass(MethodInvocation node){
		methodInvocation = node;
		mb = node.resolveMethodBinding();
		if (mb == null) {
			log.finer("cannot resolve binding " + node);
			return false;
		}
		declaringClass =  mb.getDeclaringClass();
		if (declaringClass == null){
			log.finer("declaring class not found. " + node.getName());
			return false;
		}
		if("Rlog".equals(declaringClass.getName().toString())){
			return parseMethod();
		}
		return false;
	}	
	

	private boolean parseMethod(){
		//TODO we need to check all the methods at Rlog class. 
		String loggerMethodName = methodInvocation.getName().toString();
		if (loggerMethodName.startsWith("isLoggable")){
			return false;
		}
		
		if(loggerMethodName.startsWith("println")){
			parsePrintlnMethod();
			return true;
		}	
		
		//Apply for all others methods w,e,d,v,i
		if (getLoggerMethods().contains(loggerMethodName)){
			parserGeneralMethods();
			return true;
		}
		//
		return false;
	}

	private void parserGeneralMethods() {
		level = translateLevel(methodInvocation.getName().toString());
		@SuppressWarnings("unchecked")
		List<ASTNode> args = methodInvocation.arguments();
		//TODO second argument might be or not message it could be also the Throwable argument.
		message = args.get(1);
		
		if(args.size() > 2){
			thr = args.get(2);
		}
		
		if(args.size() > 3){
			//throw new Exception("General Methods with more then 3 args arent expected on Rlog");
			log.severe("General Methods with more then 3 args arent expected on Rlog");
		}
	}

	private Level translateLevel(String level) {
		return levelMap[getLoggerMethods().indexOf(level)];
	}

	private void parsePrintlnMethod() {
		@SuppressWarnings("unchecked")
		List<ASTNode> args = methodInvocation.arguments();
		//TODO level is a int at RLog so we must treat this here
		log.severe("parsePrintlnMethod doesn't parse level correctly yet");
		level = translateLevel(args.get(0).toString());
		message = args.get(2);
		if(args.size() > 3){
			//throw new Exception("General Methods with more then 3 args arent expected on Rlog");
			log.severe("General Methods with more then 3 args arent expected on Rlog");
		}
	}

	@Override
	public Level getLevel() {
		return level;
	}

	@Override
	public ASTNode getMessage() {
		if(message==null){
			log.severe("message is null");
		}
		return message;
	}
	
	public String getBuffer(){
		//TODO Should extract this from code itself.
		return "RADIO";
	}
}