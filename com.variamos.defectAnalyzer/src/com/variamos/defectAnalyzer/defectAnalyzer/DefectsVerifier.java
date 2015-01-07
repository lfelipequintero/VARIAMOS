package com.variamos.defectAnalyzer.defectAnalyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cfm.hlcl.BooleanExpression;
import com.cfm.hlcl.Domain;
import com.cfm.hlcl.HlclProgram;
import com.cfm.hlcl.HlclUtil;
import com.cfm.hlcl.Identifier;
import com.cfm.hlcl.LiteralBooleanExpression;
import com.cfm.jgprolog.core.PrologException;
import com.cfm.productline.solver.Configuration;
import com.cfm.productline.solver.ConfigurationOptions;
import com.variamos.core.enums.SolverEditorType;
import com.variamos.core.exceptions.FunctionalException;
import com.variamos.core.exceptions.TechnicalException;
import com.variamos.defectAnalyzer.constants.TransformerConstants;
import com.variamos.defectAnalyzer.dto.VMAnalyzerInDTO;
import com.variamos.defectAnalyzer.dto.VMVerifierOutDTO;
import com.variamos.defectAnalyzer.model.DefectAnalyzerDomain;
import com.variamos.defectAnalyzer.model.Dependency;
import com.variamos.defectAnalyzer.model.VariabilityElementDefAna;
import com.variamos.defectAnalyzer.model.defects.DeadElement;
import com.variamos.defectAnalyzer.model.defects.Defect;
import com.variamos.defectAnalyzer.model.defects.FalseOptionalElement;
import com.variamos.defectAnalyzer.model.defects.FalseProductLine;
import com.variamos.defectAnalyzer.model.defects.NonAttainableDomain;
import com.variamos.defectAnalyzer.model.defects.Redundancy;
import com.variamos.defectAnalyzer.model.defects.VoidModel;
import com.variamos.defectAnalyzer.util.ConstraintRepresentationUtil;
import com.variamos.defectAnalyzer.util.SolverOperationsUtil;
import com.variamos.defectAnalyzer.util.VerifierUtilExpression;

