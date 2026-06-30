package org.openscience.sherlock.model.exchange;

import org.openscience.sherlock.dbservice.result.model.ResultRecord;
import org.openscience.sherlock.model.DereplicationOptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RequestResult {
    // basic
    private String queryType;
    private String requestId;
    // error message
    private String errorMessage;
    // dreplication
    private DereplicationOptions dereplicationOptions;
    // result/retrieval
    private ResultRecord resultRecord;
    // cancelation
    private Boolean isCancelled;
}
