package org.egov.bpa.calculator.repository;

import org.egov.bpa.calculator.repository.querybuilder.OCOutsideSujogQueryBuilder;
import org.egov.bpa.calculator.repository.rowmapper.OCOutsideScrutinyDetailsRowMapper;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.bpa.calculator.web.models.oc.ScrutinyDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OCOutsideRepository {

    @Autowired
    private OCOutsideSujogQueryBuilder queryBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    OCOutsideScrutinyDetailsRowMapper ocOutsideScrutinyDetailsRowMapper;



    public List<ScrutinyDetails> getScrutinyDetails(BPA bpa){
        List<Object> preparedStmtList = new ArrayList<>();
        String query= queryBuilder.getOCOutsideScrutinyDetails(bpa,preparedStmtList);
        List<ScrutinyDetails> scrutinyDetails = jdbcTemplate.query(query,preparedStmtList.toArray(), ocOutsideScrutinyDetailsRowMapper);
        return scrutinyDetails;
    }
}
