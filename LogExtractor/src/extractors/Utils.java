package extractors;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class Utils {
	
	//private static Logger log = LogFormatter.getLogger(Utils.class);
	
	public static String safeResolveType(IJavaProject javaproject, ITypeBinding typebinding) {
		try {

			if (typebinding.isArray()) {
				return "ARRAY OF " +safeResolveType(javaproject, typebinding.getElementType() );
			}
			if (typebinding.isParameterizedType()) {
				String retstr = "GENERIC " + safeResolveType(javaproject, typebinding.getErasure())
						+" OF ";

				//if (retstr.contains("java.lang.Class")) {
				//	int t=1;
				//}

				ITypeBinding[] typeargs = typebinding.getTypeArguments();
				for (ITypeBinding bd: typeargs) {
					if (bd.isWildcardType()) {
						retstr += "WILD";
						ITypeBinding tt = bd.getTypeDeclaration();
						//System.err.println(tt +" " +Arrays.deepToString(tt.getTypeArguments()));
						retstr += " EXTENDS " + safeResolveType(javaproject, tt.getErasure());
					} else {
						retstr += safeResolveType(javaproject, bd);
					}
				}
				return retstr;
			}

			if (typebinding.isPrimitive()) {
				return "PRIMITIVE " + typebinding.getQualifiedName();
			}
			IType itype = javaproject.findType(typebinding.getQualifiedName());
			if (itype==null) {
				return "????";
			} else {
				return itype.getFullyQualifiedName();
			}
		} catch (Exception e) {

			System.err.println("ERROR resolving type " + typebinding.getQualifiedName());
			e.printStackTrace();
			return "????";
		}
	}
	
	
	public static ASTNode findBlockStatement(ASTNode node) {
		ASTNode p = node.getParent();
		if (p==null || p instanceof Block) {
			return p;
		} else if (p instanceof IfStatement) {
			return p;
		} else if (p instanceof ForStatement) {
			return p;
		} else if (p instanceof WhileStatement) {
			return p;
		}  else if (p instanceof DoStatement) {
			return p;
		} else {
			return findBlockStatement(p);
		}
	}
	
}
