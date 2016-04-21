package gov.pnnl.stucco.dbconnect.inmemory;

import gov.pnnl.stucco.dbconnect.Condition;
import gov.pnnl.stucco.dbconnect.DBConstraintBase;

/**
 * This class is the concrete implementation of the conditional for
 * in-memory comparison types.
 *
 */
public class InMemoryConstraint extends DBConstraintBase {

    /**
     * Constructor
     * @param property
     * @param condition
     * @param value
     */
    public InMemoryConstraint(String property, Condition condition, Object value){
        super(property, condition, value);
    }

    /*
     * T.gt - greater than
     * T.gte - greater than or equal to
     * T.eq - equal to
     * T.neq - not equal to
     * T.lte - less than or equal to
     * T.lt - less than
     * T.contains - contained in a list
     * T.substring - contained in a string
     */

    public String condString(Condition c){
        if(c == Condition.eq) return "T.eq";
        if(c == Condition.neq) return "T.neq";
        if(c == Condition.gt) return "T.gt";
        if(c == Condition.gte) return "T.gte";
        if(c == Condition.lt) return "T.lt";
        if(c == Condition.lte) return "T.lte";
        if(c == Condition.contains) return "T.contains";
        if(c == Condition.substring) return "T.substring";
        return null;
    }

}
