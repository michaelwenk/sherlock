package org.openscience.sherlock.model;

/**
 * The type of query to be performed. Possible values are:
 * - "DEREPLICATION": Perform dereplication query, depending on the provided
 * dereplication options and correlation data.
 * - "DETECTION": Perform detection query, depending on the provided detection
 * options and correlation data.
 * - "ELUCIDATION": Perform elucidation query, depending on the provided
 * elucidation options and correlation data.
 * - "ELUCIDATION_ASYNC": Perform elucidation query asynchronously, depending on
 * the provided elucidation options and correlation data. The result will be
 * available afterwards and can be retrieved using the request ID.
 * - "RETRIEVE": Retrieve the result of a previous query using the provided
 * request ID.
 * - "CANCEL": Cancel a running query using the provided request ID.
 * - "STATUS": Check the status of a running query using the provided request
 * ID.
 */
public class QueryTypes {
    public static final String DEREPLICATION = "DEREPLICATION";
    public static final String DETECTION = "DETECTION";
    public static final String ELUCIDATION = "ELUCIDATION";
    public static final String ELUCIDATION_ASYNC = "ELUCIDATION_ASYNC";
    public static final String RETRIEVE = "RETRIEVE";
    public static final String CANCEL = "CANCEL";
    public static final String STATUS = "STATUS";
}
