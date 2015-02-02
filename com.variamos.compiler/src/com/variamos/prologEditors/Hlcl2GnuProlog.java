package com.variamos.prologEditors;

import java.util.List;
import java.util.Set;

import com.variamos.compiler.solverSymbols.GNUPrologSymbols;
import com.variamos.hlcl.BooleanOperation;
import com.variamos.hlcl.HlclProgram;
import com.variamos.hlcl.HlclUtil;
import com.variamos.hlcl.Identifier;
import com.variamos.hlcl.IntervalDomain;
import com.variamos.hlcl.RangeDomain;

public class Hlcl2GnuProlog extends Hlcl2Prolog implements
		GNUPrologSymbols {

	@Override
	protected void writeFooter(StringBuilder out) {
		out.append(END);

	}

	@Override
	protected void transformBooleanOperation(BooleanOperation e,
			StringBuilder out) {
		out.append(OPEN_PARENTHESIS);
		transformBooleanExpression(e.getLeft(), out);

		out.append(SPACE);
		switch (e.getOperator()) {
		case And:
			out.append(AND);
			break;
		case DoubleImplies:
			out.append(EQUIVALENT);
			break;
		case Implies:
			out.append(IMPLIES);
			break;
		case Or:
			out.append(OR);
			break;
		}
		out.append(SPACE);
		transformBooleanExpression(e.getRight(), out);
		out.append(CLOSE_PARENHESIS);
	}

	@Override
	protected void  writeHeaderWithDefinedDomains(HlclProgram program, List<String>domainList,StringBuilder out){
		Set<Identifier> ids = HlclUtil.getUsedIdentifiers(program);
		out.append(HEADER);
		out.append(makeVariables(ids));
		out.append("\n");
		for(String domain: domainList){
			out.append(domain);
		}
		out.append("\n");
	}
	
	
	private StringBuilder makeVariables(Set<Identifier> ids) {
		// Se contruye la lista de caracter�sticas y de dominios
		StringBuilder dommainAndVariables = new StringBuilder("L=[");
		StringBuilder variablesList = new StringBuilder();
		String id = "";
		for (Identifier identifier : ids) {
			id = identifier.getId();
			variablesList.append(id);
			variablesList.append(COMMA);

			
		}
		variablesList.append("],");
		dommainAndVariables.append(variablesList.toString().replace(
				",]", CLOSE_BRACKET));
		dommainAndVariables.append(LF);
	
		return dommainAndVariables;
	}
	
	
	@Override
	protected void writeHeader(HlclProgram program, StringBuilder out) {
		Set<Identifier> ids = HlclUtil.getUsedIdentifiers(program);

		out.append(HEADER);
		out.append(makeDomainsAndVariables(ids));
	}

	private StringBuilder makeDomainsAndVariables(Set<Identifier> ids) {
		// Se contruye la lista de caracter�sticas y de dominios
		StringBuilder dommainAndVariables = new StringBuilder("L=[");
		StringBuilder variablesList = new StringBuilder();
		StringBuilder domainString = new StringBuilder();
		String id = "";
		for (Identifier identifier : ids) {
			id = identifier.getId();
			variablesList.append(id);
			variablesList.append(COMMA);

			if (identifier.getDomain() instanceof RangeDomain) {
				// Sample fd_domain(Var_Car, 0, 1),
				Integer lowerValue = ((RangeDomain) identifier.getDomain())
						.getLowerValue();
				Integer upperValue = ((RangeDomain) identifier.getDomain())
						.getUpperValue();
			
				domainString.append(FD_DOMAIN);
				domainString.append(OPEN_PARENTHESIS);

				domainString.append(id);
				domainString.append(COMMA);
				domainString.append(lowerValue);
				domainString.append(COMMA);
				domainString.append(upperValue);
				domainString.append(CLOSE_PARENHESIS);

			} else if (identifier.getDomain() instanceof IntervalDomain) {

				// sAMPLE fd_domain(WidthResolution, [0, 800, 1024,
				// 1366]),
				List<Integer> domains = ((IntervalDomain) identifier
						.getDomain()).getRangeValues();


				domainString.append(FD_DOMAIN);
				domainString.append(OPEN_PARENTHESIS);
				domainString.append(id);
				domainString.append(COMMA);
				domainString.append(OPEN_BRACKET);

				for (int i = 0; i < domains.size(); i++) {
					Integer domainValue = domains.get(i);
					domainString.append(Integer.toString(domainValue));
					if (i < domains.size() - 1) {
						domainString.append(COMMA);

					} else {
						domainString.append(CLOSE_BRACKET);
						domainString.append(CLOSE_PARENHESIS);

					}

				}

			}
			domainString.append(COMMA);
		}
		variablesList.append("],");
		domainString.append(LF);

		dommainAndVariables.append(variablesList.toString().replace(
				",]", CLOSE_BRACKET));
		dommainAndVariables.append(LF);
		// add domain string
		dommainAndVariables.append(domainString);
		
		return dommainAndVariables;
	}
}