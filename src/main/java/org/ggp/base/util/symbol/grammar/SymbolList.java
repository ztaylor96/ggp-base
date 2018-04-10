package org.ggp.base.util.symbol.grammar;

import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.symbol.factory.HRFSymbolFactory;

public final class SymbolList extends Symbol
{

	private final List<Symbol> contents;

	SymbolList(List<Symbol> contents)
	{
		this.contents = contents;
	}

	public Symbol get(int index)
	{
		return contents.get(index);
	}

	public int size()
	{
		return contents.size();
	}

	@Override
	public String toString() {
		switch (GdlPool.format) {

			case HRF:
				return HRFSymbolFactory.Grinder.grind(this);
			case KIF:
				StringBuilder sb = new StringBuilder();

				sb.append("( ");
				for (Symbol symbol : contents)
				{
					sb.append(symbol.toString() + " ");
				}
				sb.append(")");

				return sb.toString();

			default:
				return ((Object) this).toString();

		}
	}

}