public class DefectsVerifier extends VariabilityModelAnalyzer implements
		IntDefectsVerifier {

	private VMAnalyzerInDTO analyzerInDTO;

	// Variables usadas para almacenar informaci�n que es �til cuando se hacen
	// otras operaciones de verificaci�n
	private Map<Identifier, Set<Integer>> attainableDomainsByVariabilityElementMap;
	private SolverOperationsUtil solver;

	public DefectsVerifier(VMAnalyzerInDTO analyzerInDTO) {
		super(analyzerInDTO);
		this.analyzerInDTO = analyzerInDTO;
		attainableDomainsByVariabilityElementMap = new HashMap<Identifier, Set<Integer>>();
		solver = new SolverOperationsUtil(analyzerInDTO.getSolverEditorType());

	}

	@Override
	public Defect isVoid(HlclProgram model) {

		boolean isVoid = !solver.isSatisfiable(model);
		if (isVoid) {
			return new VoidModel(Boolean.TRUE);
		} else {
			return new VoidModel(Boolean.FALSE);
		}

	}

	@Override
	public Defect isFalsePL(HlclProgram model) {
		boolean isFPL = solver.isFalseProductLine(model);
		if (isFPL) {
			return new FalseProductLine(Boolean.TRUE);
		} else {
			return new FalseProductLine(Boolean.FALSE);
		}
	}

	/**
	 * Identifica dead features, intentando para cada varible asignar alg�n
	 * valor posible para su dominio. Cuando esto es posible se sabe entonces
	 * que la variable no es dead y se contin�a con la siguiente variable
	 * 
	 * @return
	 * @throws FunctionalException
	 * 
	 */
	@Deprecated
	public List<Defect> identifyDeadFeatures(
			Map<String, VariabilityElementDefAna> elementsListToVerify)
			throws FunctionalException {

		List<Integer> definedDomainValues = null;
		List<Defect> deadElementsList = new ArrayList<Defect>();
		List<BooleanExpression> variabilityModelConstraintRepresentation = new ArrayList<BooleanExpression>();
		BooleanExpression constraintToAdd = null;
		BooleanExpression constraintToIdentifyDeadFeature = null;
		boolean isDead = Boolean.TRUE;
		for (VariabilityElementDefAna element : elementsListToVerify.values()) {

			DefectAnalyzerDomain domain = element.getDomain();
			// Se obtienen los valores parametrizados para esta variable
			definedDomainValues = domain.getDomainValues();
			isDead = Boolean.TRUE;
			boolean isSatisfiable = Boolean.FALSE;
			// Se busca para cada variable los valores de dominio y se analiza
			for (Integer definedDomainValue : definedDomainValues) {
				// Ejm F1 #= 1. Se excluye el 0
				isSatisfiable = Boolean.FALSE;

				if (definedDomainValue > TransformerConstants.NON_SELECTED_VALUE) {

					variabilityModelConstraintRepresentation.clear();
					// Se adiciona las restricciones que tiene el modelo de
					// variabilidad m�s las restricciones fijas
					variabilityModelConstraintRepresentation
							.addAll(ConstraintRepresentationUtil
									.dependencyToExpressionList(analyzerInDTO
											.getVariabilityModel()
											.getDependencies(), analyzerInDTO
											.getVariabilityModel()
											.getFixedDependencies()));

					constraintToAdd = VerifierUtilExpression
							.verifyAssignValueToVariabilityElementExpression(
									element, definedDomainValue);

					// Se adiciona la restricci�n al conjunto de restricciones
					// que representa el modelo de variabilidad
					variabilityModelConstraintRepresentation.add(0,
							constraintToAdd);

					// Se convierte el conjunto de restricciones a un archivo en
					// prolog que se guarda en la ruta temporal
					ConstraintRepresentationUtil
							.savePrologRepresentationProgram(prologTempPath,
									variabilityModelConstraintRepresentation,
									prologEditorType);

					// Se evalua si el modelo de restricciones guardado en la
					// ruta temporal es satisfacible
					isSatisfiable = solver.isSatisfiable(prologTempPath);

					if (isSatisfiable) {
						// Si es satisfacible la feature no es dead y se pasa a
						// la siguiente feature
						isDead = Boolean.FALSE;

						// Se adiciona ese valor como valor permitido para no
						// verificarlo en los notAttainableDomains
						if (attainableDomainsByVariabilityElementMap
								.containsKey(element)) {
							// Se adiciona el valor a la lista de dominios
							// permitidos
							attainableDomainsByVariabilityElementMap.get(
									element).add(definedDomainValue);
						} else {
							// Se adiciona el variabilityElement al mapa y se
							// adicionan el valor de dominio. Se crea un nuevo
							// objeto para evitar problemas de referencia entre
							// objetos
							Set<Integer> attainableDomainValuesList = new HashSet<Integer>();
							attainableDomainValuesList.add(definedDomainValue);

						}
						break;
					} else {
						constraintToIdentifyDeadFeature = constraintToAdd;
					}
				}

			}

			if (isDead) {
				// Se crea un nuevo defecto
				/*
				 * adElement deadElement = new DeadElement(element,
				 * constraintToIdentifyDeadFeature);
				 * deadElementsList.add(deadElement);
				 * 
				 * System.out.println("dead feature" + element.getName());
				 */
			}

		}
		return deadElementsList;
	}

	/**
	 * Una false optional es una feature que debe estar presente en todas las
	 * configuraciones posibles de la l�nea de productos aunque es declarada
	 * como opcional
	 * 
	 * @return
	 * @throws FunctionalException
	 * 
	 */
	@Deprecated
	public List<Defect> identifyFalseOptionalFeatures(
			Map<String, VariabilityElementDefAna> optionalElementsMap)
			throws FunctionalException {

		List<Defect> falseOptionalElementsList = new ArrayList<Defect>();
		List<BooleanExpression> variabilityModelConstraintRepresentation = new ArrayList<BooleanExpression>();

		BooleanExpression constraintToIdentifyFalseOptionalFeature = null;

		boolean isSatisfiable = Boolean.TRUE;
		if (optionalElementsMap != null) {
			for (VariabilityElementDefAna element : optionalElementsMap
					.values()) {

				variabilityModelConstraintRepresentation.clear();
				// Ejm F1 #= 0.
				// Se adiciona las restricciones que tiene el modelo de
				// variabilidad inicial
				variabilityModelConstraintRepresentation
						.addAll(ConstraintRepresentationUtil
								.dependencyToExpressionList(analyzerInDTO
										.getVariabilityModel()
										.getDependencies(), analyzerInDTO
										.getVariabilityModel()
										.getFixedDependencies()));

				constraintToIdentifyFalseOptionalFeature = VerifierUtilExpression
						.verifyFalseOptionalExpression(element);

				// Se adiciona la restricci�n al conjunto de restricciones
				// que representa el modelo de variabilidad
				variabilityModelConstraintRepresentation.add(0,
						constraintToIdentifyFalseOptionalFeature);

				// Se convierte el conjunto de restricciones a un archivo en
				// prolog que se guarda en la ruta temporal
				ConstraintRepresentationUtil.savePrologRepresentationProgram(
						prologTempPath,
						variabilityModelConstraintRepresentation,
						prologEditorType);

				// Se evalua si el modelo de restricciones guardado en la
				// ruta temporal es resoluble
				isSatisfiable = solver.isSatisfiable(prologTempPath);

				// Se evalua si este nuevo modelo es satisfacible
				if (!isSatisfiable) {
					// Se crea un nuevo defecto
					/*
					 * FalseOptionalElement falseOptionalElement = new
					 * FalseOptionalElement( element,
					 * constraintToIdentifyFalseOptionalFeature);
					 * falseOptionalElementsList.add(falseOptionalElement);
					 * System.out.println("false optional" + element.getName());
					 */

				}
			}
		} else {
			throw new FunctionalException("Optional elements were not received");
		}
		return falseOptionalElementsList;
	}

	/**
	 * Identifica todas las redudancias de un modelo de variabilidad expresado
	 * en SPLOT. Funciona para este modelo pues fue para ese transformador que
	 * se almacenaron las redundancias
	 * 
	 * @throws FunctionalException
	 */
	public List<Defect> identifyAllRedundanciesSPLOTModels(
			Map<Long, Dependency> dependenciesMap) throws FunctionalException {

		List<Defect> redundancies = new ArrayList<Defect>();

		for (Dependency dependency : dependenciesMap.values()) {
			// Se verifica cada dependencia para ver si es redundante, siempre y
			// cuando tenga la negaci�n de la restricci�n.
			if (dependency.getNegationExpression() != null) {
				Defect defect = identifyRedundancy(null,
						dependency.getNegationExpression(), dependency);
				if (defect != null) {
					redundancies.add(defect);
				}
			} else {
				// No se puede evaluar la redundacias, el modelo debe ser
				// resoluble
				throw new FunctionalException(
						"No se puede verificar la redundancia, la dependencia no tiene la expresi�n de negaci�n");
			}
		}
		return redundancies;
	}

	public Defect identifyRedundancy(String instructionToCheckRedundacy,
			BooleanExpression expressionInstructionToCheckRedundancy,
			Dependency redundantDependency) throws FunctionalException {

		Collection<BooleanExpression> completeVariabilityModelConstraintRepresentation = new HashSet<BooleanExpression>();
		Collection<BooleanExpression> variabilityModelConstraintRepresentationWithoutRedundacy = new HashSet<BooleanExpression>();
		Map<Long, Dependency> dependenciesModel = new HashMap<Long, Dependency>();
		// Copia del mapa con todas las dependencias
		dependenciesModel.putAll(analyzerInDTO.getVariabilityModel()
				.getDependencies());

		boolean isSatisfiable = Boolean.FALSE;

		// 1.Se verifica si el modelo con la redundancia es resoluble
		completeVariabilityModelConstraintRepresentation = ConstraintRepresentationUtil
				.dependencyToExpressionList(analyzerInDTO.getVariabilityModel()
						.getDependencies(), analyzerInDTO.getVariabilityModel()
						.getFixedDependencies());

		// Se convierte el conjunto de restricciones a un archivo en
		// prolog que se guarda en la ruta temporal
		ConstraintRepresentationUtil.savePrologRepresentationProgram(
				prologTempPath,
				completeVariabilityModelConstraintRepresentation,
				prologEditorType);

		// Se evalua si el modelo de restricciones guardado en la
		// ruta temporal es resoluble
		isSatisfiable = solver.isSatisfiable(prologTempPath);

		if (isSatisfiable) {

			// 2. Se verifica si el modelo sin la caracter�stica redundante
			// ofrece es resoluble
			if (dependenciesModel.containsKey(redundantDependency
					.getRelationShipNumber())) {
				dependenciesModel.remove(redundantDependency
						.getRelationShipNumber());
			} else {
				throw new FunctionalException(
						"Variability model does not have the redundant dependency that you want to check");
			}
			variabilityModelConstraintRepresentationWithoutRedundacy = ConstraintRepresentationUtil
					.dependencyToExpressionList(dependenciesModel,
							analyzerInDTO.getVariabilityModel()
									.getFixedDependencies());
			// Se convierte el conjunto de restricciones a un archivo en
			// prolog que se guarda en la ruta temporal
			ConstraintRepresentationUtil.savePrologRepresentationProgram(
					prologTempPath,
					variabilityModelConstraintRepresentationWithoutRedundacy,
					prologEditorType);

			// Se evalua si el modelo de restricciones guardado en la
			// ruta temporal es resoluble
			isSatisfiable = solver.isSatisfiable(prologTempPath);

			// Se evalua si este nuevo modelo es satisfacible
			if (!isSatisfiable) {
				// La relaci�n no es redundante, pq se requiere para que el
				// modelo funcione
				return null;
			} else {
				// Si el nuevo modelo es resoluble entonces se adicionan las
				// instrucciones que corresponen a la negaci�n de la restricci�n
				// que se cree redundante
				BooleanExpression redundancyNegationExpression = null;
				if (instructionToCheckRedundacy != null
						&& expressionInstructionToCheckRedundancy == null) {
					redundancyNegationExpression = new LiteralBooleanExpression(
							instructionToCheckRedundacy);
				} else if (expressionInstructionToCheckRedundancy != null) {
					redundancyNegationExpression = expressionInstructionToCheckRedundancy;
				}
				variabilityModelConstraintRepresentationWithoutRedundacy
						.add(redundancyNegationExpression);

				// Se convierte el conjunto de restricciones sin la instrucci�n
				// redundante y con las instrucciones que niegan la redundancia
				// a un archivo en
				// prolog que se guarda en la ruta temporal. Se quita la
				// instrucci�n que se crea redundante pq si es realmente
				// redundante otras cosas en el modelo estan haciendo que ese
				// valor sera verdadero
				ConstraintRepresentationUtil
						.savePrologRepresentationProgram(
								prologTempPath,
								variabilityModelConstraintRepresentationWithoutRedundacy,
								prologEditorType);

				// Se evalua si el modelo de restricciones guardado en la
				// ruta temporal es resoluble
				isSatisfiable = solver.isSatisfiable(prologTempPath);

				if (!isSatisfiable) {
					// La restricci�n si es redundante pq el modelo se volvi�
					// irresoluble
					Defect redundancy = new Redundancy(redundantDependency,
							redundancyNegationExpression);
					return redundancy;

				}
			}
		} else {
			// No se puede evaluar la redundacias, el modelo debe ser resoluble
			throw new FunctionalException(
					"Variability model must be not void to check redundancies");
		}

		return null;
	}

	/**
	 * @param elementsMapToVerify
	 * @return
	 * @throws FunctionalException
	 */
	private List<Defect> identifyNonAttainableDomains(
			Map<String, VariabilityElementDefAna> elementsMapToVerify)
			throws FunctionalException {

		List<Defect> notAttainableDomains = new ArrayList<Defect>();
		List<Integer> definedDomainValues = null;
		Set<Integer> attainableDomains = null;
		List<BooleanExpression> variabilityModelConstraintRepresentation = new ArrayList<BooleanExpression>();

		BooleanExpression constraintToIdentifyNonAttainableDomain = null;

		for (VariabilityElementDefAna element : elementsMapToVerify.values()) {
			DefectAnalyzerDomain domain = element.getDomain();
			// Se obtienen los valores parametrizados para esta variable
			definedDomainValues = domain.getDomainValues();

			// Se busca para cada variable los valores de dominio y se analiza
			// siempre y cuando no este en la lista de valores permitidos
			for (Integer valueToTest : definedDomainValues) {

				attainableDomains = attainableDomainsByVariabilityElementMap
						.get(element);
				attainableDomains = null;
				variabilityModelConstraintRepresentation.clear();
				if (attainableDomains == null
						|| (attainableDomains != null && !attainableDomains
								.contains(valueToTest))) {

					// Se adiciona las restricciones que tiene el modelo de
					// variabilidad inicial
					variabilityModelConstraintRepresentation
							.addAll(ConstraintRepresentationUtil
									.dependencyToExpressionList(analyzerInDTO
											.getVariabilityModel()
											.getDependencies(), analyzerInDTO
											.getVariabilityModel()
											.getFixedDependencies()));

					// Constraint para verificar el dominio no alcanzable
					// variabilityElement= valueToTest
					constraintToIdentifyNonAttainableDomain = VerifierUtilExpression
							.verifyAssignValueToVariabilityElementExpression(
									element, valueToTest);

					// Se adiciona la restricci�n al conjunto de restricciones
					// que representa el modelo de variabilidad
					variabilityModelConstraintRepresentation.add(0,
							constraintToIdentifyNonAttainableDomain);

					// Se convierte el conjunto de restricciones a un archivo en
					// prolog que se guarda en la ruta temporal
					ConstraintRepresentationUtil
							.savePrologRepresentationProgram(prologTempPath,
									variabilityModelConstraintRepresentation,
									prologEditorType);

					List<List<Integer>> configuredValuesList = new ArrayList<List<Integer>>();

					// Se evalua si este nuevo modelo es satisfacible y se
					// obtienen los valores de la configuraci�n
					// FIXME
					/*
					 * configuredValuesList = SolverOperationsUtil
					 * .getSelectedVariablesByConfigurations( prologTempPath, 1,
					 * prologEditorType);
					 */

					// Si se obtienen valores esto quiere decir q es
					// satisfacible
					if (configuredValuesList != null
							&& !configuredValuesList.isEmpty()) {

						List<Integer> configuredValues = configuredValuesList
								.get(0);
						HlclProgram expressionProgram = ConstraintRepresentationUtil
								.expressionToHlclProgram(variabilityModelConstraintRepresentation);
						Set<Identifier> constraintProgramIdentifiersCollection = HlclUtil
								.getUsedIdentifiers(expressionProgram);
						// Los valores identificados no son non attainable
						// domains, se actualizan los valores en el mapa
						updateEvaluatedDomainsMap(configuredValues,
								elementsMapToVerify,
								constraintProgramIdentifiersCollection);
					} else {
						// Se crea un nuevo defecto
						NonAttainableDomain defect = new NonAttainableDomain(
								element, valueToTest);
						notAttainableDomains.add(defect);
						System.out.println("NonattainableDomain"
								+ element.getName() + ": " + valueToTest);
					}
				}

			}

		}
		return notAttainableDomains;

	}

	private void updateEvaluatedDomainsMap(List<Integer> configuredValues,
			Map<String, VariabilityElementDefAna> variabilityElementDefAnas,
			Set<Identifier> constraintProgramIdentifiersCollection) {

		VariabilityElementDefAna element = null;
		int i = 0;
		for (Identifier identifier : constraintProgramIdentifiersCollection) {

			Integer value = configuredValues.get(i);

			// Se busca en la lista de variabilityElements
			if (variabilityElementDefAnas.containsKey(identifier.getId())) {
				// Se obtiene el variabilityElement
				element = variabilityElementDefAnas.get(identifier.getId());
				// Se verifica si el mapa tiene el variability element
				if (attainableDomainsByVariabilityElementMap
						.containsKey(element)) {
					// Si existe se adiciona el valor de la variable al set de
					// dominios permitidos
					attainableDomainsByVariabilityElementMap.get(element).add(
							value);
				} else {
					// Si el mapa no contiene el variabilityElement se adiciona
					// a
					// mapa y se adiciona el valor de dominio
					// objetos
					Set<Integer> attainableDomainValuesSet = new HashSet<Integer>();
					attainableDomainValuesSet.add(value);
					// FIXME
					/*
					 * attainableDomainsByVariabilityElementMap.put(element,
					 * attainableDomainValuesSet);
					 */
				}

			} else {
				// Quiere decir que no se encuentran dentro de los elementos
				// para los que se debe verificar el valor
			}
			i++;
		}

	}

	/**
	 * Contrala las invocaciones a la clase que verifica los defectos del modelo
	 * 
	 * @param defectAnalyzerInDTO
	 * @return VerifierOutDTO
	 * @throws FunctionalException
	 * @throws PrologException
	 */
	public VMVerifierOutDTO verifierOfDefects(boolean verifyDeadFeatures,
			boolean verifyFalseOptionalElement, boolean verifyFalseProductLine,
			boolean verifyNonAttainableDomains, boolean verifyRedundancies)
			throws FunctionalException {

		VMVerifierOutDTO outDTO = new VMVerifierOutDTO();

		// Siempre se verifica si es void, pq por como est� pensado la
		// soluci�n
		// es obligatorio hacer esto
		// Defect isVoid = isVoid();
		// outDTO.setVoidModel(isVoid);
		Defect isVoid = null;// FIXME

		if (isVoid == null) {

			outDTO.setVoidModel(isVoid);
			if (verifyFalseProductLine) {
				// Defect isFalsePLM = isFalsePLM();
				// outDTO.setFalseProductLineModel(isFalsePLM);
			}
			if (verifyDeadFeatures) {
				// Dead features
				List<Defect> deadFeatures = identifyDeadFeatures(analyzerInDTO
						.getVariabilityModel().getElements());
				outDTO.setDeadFeaturesList(deadFeatures);
			}
			if (verifyFalseOptionalElement) {
				// False optional elements
				Map<String, VariabilityElementDefAna> optionalElements = analyzerInDTO
						.getVariabilityModel().getOptionalVariabilityElements();
				if (optionalElements != null && !optionalElements.isEmpty()) {
					// Si esta vac�o entonces no se verifica ninguno
					List<Defect> falseOptionalFeatures = identifyFalseOptionalFeatures(optionalElements);
					outDTO.setFalseOptionalFeaturesList(falseOptionalFeatures);
				}

			}
			if (verifyNonAttainableDomains) {
				List<Defect> nonAttainableDomainsList = identifyNonAttainableDomains(analyzerInDTO
						.getVariabilityModel().getElements());
				outDTO.setDomainNotAttainableList(nonAttainableDomainsList);
			}

			if (verifyRedundancies) {
				List<Defect> redundancies = identifyAllRedundanciesSPLOTModels(analyzerInDTO
						.getVariabilityModel()
						.getInclusionExclusionDependencies());
				outDTO.setRedundanciesList(redundancies);
			}

		} else {
			System.out.println("Void model");
		}

		return outDTO;

	}

	public static void printFoundDefects(VMVerifierOutDTO outDTO) {
		// 3. PRINT RESULTS
		System.out.println("VOID MODEL: " + outDTO.isVoidModel());
		System.out.println("FALSE PRODUCT LINE: "
				+ outDTO.isFalseProductLineModel());

		if (outDTO.getDeadFeaturesList() != null) {
			for (Defect deadElement : outDTO.getDeadFeaturesList()) {
				System.out.println("DEAD FEATURE " + deadElement.getId());
			}
		}

		if (outDTO.getFalseOptionalFeaturesList() != null) {
			for (Defect falseOptionalFeature : outDTO
					.getFalseOptionalFeaturesList()) {
				System.out.println("FALSE OPTIONAL FEATURE "
						+ falseOptionalFeature.getId());
			}
		}
		if (outDTO.getDomainNotAttainableList() != null) {
			for (Defect nonAttainableDomain : outDTO
					.getDomainNotAttainableList()) {

				Integer nonAttainableValue = ((NonAttainableDomain) nonAttainableDomain)
						.getNotAttainableDomain();
				System.out.println("NON ATTAINABLE DOMAIN "
						+ nonAttainableDomain.getId() + " VALUE : "
						+ nonAttainableValue);
			}
		}
	}

	/**
	 * @return the prologEditorType
	 */
	public SolverEditorType getPrologEditorType() {
		return prologEditorType;
	}

	/**
	 * @param prologEditorType
	 *            the prologEditorType to set
	 */
	public void setPrologEditorType(SolverEditorType prologEditorType) {
		this.prologEditorType = prologEditorType;
	}

	@Override
	public Defect isDeadElement(HlclProgram model, Identifier identifier) {

		List<Integer> definedDomainValues = null;
		List<BooleanExpression> variabilityModelConstraintRepresentation = new ArrayList<BooleanExpression>();
		BooleanExpression constraintToAdd = null;
		BooleanExpression verificationExpression = null;
		boolean isDead = Boolean.TRUE;
		Domain domain = identifier.getDomain();
		// Se obtienen los valores parametrizados para esta variable
		definedDomainValues = domain.getPossibleValues();
		boolean isSatisfiable = Boolean.FALSE;
		// Se busca para cada variable los valores de dominio y se analiza
		for (Integer definedDomainValue : definedDomainValues) {
			// Ejm F1 #= 1. Se excluye el 0
			isSatisfiable = Boolean.FALSE;

			if (definedDomainValue > TransformerConstants.NON_SELECTED_VALUE) {
				variabilityModelConstraintRepresentation.clear();

				constraintToAdd = VerifierUtilExpression
						.verifyAssignValueToVariabilityElementExpression(
								identifier, definedDomainValue);
				// Se adiciona la restricci�n al conjunto de restricciones
				// que representa el modelo de variabilidad
				ConfigurationOptions options = new ConfigurationOptions();
				options.addAdditionalExpression(constraintToAdd);

				// Se evalua si el modelo de restricciones es satisfacible
				isSatisfiable = solver.isSatisfiable(model,
						new Configuration(), options);

				if (isSatisfiable) {
					// Si es satisfacible la feature no es dead y se pasa a
					// la siguiente feature
					isDead = Boolean.FALSE;

					// Se adiciona ese valor como valor permitido para no
					// verificarlo en los notAttainableDomains
					if (attainableDomainsByVariabilityElementMap
							.containsKey(identifier)) {
						// Se adiciona el valor a la lista de dominios
						// permitidos
						attainableDomainsByVariabilityElementMap
								.get(identifier).add(definedDomainValue);
					} else {
						// Se adiciona el variabilityElement al mapa y se
						// adicionan el valor de dominio. Se crea un nuevo
						// objeto para evitar problemas de referencia entre
						// objetos
						Set<Integer> attainableDomainValuesList = new HashSet<Integer>();
						attainableDomainValuesList.add(definedDomainValue);
						attainableDomainsByVariabilityElementMap.put(
								identifier, attainableDomainValuesList);
					}
					break;
				} else {
					verificationExpression = constraintToAdd;
				}
			}

		}

		if (isDead) {
			// Se crea un nuevo defecto
			DeadElement deadElement = new DeadElement(identifier,
					verificationExpression);
			System.out.println("dead feature" + identifier.getId());
			return deadElement;
		}
		return null;
	}

	@Override
	public Defect isFalseOptionalElement(HlclProgram model,
			Identifier identifier) {
		BooleanExpression verificationExpression = null;
		boolean isSatisfiable = Boolean.TRUE;

		// Ejm F1 #= 0.
		// Se adiciona las restricciones que tiene el modelo de
		// variabilidad inicial

		verificationExpression = VerifierUtilExpression
				.verifyFalseOptionalExpression(identifier);
		// Se adiciona la restricci�n de verificacion al conjunto de
		// restricciones
		// que representa el modelo de variabilidad
		ConfigurationOptions options = new ConfigurationOptions();
		options.addAdditionalExpression(verificationExpression);

		// Se evalua si el modelo de restricciones es satisfacible
		isSatisfiable = solver.isSatisfiable(model, new Configuration(),
				options);

		// Se evalua si este nuevo modelo es satisfacible
		if (!isSatisfiable) {
			// Se crea un nuevo defecto
			FalseOptionalElement falseOptionalElement = new FalseOptionalElement(
					identifier, verificationExpression);
			return falseOptionalElement;
		} else {
			return null;
		}

	}

	@Override
	public List<Defect> getDeadElements(HlclProgram model,
			Set<Identifier> elementsToVerify) {
		List<Defect> deadElementsList = new ArrayList<Defect>();

		for (Identifier identifier : elementsToVerify) {
			DeadElement deadElement = (DeadElement) isDeadElement(model,
					identifier);
			if (deadElement != null) {
				deadElementsList.add(deadElement);
			}
		}
		return deadElementsList;
	}

	@Override
	public List<Defect> getFalseOptionalElements(HlclProgram model,
			Set<Identifier> elementsToVerify) {
		List<Defect> falseOptionalList = new ArrayList<Defect>();

		for (Identifier identifier : elementsToVerify) {
			FalseOptionalElement falseOptionalElement = (FalseOptionalElement) isFalseOptionalElement(
					model, identifier);
			if (falseOptionalElement != null) {
				falseOptionalList.add(falseOptionalElement);
			}
		}
		return falseOptionalList;
	}

	@Override
	public Defect isRedundant(HlclProgram model,
			BooleanExpression expressionToVerify, BooleanExpression negation) {

		if (expressionToVerify == null || negation == null) {
			throw new TechnicalException(
					"The expreession to verify redundancy and their negation is mandatory");
		}

		if (!model.contains(expressionToVerify)) {
			throw new TechnicalException(
					"HlclProgram does not contain the expression to verify:"
							+ expressionToVerify
							+ "redundancy verification is not possible");
		}

		HlclProgram modelWithoutRedundancy = new HlclProgram();
		boolean isSatisfiable = Boolean.FALSE;

		// Se evalua si el modelo de restricciones guardado en la
		// ruta temporal es resoluble
		isSatisfiable = solver.isSatisfiable(model);

		if (isSatisfiable) {

			// 2. Se verifica si el modelo sin la restriccion redundante es
			// resoluble
			for (BooleanExpression expression : model) {
				if (!expression.equals(expressionToVerify)) {
					modelWithoutRedundancy.add(expression);
				}
			}

			// Se evalua si el modelo de restricciones sin la redundancia es
			// resoluble
			isSatisfiable = solver.isSatisfiable(modelWithoutRedundancy);

			if (!isSatisfiable) {
				// La relaci�n no es redundante, pq se requiere para que el
				// modelo funcione
				return null;
			} else {
				// Si el nuevo modelo es resoluble entonces se adicionan las
				// instrucciones que corresponen a la negaci�n de la restricci�n
				// que se cree redundante
				ConfigurationOptions options = new ConfigurationOptions();
				options.addAdditionalExpression(negation);

				isSatisfiable = solver.isSatisfiable(modelWithoutRedundancy,
						new Configuration(), options);

				if (!isSatisfiable) {
					// La restricci�n si es redundante pq el modelo se volvi�
					// irresoluble
					Defect redundancy = new Redundancy(expressionToVerify,
							negation);
					return redundancy;

				}
			}
		}
		return null;
	}

	@Override
	public List<Defect> getRedundancies(HlclProgram model,
			Map<BooleanExpression, BooleanExpression> constraitsToVerify) {
		List<Defect> redundanciesList = new ArrayList<Defect>();
		Iterator<BooleanExpression> it = constraitsToVerify.keySet().iterator();
		while (it.hasNext()) {
			BooleanExpression expressionToVerify = it.next();
			BooleanExpression negation = constraitsToVerify
					.get(expressionToVerify);
			Redundancy redudancy = (Redundancy) isRedundant(model,
					expressionToVerify, negation);
			if (redudancy != null) {
				redundanciesList.add(redudancy);
			}

		}

		return redundanciesList;

	}
}