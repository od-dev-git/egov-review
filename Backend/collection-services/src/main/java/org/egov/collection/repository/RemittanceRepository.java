package org.egov.collection.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.collection.repository.querybuilder.RemittanceQueryBuilder;
import org.egov.collection.repository.rowmapper.RemittanceDepositWorkResultSetExtractor;
import org.egov.collection.repository.rowmapper.RemittanceResultSetExtractor;
import org.egov.collection.web.contract.Remittance;
import org.egov.collection.web.contract.RemittanceDepositWorkDetail;
import org.egov.collection.web.contract.RemittanceDetail;
import org.egov.collection.web.contract.RemittanceInstrument;
import org.egov.collection.web.contract.RemittanceReceipt;
import org.egov.collection.web.contract.RemittanceSearchRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class RemittanceRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private RemittanceResultSetExtractor remittanceResultSetExtractor;
    
    @Autowired
    private  RemittanceDepositWorkResultSetExtractor remittanceDepositWorkResultSetExtractor;


    public void saveRemittance(Remittance remittance) {
        try {

            namedParameterJdbcTemplate.update(RemittanceQueryBuilder.INSERT_REMITTANCE_SQL,
                    RemittanceQueryBuilder.getParametersForRemittance(remittance));

            List<MapSqlParameterSource> remittanceDetailSource = new ArrayList<>();
            List<MapSqlParameterSource> remittanceInstrumentSource = new ArrayList<>();
            List<MapSqlParameterSource> remittanceReceiptSource = new ArrayList<>();

            for (RemittanceDetail detail : remittance.getRemittanceDetails()) {
                remittanceDetailSource.add(RemittanceQueryBuilder.getParametersForRemittanceDetails(detail));
            }

            for (RemittanceInstrument instrument : remittance.getRemittanceInstruments()) {
                remittanceInstrumentSource.add(RemittanceQueryBuilder.getParametersForRemittanceInstrument(instrument));
            }

            for (RemittanceReceipt receipt : remittance.getRemittanceReceipts()) {
                remittanceReceiptSource.add(RemittanceQueryBuilder.getParametersForRemittanceReceipt(receipt));
            }

            namedParameterJdbcTemplate.batchUpdate(RemittanceQueryBuilder.INSERT_REMITTANCE_DETAILS_SQL,
                    remittanceDetailSource.toArray(new MapSqlParameterSource[0]));
            namedParameterJdbcTemplate.batchUpdate(RemittanceQueryBuilder.INSERT_REMITTANCE_INSTRUMENT_SQL,
                    remittanceInstrumentSource.toArray(new MapSqlParameterSource[0]));
            namedParameterJdbcTemplate.batchUpdate(RemittanceQueryBuilder.INSERT_REMITTANCE_RECEIPT_SQL,
                    remittanceReceiptSource.toArray(new MapSqlParameterSource[0]));

        } catch (Exception e) {
            log.error("Failed to persist remittance to database", e);
            throw new CustomException("REMITTANCE_CREATION_FAILED", "Unable to create remittance");
        }
    }

    public void updateRemittance(Remittance remittance) {
    }

    public List<Remittance> fetchRemittances(RemittanceSearchRequest remittanceSearchRequest) {
        Map<String, Object> preparedStatementValues = new HashMap<>();
        String query = RemittanceQueryBuilder.getRemittanceSearchQuery(remittanceSearchRequest, preparedStatementValues);
        log.debug(query);
        List<Remittance> remittances = namedParameterJdbcTemplate.query(query, preparedStatementValues,
                remittanceResultSetExtractor);
        return remittances;
    }
    
    public List<RemittanceDepositWorkDetail> fetchRemittancesDepositWork(RemittanceSearchRequest remittanceSearchRequest) {
        Map<String, Object> preparedStatementValues = new HashMap<>();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("receiptnumber", remittanceSearchRequest.getReceiptNumbers());

        String query="select er2.id,er2.referencenumber,er2.referencedate,er3.creditamount,ep.receiptnumber from egcl_remittance er2 inner join egcl_remittancedetails er3 on er3.remittance = er2.id\r\n" + 
        		"\r\n" + 
        		"inner join  egcl_remittancereceipt er on er.remittance = er2.id \r\n" + 
        		"inner join  egcl_billdetial eb on eb.id =er.receipt \r\n" + 
        		"inner join  egcl_paymentdetail ep on eb.billid=ep.billid   where ep.receiptnumber in (:receiptnumber)";
     //   String query = RemittanceQueryBuilder.getRemittanceSearchQuery(remittanceSearchRequest, preparedStatementValues);
        log.debug(query);
        List<RemittanceDepositWorkDetail> remittances = namedParameterJdbcTemplate.query(query, parameters,
                remittanceDepositWorkResultSetExtractor);
        return remittances; 
    }

}
