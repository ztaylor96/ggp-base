package org.ggp.base.util.symbol.factory;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.symbol.factory.exceptions.SymbolFormatException;
import org.ggp.base.util.symbol.grammar.Symbol;
import org.ggp.base.util.symbol.grammar.SymbolAtom;
import org.ggp.base.util.symbol.grammar.SymbolList;
import org.ggp.base.util.symbol.grammar.SymbolPool;

import com.google.common.collect.Lists;

/**
 * A parallel version of SymbolFactory which handles the conversion between
 * String and Symbol objects.
 *
 * HRFReader - converts String to Symbol
 *
 * Grinder - converts Symbol to String
 *
 * @author robertchuchro
 */
public class HRFSymbolFactory {

	public static Symbol create(String string) throws SymbolFormatException {
		try {
			HRFReader reader = (new HRFSymbolFactory()).new HRFReader(string);
			Symbol tokens = reader.read();
			return tokens;
		} catch (Exception e) {
			throw new SymbolFormatException(string);
		}
	}

	public static Symbol createRules(String string) throws SymbolFormatException {
		try {
			// string = string.replace("\"", "");
			HRFReader reader = (new HRFSymbolFactory()).new HRFReader(string);
			// Symbol tokens = reader.readdata();
			Symbol tokens = reader.read();
			// ((SymbolList) tokens).isData = true;
			return tokens;
		} catch (Exception e) {
			throw new SymbolFormatException(string);
		}
	}

	/*
	 * public static void main(String[] args) {
	 *
	 * BufferedReader reader = new BufferedReader(new
	 * InputStreamReader(System.in)); String in = null; try { in =
	 * reader.readLine(); } catch (IOException e) { e.printStackTrace(); }
	 * HRFSymbolFactory factory = new HRFSymbolFactory(); HRFReader hrf =
	 * factory.new HRFReader(in); //List<String> out = hrf.scan();
	 * //System.out.println(out); //System.out.println(out.size()); Symbol
	 * readout = hrf.read(); System.out.println(readout);
	 * //System.out.println(readout.size());
	 *
	 * Symbol readout2 = hrf.readdata(); System.out.println(readout2);
	 * //System.out.println(readout2.size());
	 *
	 * String grinded = Grinder.grind(readout); System.out.println(grinded); }
	 */

	private static boolean parenp(String lop, String op, String rop) {
		List<String> lopList = new ArrayList<String>();
		lopList.add(lop);
		List<String> opList = new ArrayList<String>();
		opList.add(op);
		return precedencep(lopList, op) || !precedencep(opList, rop);
	}

	private static boolean precedencep(List<String> lop, String rop) {
		boolean dum = pp(lop.get(0), rop);
		return dum;
	}

