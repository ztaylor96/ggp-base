package org.ggp.base.util.gdl.grammar;

import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("serial")
public final class GdlRelation extends GdlSentence
{

	private final ImmutableList<GdlTerm> body;
	private transient Boolean ground;
	private final GdlConstant name;

	GdlRelation(GdlConstant name, ImmutableList<GdlTerm> body)
	{
		this.name = name;
		this.body = body;
		ground = null;
	}

	@Override
	public int arity()
	{
		return body.size();
	}

	private boolean computeGround()
	{
		for (GdlTerm term : body)
		{
			if (!term.isGround())
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public GdlTerm get(int index)
	{
		return body.get(index);
	}

	@Override
	public GdlConstant getName()
	{
		return name;
	}

	@Override
	public boolean isGround()
	{
		if (ground == null)
		{
			ground = computeGround();
		}

		return ground;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		switch (GdlPool.format) {
			case HRF:
				sb.append(name + "(");
				for (int i=0; i<body.size(); i++) {
					sb.append(body.get(i));
					if (i < body.size()-1) {
						sb.append(", ");
					}
				}
				sb.append(")");
				break;

			case KIF:
				sb.append("( " + name + " ");
				for (GdlTerm term : body)
				{
					sb.append(term + " ");
				}
				sb.append(")");
				break;
		}

		return sb.toString();
	}

	@Override
	public GdlTerm toTerm()
	{
		return GdlPool.getFunction(name, body);
	}

	@Override
	public List<GdlTerm> getBody()
	{
		return body;
	}

}
