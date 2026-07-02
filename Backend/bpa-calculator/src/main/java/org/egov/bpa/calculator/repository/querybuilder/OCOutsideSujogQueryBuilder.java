package org.egov.bpa.calculator.repository.querybuilder;

import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OCOutsideSujogQueryBuilder {


    private static final String OC_OUTSIDE_SCRUTINY_DETAILS = "Select eboos.id, eboos.bpaid, eboos.tenantId, eboos.infotype, eboos.plotarea, eboos.giftedlandarea, eboos.buildingblocks, eboos.basefar, eboos.maxpermissiblefar, eboos.approvedfar, eboos.providedfar, eboos.tdrfarrelaxation, eboos.totalbua, eboos.totalfloorarea," +
            " eboos.totalcarpetarea, eboos.nooftemporarystructures, eboos.projectvalueforeidp, eboos.isShelterFeeApplicable, eboos.isSecurityDepositRequired, eboos.bmvperacre, eboos.isretentionfeeapplicable, eboos.totalnoofdwellingunits, eboos.occupancyTypeHelperCode, eboos.occupancySubTypeHelperCode, eboos.permitfee, eboos.additionaldetails from eg_bpa_oc_outsidesujog_scrutiny eboos ";


    /**
     * add if clause to the Statement if required or elese AND
     *
     * @param values
     * @param queryString
     */
    private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
        if (values.isEmpty())
            queryString.append(" WHERE ");

        else {
            queryString.append(" AND");
        }
    }


    public String getOCOutsideScrutinyDetails(BPA bpa, List<Object> preparedStmtList){
        StringBuilder queryBuilder = new StringBuilder(OC_OUTSIDE_SCRUTINY_DETAILS);

        if(bpa.getId()!=null){
            addClauseIfRequired(preparedStmtList, queryBuilder);
            queryBuilder.append(" eboos.bpaid=? ");
            preparedStmtList.add(bpa.getId());
        }
        if(bpa.getTenantId()!=null){
            addClauseIfRequired(preparedStmtList, queryBuilder);
            queryBuilder.append(" eboos.tenantid=? ");
            preparedStmtList.add(bpa.getTenantId());
        }
        return queryBuilder.toString();
    }
}
