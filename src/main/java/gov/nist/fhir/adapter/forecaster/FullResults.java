/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter.forecaster;

import java.util.ArrayList;
import java.util.List;
import org.tch.fc.model.ForecastActual;
import org.tch.fc.model.ForecastEngineIssue;

/**
 *
 * @author mccaffrey
 */
public class FullResults {

    private List<ForecastActual> results = null;
    private String tchLog = null;
    private List<ForecastEngineIssue> issues = null;
    private String softwareResultStatus = null;

    /**
     * @return the results
     */
    public List<ForecastActual> getResults() {
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * @param results the results to set
     */
    public void setResults(List<ForecastActual> results) {
        this.results = results;
    }

    /**
     * @return the tchLog
     */
    public String getTchLog() {
        return tchLog;
    }

    /**
     * @param tchLog the tchLog to set
     */
    public void setTchLog(String tchLog) {
        this.tchLog = tchLog;
    }

    /**
     * @return the issues
     */
    public List<ForecastEngineIssue> getIssues() {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        return issues;
    }

    /**
     * @param issues the issues to set
     */
    public void setIssues(List<ForecastEngineIssue> issues) {
        this.issues = issues;
    }

    /**
     * @return the softwareResultStatus
     */
    public String getSoftwareResultStatus() {
        return softwareResultStatus;
    }

    /**
     * @param softwareResultStatus the softwareResultStatus to set
     */
    public void setSoftwareResultStatus(String softwareResultStatus) {
        this.softwareResultStatus = softwareResultStatus;
    }
}