	private static boolean pp(String lop, String rop) {
		if (lop.equals("!")) {
			return !rop.equals("!");
		}
		if (lop.equals("=")) {
			return !rop.equals("!") && !rop.equals("=");
		}
		if (lop.equals(":")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":");
		}
		if (lop.equals("#")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":");
		}
		if (lop.equals("~")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":");
		}
		if (lop.equals("&")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":");
		}
		if (lop.equals("|")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&");
		}
		if (lop.equals("=>")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&") && !rop.equals("|")
					&& !rop.equals("=>") && !rop.equals("<=") && !rop.equals("<=>");
		}
		if (lop.equals("<=")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&") && !rop.equals("|")
					&& !rop.equals("=>") && !rop.equals("<=") && !rop.equals("<=>");
		}
		if (lop.equals("<=>")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&") && !rop.equals("|")
					&& !rop.equals("=>") && !rop.equals("<=") && !rop.equals("<=>");
		}
		if (lop.equals(":-")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&") && !rop.equals("|")
					&& !rop.equals("=>") && !rop.equals("<=") && !rop.equals("<=>");
		}
		if (lop.equals("==>")) {
			return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&") && !rop.equals("|")
					&& !rop.equals("=>") && !rop.equals("<=") && !rop.equals("<=>");
		}
		return !rop.equals("!") && !rop.equals("=") && !rop.equals(":") && !rop.equals("&") && !rop.equals("|")
				&& !rop.equals("=>") && !rop.equals("<=") && !rop.equals("<=>") && !rop.equals(":-")
				&& !rop.equals("==>");
	}

	/**
	 * This class serves to read a string in HRF format and convert it to Symbols.
	 *
	 * The code was adopted from Michael Genereseth's original implementation in
	 * Javascript. Please forward all comments or complaints to him.
	 */
	private final class HRFReader {

		private String input;
		private ArrayList<String> output;
		private int current;

		public HRFReader(String str) {
			input = str;
			output = new ArrayList<String>();
			current = 0;
		}

		private List<Symbol> seq(Object... elements) {
			ArrayList<Symbol> list = new ArrayList<Symbol>();
			for (Object element : elements) {
				if (element instanceof Symbol || element instanceof SymbolList || element instanceof SymbolAtom) {
					list.add((Symbol) element);
				} else if (element instanceof String) {
					list.add(SymbolPool.getAtom((String) element));
				} else if (element instanceof List<?>) {
					list.add(SymbolPool.getList((List<Symbol>) element));
				}
			}
			return list;
		}

		public Symbol readdata() {
			return parsedata(scan());
		}

		public Symbol read() {
			return parse(scan());
		}

		public List<String> scan() {
			int cur = 0;
			int len = input.length();
			while (cur < len) {
				char charcode = input.charAt(cur);
				if (charcode == 32 || charcode == 13) {
					cur++;
				} else if (charcode == 33) {
					output.add("!");
					cur++;
				} else if (charcode == 34) {
					cur = scanstring(cur);
				} else if (charcode == 35) {
					output.add("#");
					cur++;
				} else if (charcode == 37) {
					cur = scancomment(cur);
				} else if (charcode == 38) {
					output.add("&");
					cur++;
				} else if (charcode == 40) {
					output.add("(");
					cur++;
				} else if (charcode == 41) {
					output.add(")");
					cur++;
				} else if (charcode == 44) {
					output.add(",");
					cur++;
				} else if (charcode == 58) {
					cur = scanrulesym(cur);
				} else if (charcode == 60) {
					cur = scanbacksym(cur);
				} else if (charcode == 61) {
					cur = scanthussym(cur);
				} else if (charcode == 62) {
					output.add(">");
					cur++;
				} else if (charcode == 91) {
					output.add("[");
					cur++;
				} else if (charcode == 93) {
					output.add("]");
					cur++;
				} else if (charcode == 123) {
					output.add("{");
					cur++;
				} else if (charcode == 124) {
					output.add("|");
					cur++;
				} else if (charcode == 125) {
					output.add("}");
					cur++;
				} else if (charcode == 126) {
					output.add("~");
					cur++;
				} else if (idcharp(charcode)) {
					cur = scansymbol(cur);
				} else
					cur++;
			}
			return output;
		}

		private int scanrulesym(int cur) {
			if (input.length() > cur + 1 && input.charAt(cur + 1) == 45) {
				output.add(":-");
				return cur + 2;
			}
			if (input.length() > cur + 1 && input.charAt(cur + 1) == 61) {
				output.add(":=");
				return cur + 2;
			} else {
				output.add(":");
				return cur + 1;
			}
		}

		private int scanbacksym(int cur) {
			if (input.length() > cur + 1 && input.charAt(cur + 1) == 61) {
				if (input.length() > cur + 2 && input.charAt(cur + 2) == 62) {
					output.add("<=>");
					return cur + 3;
				} else {
					output.add("<=");
					return cur + 2;
				}
			} else {
				output.add("<");
				return cur + 1;
			}
		}

		private int scanthussym(int cur) {
			if (input.length() > cur + 1 && input.charAt(cur + 1) == 62) {
				output.add("=>");
				return cur + 2;
			}
			if (input.length() > cur + 2 && input.charAt(cur + 1) == 61 && input.charAt(cur + 2) == 62) {
				output.add("==>");
				return cur + 3;
			}
			output.add("=");
			return cur + 1;
		}

		private int scansymbol(int cur) {
			int n = input.length();
			String exp = "";
			while (cur < n) {
				if (idcharp(input.charAt(cur))) {
					exp = exp + input.charAt(cur);
					cur++;
				} else
					break;
			}
			if (!exp.equals("")) {
				output.add(exp);
			}
			return cur;
		}

		private int scanstring(int cur) {
			String exp = "\"";
			cur++;
			while (cur < input.length()) {
				exp = exp + input.charAt(cur);
				if (input.charAt(cur) == 34) {
					cur++;
					break;
				}
				cur++;
			}
			output.add(exp);
			return cur;
		}

		private int scancomment(int cur) {
			while (cur < input.length() && input.charAt(cur) != 10 && input.charAt(cur) != 13) {
				cur++;
			}
			return cur;
		}

		private boolean idcharp(char charcode) {
			if (charcode == 42) {
				return true;
			}
			if (charcode == 43) {
				return true;
			}
			if (charcode == 45) {
				return true;
			}
			if (charcode == 46) {
				return true;
			}
			if (charcode == 47) {
				return true;
			}
			if (charcode >= 48 && charcode <= 57) {
				return true;
			}
			if (charcode >= 65 && charcode <= 90) {
				return true;
			}
			if (charcode >= 97 && charcode <= 122) {
				return true;
			}
			if (charcode == 95) {
				return true;
			}
			return false;
		}

		// ------------------------------------------------------------------------------

		public Symbol parsedata(List<String> tokens) {
			tokens.add("\u001a");
			current = 0;
			ArrayList<Symbol> exp = new ArrayList<Symbol>();

			while (current < tokens.size() && !tokens.get(current).equals("\u001a")) {
				List<String> lparen = new ArrayList<String>();
				lparen.add("(");
				List<String> rparen = new ArrayList<String>();
				rparen.add(")");
				exp.add(SymbolPool.getList(parsexp(lparen, rparen)));
			}
			return removeInner(SymbolPool.getList(exp));
		}

		private Symbol parse(List<String> tokens) {
			tokens.add("\u001a");
			current = 0;
			List<String> lparen = new ArrayList<String>();
			lparen.add("(");
			List<String> rparen = new ArrayList<String>();
			rparen.add(")");
			List<Symbol> list = parsexp(lparen, rparen);

			return removeInner(SymbolPool.getList(list));
		}

		/*
		 * Reconstruct our symbol such that we remove nested singleton lists
		 * which were used to construct symbol objects more smoothly
		 */
		private Symbol removeInner(Symbol sym) {
			if (sym instanceof SymbolAtom) {
				return sym;
			}
			SymbolList list = (SymbolList) sym;
			if (list.size() == 1 && list.get(0) instanceof SymbolAtom) {
				return list.get(0);
			}

			List<Symbol> stripped = new ArrayList<Symbol>();
			for (int i = 0; i < list.size(); i++) {
				Symbol mod = removeInner(list.get(i));
				stripped.add(mod);
			}
			return SymbolPool.getList(stripped);
		}

		private List<Symbol> parsexp(List<String> lop, List<String> rop) {
			List<Symbol> left = parseprefix(rop);
			while (current < output.size()) {
				if (output.get(current) == "\u001a") {
					break;
				} else if (output.get(current).equals("(")) {
					left = parseatom(left);
				} else if (output.get(current).equals(".")) {
					current++;
					break;
				} else if (precedencep(lop, output.get(current))) {
					break;
				} else {
					left = parseinfix(left, output.get(current), rop);
				}
			}
			return left;
		}

		private List<Symbol> parseprefix(List<String> rop) {
			List<Symbol> left = new ArrayList<Symbol>();
			left.add(SymbolPool.getAtom(output.get(current)));
			current++;
			if (left.get(0).toString().equals("(")) {
				List<String> lparen = new ArrayList<String>();
				lparen.add("(");
				List<String> rparen = new ArrayList<String>();
				rparen.add(")");
				current++;
				return parsexp(lparen, rparen);
			}
			if (left.get(0).toString().equals("#")) {
				List<String> pound = new ArrayList<String>();
				pound.add("#");
				return makeprovable(parsexp(pound, rop));
			}
			if (left.get(0).toString().equals("~")) {
				List<String> tilde = new ArrayList<String>();
				tilde.add("~");
				return makenegation(parsexp(tilde, rop));
			}
			if (left.get(0).toString().equals("{")) {
				return parseclause();
			}
			if (left.get(0).toString().equals("[")) {
				return parselist();
			}
			return left;
		}

		private List<Symbol> parseatom(List<Symbol> left) {
			List<Symbol> exp = parseparenlist();
			exp.add(0, SymbolPool.getList(left));
			return exp;
		}

		private List<Symbol> parseparenlist() {
			List<Symbol> exp = new ArrayList<Symbol>();
			current++;
			if (output.get(current).equals(")")) {
				current++;
				return exp;
			}
			while (current < output.size()) {
				List<String> comma1 = new ArrayList<String>();
				comma1.add(",");
				List<String> comma2 = new ArrayList<String>();
				comma2.add(",");
				exp.add(SymbolPool.getList(parsexp(comma1, comma2)));
				if (output.get(current).equals(")")) {
					current++;
					return exp;
				}
				if (output.get(current).equals(",")) {
					current++;
				} else {
					return exp;
				}
			}
			return exp;
		}

		private List<Symbol> parseclause() {
			List<Symbol> exp = new ArrayList<Symbol>();
			exp.add(SymbolPool.getAtom("clause"));
			if (output.get(current).equals("}")) {
				current++;
				return exp;
			}
			while (current < output.size()) {
				List<String> comma1 = new ArrayList<String>();
				comma1.add(",");
				List<String> comma2 = new ArrayList<String>();
				comma2.add(",");
				exp.add(SymbolPool.getList(parsexp(comma1, comma2)));
				if (output.get(current).equals("}")) {
					current++;
					return exp;
				}
				if (output.get(current).equals(",")) {
					current++;
				} else {
					return exp;
				}
			}
			return exp;
		}

		private List<Symbol> parselist() {
			if (output.get(current).equals("]")) {
				current++;
				return seq("nil");
			}
			List<String> comma1 = new ArrayList<String>();
			comma1.add(",");
			List<String> comma2 = new ArrayList<String>();
			comma2.add(",");
			List<Symbol> head = parsexp(comma1, comma2);
			return seq("cons", head, parselistexp());
		}

		private List<Symbol> parselistexp() {
			if (output.get(current).equals("]")) {
				current++;
				return seq("nil");
			}
			if (output.get(current).equals(",")) {
				current++;
				List<String> comma1 = new ArrayList<String>();
				comma1.add(",");
				List<String> comma2 = new ArrayList<String>();
				comma2.add(",");
				return seq("cons", parsexp(comma1, comma2), parselistexp());
			}
			return seq("nil");
		}

		private List<Symbol> parseinfix(List<Symbol> left, String op, List<String> rop) {
			if (op.equals("!")) {
				return parsecons(left, rop);
			}
			// if (op.equals(":")) {return parsequantifier(left,rop);}
			if (op.equals("&")) {
				return parseand(left, rop);
			}
			if (op.equals("|")) {
				return parseor(left, rop);
			}
			// if (op.equals("<=>")) {return parseequivalence(left,rop);}
			// if (op.equals("=>")) {return parseimplication(left,rop);}
			// if (op.equals("<=")) {return parsereduction(left,rop);}
			if (op.equals(":-")) {
				return parserule(left, rop);
			}
			if (op.equals("==>")) {
				return parsetransition(left, rop);
			}
			return left;
		}

		private List<Symbol> parsecons(List<Symbol> left, List<String> rop) {
			current++;
			List<String> excl1 = new ArrayList<String>();
			excl1.add("!");
			List<Symbol> dum = parsexp(excl1, rop);
			return seq("cons", left, dum);
		}

		private List<Symbol> parseand(List<Symbol> left, List<String> rop) {
			current++;
			List<String> and1 = new ArrayList<String>();
			and1.add("&");
			return makeconjunction(left, parsexp(and1, rop));
		}

		private List<Symbol> parseor(List<Symbol> left, List<String> rop) {
			current++;
			List<String> or1 = new ArrayList<String>();
			or1.add("|");
			return makedisjunction(left, parsexp(or1, rop));
		}

		private List<Symbol> parserule(List<Symbol> left, List<String> rop) {
			current++;
			List<String> rul1 = new ArrayList<String>();
			rul1.add(":-");
			List<Symbol> dum = parsexp(rul1, rop);
			return makerule(left, dum);
		}

		private List<Symbol> parsetransition(List<Symbol> left, List<String> rop) {
			current++;
			List<String> tran1 = new ArrayList<String>();
			tran1.add("==>");
			List<Symbol> dum = parsexp(tran1, rop);
			return maketransition(left, dum);
		}

		private List<Symbol> makeprovable(List<Symbol> p) {
			return seq("provable", p);
		}

		private List<Symbol> makenegation(List<Symbol> p) {
			return seq("not", p);
		}

		private List<Symbol> makeconjunction(List<Symbol> p, List<Symbol> q) {
			if (p.get(0).toString().equals("and")) {
				List<Symbol> conj = Lists.newArrayList(p);
				conj.add(SymbolPool.getList(q));
				return conj;
			}
			return seq("and", p, q);
		}

		private List<Symbol> makedisjunction(List<Symbol> p, List<Symbol> q) {
			if (p.get(0).toString().equals("or")) {
				List<Symbol> disj = Lists.newArrayList(p);
				for (Symbol sym : q) {
					disj.add(sym);
				}
				return disj;
			}
			return seq("or", p, q);
		}

		private List<Symbol> makerule(List<Symbol> head, List<Symbol> body) {
			if (body.size() == 0) {
				return head;
			}
			if (body.get(0).toString().equals("and")) {
				List<Symbol> head2 = seq("rule", head);
				for (int i = 1; i < body.size(); i++) {
					head2.add(body.get(i));
				}
				return head2;
			}
			return seq("rule", head, body);
		}

		private List<Symbol> maketransition(List<Symbol> head, List<Symbol> body) {
			return seq("transition", head, body);
		}
	}

	/**
	 * This class serves to take a Symbol and convert it to a String in HRF format.
	 *
	 * The code was adopted from Michael Genereseth's original implementation in
	 * Javascript. Please forward all comments or complaints to him.
	 */
	public static class Grinder {

		public static String grinddata(SymbolList data) {
			String exp = "";
			int n = data.size();
			for (int i = 0; i < n; i++) {
				exp += grind(data.get(i)) + '\n';
			}
			return exp;
		}

		public static String grindem(SymbolList data) {
			String exp = "";
			int n = data.size();
			for (int i = 0; i < n; i++) {
				exp = exp + grind(data.get(i)) + '\t';
			}
			return exp;
		}

		public static String grind(Symbol p) {
			return grindit(p, "(", ")");
		}

		private static String grindit(Symbol p, String lop, String rop) {
			if ((p instanceof SymbolAtom && p.toString().equals("nil"))
					|| (p instanceof SymbolList && ((SymbolList) p).size() == 0)) {
				return "[]";
			}
			if (p instanceof SymbolAtom) {
				return ((SymbolAtom) p).getValue();
			}
			SymbolList pList = (SymbolList) p;
			if (pList.get(0).toString().equals("cons")) {
				return grindcons(p, lop, rop);
			}
			// if (p.get(0).toString().equals("definition")) {return
			// grinddefinition(p,lop,rop)};
			// if (p.get(0).toString().equals("provable")) {return
			// grindprovable(p,rop)};
			if (pList.get(0).toString().equals("not")) {
				return grindnegation(pList, rop);
			}
			if (pList.get(0).toString().equals("and")) {
				return grindand(pList, lop, rop);
			}
			if (pList.get(0).toString().equals("or")) {
				return grindor(pList, lop, rop);
			}
			// if (p.get(0).toString().equals("equivalence")) {return
			// grindequivalence(p,lop,rop);}
			// if (p.get(0).toString().equals("implication")) {return
			// grindimplication(p,lop,rop);}
			// if (p.get(0).toString().equals("reduction")) {return
			// grindreduction(p,lop,rop);}
			if (pList.get(0).toString().equals("rule")) {
				return grindrule(pList, lop, rop);
			}
			if (pList.get(0).toString().equals("transition")) {
				return grindtransition(pList, lop, rop);
			}
			// if (p.get(0).toString().equals("clause")) {return
			// grindclause(p);}
			return grindatom(pList);
		}

		private static String grindcons(Symbol p, String lop, String rop) {
			if (listp(p)) {
				return grindlist((SymbolList) p);
			}
			String exp = "";
			boolean parens = parenp(lop, "!", rop);
			if (parens) {
				lop = "(";
				rop = ")";
				exp = "(";
			}
			SymbolList pList = (SymbolList) p;
			exp += grindit(pList.get(1), lop, "!") + "!" + grindit(pList.get(2), "!", rop);
			if (parens) {
				exp += ")";
			}
			return exp;
		}

		private static String grindlist(SymbolList l) {
			String out = "[" + grind(l.get(1));
			Symbol p = l.get(2);
			while (p instanceof SymbolList && ((SymbolList) p).get(0).toString().equals("cons")) {
				out = out + "," + grind(((SymbolList) p).get(1));
				p = ((SymbolList) p).get(2);
			}
			if (!p.toString().equals("nil") && p instanceof SymbolList && ((SymbolList) p).size() > 0) {
				out += '|' + grind(p);
			}
			out += ']';
			return out;
		}

		private static String grindatom(SymbolList p) {
			int n = p.size();
			String exp = p.get(0).toString() + '(';
			if (n > 1) {
				exp += grind(p.get(1));
			}
			for (int i = 2; i < n; i++) {
				exp += ',' + grind(p.get(i));
			}
			exp += ')';
			return exp;
		}

		private static String grindnegation(SymbolList p, String rop) {
			return '~' + grindit(p.get(1), "~", rop);
		}

		private static String grindand(SymbolList p, String lop, String rop) {
			if (p.size() == 1) {
				return "";
			}
			if (p.size() == 2) {
				return grindit(p.get(1), lop, rop);
			}
			String exp = grindleft(lop, "&", rop) + grindit(p.get(1), lop, "&");
			for (int i = 2; i < p.size() - 1; i++) {
				exp = exp + " & " + grindit(p.get(i), "&", "&");
			}
			exp += " & " + grindit(p.get(p.size() - 1), "&", rop) + grindright(lop, "&", rop);
			return exp;
		}

		private static String grindor(SymbolList p, String lop, String rop) {
			String exp;
			if (p.size() == 1) {
				return "";
			}
			if (p.size() == 2) {
				return grindit(p.get(1), lop, rop);
			}
			exp = grindleft(lop, "|", rop) + grindit(p.get(1), lop, "|");
			for (int i = 2; i < p.size() - 1; i++) {
				exp += " | " + grindit(p.get(i), "|", "|");
			}
			exp += " | " + grindit(p.get(p.size() - 1), "|", rop) + grindright(lop, "|", rop);
			return exp;
		}

		private static String grindrule(SymbolList p, String lop, String rop) {
			String exp = grind(p.get(1)) + " :- ";
			if (p.size() == 2) {
				exp += "true";
			} else if (p.size() == 3) {
				exp += grindit(p.get(2), ":-", rop);
			} else {
				exp += grindit(p.get(2), lop, "&");
				for (int i = 3; i < p.size() - 1; i++) {
					exp = exp + " & " + grindit(p.get(i), "&", "&");
				}
				exp += " & " + grindit(p.get(p.size() - 1), "&", rop);
			}
			return exp;
		}

		private static String grindtransition(SymbolList p, String lop, String rop) {
			String exp = "";
			boolean parens = parenp(lop, "==>", rop);
			if (parens) {
				lop = "(";
				rop = ")";
				exp = "(";
			}
			exp += grindit(p.get(1), lop, "==>") + " ==> " + grindit(p.get(2), "==>", rop);
			if (parens) {
				exp += ")";
			}
			return exp;
		}

		private static String grindleft(String lop, String op, String rop) {
			List<String> lopList = new ArrayList<String>();
			lopList.add(lop);
			List<String> ropList = new ArrayList<String>();
			ropList.add(rop);
			if (precedencep(lopList, op) || precedencep(ropList, op)) {
				return "(";
			}
			return "";
		}

		private static String grindright(String lop, String op, String rop) {
			List<String> lopList = new ArrayList<String>();
			lopList.add(lop);
			List<String> ropList = new ArrayList<String>();
			ropList.add(rop);
			if (precedencep(lopList, op) || precedencep(ropList, op)) {
				return ")";
			}
			return "";
		}

		private static boolean listp(Symbol x) {
			if (x.toString().equals("nil") || (x instanceof SymbolList && ((SymbolList) x).size() == 0)) {
				return true;
			}
			if (x instanceof SymbolAtom) {
				return false;
			}
			SymbolList xList = (SymbolList) x;
			if (xList.get(0).toString().equals("cons")) {
				return listp(xList.get(2));
			}
			return false;
		}
	}
}
