package com.variamos.defectAnalyzer.model;

import java.util.List;

import com.variamos.defectAnalyzer.dto.VMVerifierOutDTO;
import com.variamos.defectAnalyzer.model.defects.Defect;

public class AnalyzedCorrectionSet {

	private Long id;
	private VariabilityModel variabilityModel;
	private List<Dependency> correctionSubsets;
	private VMVerifierOutDTO verifierOutDTO;
	private Defect analyzedDefect;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the variabilityModel
	 */
	public VariabilityModel getVariabilityModel() {
		return variabilityModel;
	}

	/**
	 * @param variabilityModel
	 *            the variabilityModel to set
	 */
	public void setVariabilityModel(VariabilityModel variabilityModel) {
		this.variabilityModel = variabilityModel;
	}

	/**
	 * @return the correctionSubsets
	 */
	public List<Dependency> getCorrectionSubsets() {
		return correctionSubsets;
	}

	/**
	 * @param correctionSubsets
	 *            the correctionSubsets to set
	 */
	public void setCorrectionSubsets(List<Dependency> correctionSubsets) {
		this.correctionSubsets = correctionSubsets;
	}

	/**
	 * @return the analyzedDefect
	 */
	public Defect getAnalyzedDefect() {
		return analyzedDefect;
	}

	/**
	 * @param analyzedDefect
	 *            the analyzedDefect to set
	 */
	public void setAnalyzedDefect(Defect analyzedDefect) {
		this.analyzedDefect = analyzedDefect;
	}

	/**
	 * @return the verifierOutDTO
	 */
	public VMVerifierOutDTO getVerifierOutDTO() {
		return verifierOutDTO;
	}

	/**
	 * @param verifierOutDTO
	 *            the verifierOutDTO to set
	 */
	public void setVerifierOutDTO(VMVerifierOutDTO verifierOutDTO) {
		this.verifierOutDTO = verifierOutDTO;
	}

}
