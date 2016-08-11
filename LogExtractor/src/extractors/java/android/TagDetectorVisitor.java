package extractors.java.android;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import data.ElementList;
import data.StringExprElement;

//Lots of compilation units defines a LOG_TAG variable which contains the Component Tag
//We visit VariableDeclarationFragment nodes looking for this tag and save it.
public class TagDetectorVisitor extends ASTVisitor {
	private Optional<ArrayList<StringExprElement>> tagElementList;
	private AndroidLogParser androidLogParser;
	
	public TagDetectorVisitor(AndroidLogParser androidLogParser) {
		this.androidLogParser = androidLogParser;
		tagElementList = Optional.empty();
	}

	public boolean visit(VariableDeclarationFragment node){
		if(tagElementList.isPresent()) return false; // we already found it then stop looking.
		String expression = node.toString();
		if(expression.contains("LOG_TAG")){
			//Expession has the format-> LOG_TAG = "value"
			ElementList result = androidLogParser.getElementList(Optional.of(node));
			ArrayList<StringExprElement> list = result.list;
			StringExprElement item =  list.get(0);
			list.remove(0);
			list.add(item);
			result.list = list;
			//log.info("result: " + result.toString() );
			//log.info("regexpr: " + result.toRegExpr(true));
			//log.info("namemap: " + result.getNameMapString());
			//log.info("typemap: " + result.getTypeMapString());
			tagElementList = Optional.of(result.list);
		}else{
			tagElementList = Optional.empty();
		}
		return false;
	}
	
	public ElementList expandElementList(ElementList elementList) {
		//TODO We suppose that this should be present, if not we should map how to recovery it for this compilation unit, or at least how many case do we have.
		if (tagElementList.isPresent()){
			elementList.list.addAll(0, tagElementList.get());
		}	
		return elementList;
	}
}
