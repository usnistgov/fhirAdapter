/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nist.fhir.adapter;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import gov.nist.fhir.adapter.forecaster.ForecasterUtils;

import org.hl7.fhir.dstu3.model.Parameters;

/**
 *
 * @author mccaffrey
 */
public class ImmunizationRecommendationProvider implements IResourceProvider {

    @Override
    public Class<Parameters> getResourceType() {
        return Parameters.class;
    }

    @Operation(name = "$cds-forecast")
    public Parameters getImmunizationRecommendation(@ResourceParam Parameters inputParameters) {
        System.out.println("===> getImmunizationRecommendation");
        return ForecasterUtils.run(inputParameters);
    }
    
}
