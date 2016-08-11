package extractors.java.android;

import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import data.ElementList;
import data.StringExprElement;

//Some Class on Telephony Packages declares a method which has only MethodInvocations for Logger class methods. 
//I call this pattern of LogMethod pattern and this class detects when compilation unit is using this pattern. 
public class LogMethodDetectorVisitor extends ASTVisitor{
	private static Logger log = Logger.getLogger(AndroidLogParser.class.getName());	
	private ArrayList<LogMethodDescription> logMethodDescriptions = new ArrayList<LogMethodDescription>();
	private boolean isLogMethod;
	private LogMethodDescription logMethodDescription;
	private AndroidLogParser androidLogParser;
	private Optional<LogMethodDescription> currentLogMethodDescription;
	
	public LogMethodDetectorVisitor(AndroidLogParser androidLogParser) {
		this.androidLogParser = androidLogParser;
	}
	
	public boolean visit(MethodDeclaration node){
		final ArrayList<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>();
		isLogMethod = true; // first we assume that this method is a log method and the visit his method invocation nodes
		if(node.toString().contains("private void riljLog")){
			log.info("riljLog");
		}
		node.accept(new ASTVisitor() {
			public boolean visit(MethodInvocation node){
				if(!isLogMethod) return true; //we already know that is not a log method so by pass all remaining nodes
				if(androidLogParser.isMethodInvocationOnLoggerClass(node)){
					methodInvocations.add(node);
					logMethodDescription = new LogMethodDescription(node,
						androidLogParser.getLogMessageElementList(node).list,
						androidLogParser.getLevel());
				}else{
					isLogMethod = false;
				}
				return true;
			}
			//Avoid set methods be selected as log methods when they have only assignment and MethodInvocation for a log class..
			public boolean visit(Assignment node) {
				isLogMethod = false;
				return true;
			}
			//Avoid get methods be selected as log methods when they have only return and MethodInvocation for a log class.
			public boolean visit(ReturnStatement node) {
				isLogMethod = false;
				return true;
			}
		});
		
		if(isLogMethod && methodInvocations.size()==1){ //we must have at list one loggger class method invocation
			IMethodBinding mb = node.resolveBinding();
			logMethodDescription.setName(mb.getName());
			for(Object param : node.parameters()){
				if(param instanceof SingleVariableDeclaration){
					String parameterName = ((SingleVariableDeclaration)param).getName().getIdentifier();
					logMethodDescription.addParameterName(parameterName);
				}else{
					log.info("oxiiii");
				}
			}
			logMethodDescriptions.add(logMethodDescription);
			return true;
		}
		if(isLogMethod && methodInvocations.size()==2){
			log.severe("This is a log Method who has more then 1 Method Invocation for a log class method, what we should do??");
			throw new UnsupportedOperationException(
					"found log Method who has more then 1 Method Invocation for a log class method");
		}
		return false;
	}

	public boolean isInvocatedByLogMethod(MethodInvocation node) {
		return logMethodDescriptions.stream().anyMatch(d -> d.hasMethodInvocation(node));
	}	
	
	public boolean isLogMethodInvocation(MethodInvocation methodInvocation) {
		searchLogMethodDescription(methodInvocation); 
		return currentLogMethodDescription.isPresent();
	}

	private void searchLogMethodDescription(MethodInvocation methodInvocation) {
		Optional<IMethodBinding> mb = Optional.ofNullable(methodInvocation.resolveMethodBinding());
		if(mb.isPresent()){
			currentLogMethodDescription = logMethodDescriptions.stream().
				filter(d->d.hasName(mb.get().getName())).findFirst();
		}else{
			currentLogMethodDescription = Optional.empty();
		}
	}
	
	public LogMethodDescription getLogMethodDescription(MethodInvocation methodInvocation){
		if(currentLogMethodDescription.isPresent()){
			logMethodDescription = currentLogMethodDescription.get();
			//check if the current is the one we are trying to get.
			Optional<IMethodBinding> mb = Optional.ofNullable(methodInvocation.resolveMethodBinding());
			if(mb.isPresent() && logMethodDescription.hasName(mb.get().getName()))
				return logMethodDescription;
		}
		searchLogMethodDescription(methodInvocation);
		//if not found, then this will throw a exception.
		return currentLogMethodDescription.get();
	}
	
	public class LogMethodDescription{
		private ArrayList<StringExprElement> logMethodElementList;
		private Level level;
		private String name;
		private MethodInvocation methodInvocation;
		private ArrayList<String> parameterNames;

		public LogMethodDescription(MethodInvocation methodInvocation,
				ArrayList<StringExprElement> elementList, Level level) {
			this.methodInvocation = methodInvocation;
			this.logMethodElementList = elementList;
			this.level = level;
			parameterNames = new ArrayList<String>();
		}

		public void addParameterName(String paramName) {
			parameterNames.add(paramName);
		}

		public boolean hasName(String name) {
			return this.name == name;
		}

		public boolean hasMethodInvocation(MethodInvocation methodInvocation) {
			return this.methodInvocation == methodInvocation;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Level getLevel() {
			return level;
		}

		//create expanded list as a copy from listToExpand, 
		//replace at expanded list the element with name == argumentName by all elements at argument element list.
		//replace argument element list by the expanded list 
		public ElementList expandElementList(ElementList argument) {
			ArrayList<StringExprElement> expanded = new ArrayList<StringExprElement>(logMethodElementList);
			//The number of arguments from method invocation should match the number of parameters from LogMethodDescription.
			//There should be a list of argument names and a list of arguments expanded, ElementList type as parameter here
			//doesn't seems to fit ok, we also need a list of ElementLists.
			for(String paramName : parameterNames){
				Optional<StringExprElement> element = expanded.stream().filter(
					e -> e.hasName(paramName)).findFirst();
				
				if(element.isPresent()){
					int index = expanded.indexOf(element.get());
					expanded.remove(index);
					expanded.addAll(index, argument.list);
				}
			}
			argument.list = expanded;
			//log.info("result: " + argument.toString());
			//log.info("regexpr: " + argument.toRegExpr(true));
			//log.info("namemap: " + argument.getNameMapString());
			//log.info("typemap: " + argument.getTypeMapString());
			return argument;
		}
	}
}