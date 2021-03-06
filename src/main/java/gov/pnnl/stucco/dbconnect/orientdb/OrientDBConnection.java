package gov.pnnl.stucco.dbconnect.orientdb;

import gov.pnnl.stucco.dbconnect.Condition;
import gov.pnnl.stucco.dbconnect.DBConnectionBase;
import gov.pnnl.stucco.dbconnect.DBConstraint;
import gov.pnnl.stucco.dbconnect.StuccoDBException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONArray;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.impls.orient.OrientDynaElementIterable;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class OrientDBConnection extends DBConnectionBase {
   
    private OrientGraph graphDB = null;
    private Logger logger = null;
    private Map<String, Object> configuration = new HashMap<String,Object>();
    //private Map<String, String> vertIDCache; //TODO could really split this into a simple cache class.
    //private Set<String> vertIDCacheRecentlyRead;
    private static int VERT_ID_CACHE_LIMIT = 10000;
    private static String[] HIGH_FORWARD_DEGREE_EDGE_LABELS = {"hasFlow"}; //TODO: update as needed.  Knowing these allows some queries to be optimized.

    
    public OrientDBConnection(Map<String, Object> configuration) {
        this.configuration.putAll(configuration);
        logger = LoggerFactory.getLogger(OrientDBConnection.class);
        //vertIDCache = new HashMap<String, String>((int) (VERT_ID_CACHE_LIMIT * 1.5));
        //vertIDCacheRecentlyRead = new HashSet<String>((int) (VERT_ID_CACHE_LIMIT * 1.5));

    }

    @Override
    public void updateVertex(String id, Map<String, Object> properties)
            throws IllegalArgumentException, IllegalStateException 
    {
        String[] keys = properties.keySet().toArray(new String[0]);
        for(int i=0; i<keys.length; i++){
            updateVertProperty(id, keys[i], properties.get(keys[i]));
            graphDB.commit();
        }

    }

    private void updateVertProperty(String id, String key, Object val) throws OCommandExecutionException{ 
        updateVertexProperty(id, key, val);
    }


    @Override
    protected void setPropertyInDB(String id, String key, Object newValue) {
        String query = String.format("SELECT FROM %s",id);
        List<OrientVertex> verts = this.getVerticesFromQuery(query);
        if(!verts.isEmpty()) {
            verts.get(0).setProperty(key, newValue);
        }
    }

    /** 
     * Runs SQL query to get vertices.
     * 
     * @throws StuccoDBException on bad query
     * 
     * @return Zero or more vertices
     * */
    private List<OrientVertex> getVerticesFromQuery(String query) {
        OrientDynaElementIterable qiterable = executeSQL(query);
        List<OrientVertex> vertexList = new ArrayList<OrientVertex>(1);
        if (qiterable != null) { // Don't know if this can happen, but just in case
            Iterator<Object> iter = qiterable.iterator();
            while (iter.hasNext()) {
                vertexList.add((OrientVertex) iter.next());
            }
        }
        return vertexList;
    }
    
    /** 
     * Runs SQL query to get edges.
     * 
     * @throws StuccoDBException on bad query
     * 
     * @return Zero or more edges
     * */
    private List<OrientEdge> getEdgesFromQuery(String query) {
        OrientDynaElementIterable qiterable = executeSQL(query);
        List<OrientEdge> edgeList = new ArrayList<OrientEdge>(1);
        if (qiterable != null) { // Don't know if this can happen, but just in case
            Iterator<Object> iter = qiterable.iterator();
            while (iter.hasNext()) {
                edgeList.add((OrientEdge) iter.next());
            }
        }
        return edgeList;
    }

    /**
     *  Runs SQL query to do things like create properties, indexes, etc...
     * @param query
     * @return
     */
    <T> T executeSQL(String query) {
        try {
            OCommandSQL sql = new OCommandSQL(query);
            OCommandRequest ocr = graphDB.command(sql);
            T obj = ocr.<T>execute();
            return obj;
        } catch (OCommandExecutionException e)
        {
            throw new StuccoDBException(e);
        }
        
    }
    
    /** 
     * {@inheritDoc}
     * <p>Gets the properties of a vertex selected by RID. 
     * 
     * TODO: deal with exceptions
     * @throws OCommandExecutionException on bad query
     * 
     * @return1 Map of properties (or null if vertex not found)
     */
    @Override
    public Map<String, Object> getVertByID(String id) {
        if(id == null || id.isEmpty())
            return null;
        
        String query = String.format("Select from %s", id);
        List<OrientVertex> vertexList = getVerticesFromQuery(query);
        if(vertexList.isEmpty()){
            return null;
        }
        Map<String,Object> propertyMap = vertexList.get(0).getProperties();
        return propertyMap;
    }

    /*
    @Override
    public Map<String, Object> getVertByName(String name){
        if(name == null || name.isEmpty())
            return null;

        String query = String.format("Select * from V where name='%s'", name);
        List<OrientVertex> vertexList = getVerticesFromQuery(query);

        if(vertexList.size() == 0){
            return null;
        }else if(vertexList.size() > 1){
            logger.warn("findVert found more than 1 matching verts for name: " + name + " so returning the first item.");
        }
        
        Map<String,Object> propertyMap = vertexList.get(0).getProperties();

        return propertyMap;
    }
    */

    /**
     * {@inheritDoc}
     * <p>  function will check vertIDCache first, 
     * if id is not in there, then it is calling the findVert function
     */
    /*
    @Override
    public String getVertIDByName(String name) {
        String id = vertIDCacheGet(name);
        if(id != null){
            return id;
        }else{
            Map<String, Object> vert = getVertByName(name);
            if(vert == null) 
                id = null;
            else 
                id = vert.get("@rid").toString();
            if(id != null){
                vertIDCachePut(name, id);
            }
            return id;
        }
    }
    */
    
    /*
    private void vertIDCachePut(String name, String id){
        if(vertIDCache.size() >= VERT_ID_CACHE_LIMIT){
            logger.info("vertex id cache exceeded limit of " + VERT_ID_CACHE_LIMIT + 
                    " ... evicting " + (vertIDCache.size() - vertIDCacheRecentlyRead.size()) + " unused items.");
            vertIDCache.keySet().retainAll(vertIDCacheRecentlyRead);
            vertIDCacheRecentlyRead.clear();
        }
        vertIDCache.put(name, id);
    }
    
    private String vertIDCacheGet(String name){
        if(vertIDCache.containsKey(name)){ 
            vertIDCacheRecentlyRead.add(name);
            return vertIDCache.get(name);
        }else{
            return null;
        }
        
    }
    */

    @Override
    public String addVertex(Map<String, Object> properties) {
        String name = (String)properties.get("name");
        if(name == null || name.isEmpty()){
            String msg = String.format("cannot add vertex with missing or invalid vertID");
            throw new IllegalArgumentException(msg);
        }
        
        //convert any multi-valued properties to a set form.
        convertAllMultiValuesToSet(properties);
        OrientVertex v = graphDB.addVertex("class:V", properties);
        graphDB.commit();
        return v.getId().toString();
    }

    @Override
    public List<String> getVertIDsByConstraints(List<DBConstraint> constraints) {
        if(constraints == null)
            throw new NullPointerException();
        
        String query = "SELECT * FROM V " + buildQueryConstraintSql(constraints);
        
        List<OrientVertex> verts = this.getVerticesFromQuery(query);
        List<String> vertIDs = new ArrayList<String>();
        for (OrientVertex vertID : verts ){
            vertIDs.add(vertID.getId().toString());
        }
        
        return vertIDs;
    }

    @Override
    public List<String> getVertIDsByConstraints(List<DBConstraint> constraints, int offset, int limit) {
        if(constraints == null)
            throw new NullPointerException();
        
        String query = "SELECT * FROM V OFFSET " + offset + " LIMIT " + limit + " " + buildQueryConstraintSql(constraints);
        
        List<OrientVertex> verts = this.getVerticesFromQuery(query);
        List<String> vertIDs = new ArrayList<String>();
        for (OrientVertex vertID : verts ){
            vertIDs.add(vertID.getId().toString());
        }
        
        return vertIDs;
    }
    
    /** Wraps a query within a WHERE query adding constraints on the result. */
    private String wrapQueryWithWhereClause(String sqlToWrap, List<DBConstraint> constraints) {
        if (constraints.isEmpty()) {
            return sqlToWrap;
        }
        
        String whereClause = buildQueryConstraintSql(constraints);
        String query = String.format("SELECT * FROM (%s) %s", sqlToWrap, whereClause);
        return query;
    }
    
    /** Builds the WHERE clause of a constraints query. */
    private String buildQueryConstraintSql(List<DBConstraint> constraints) {
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<constraints.size(); i++){
            DBConstraint c = constraints.get(i);
            String cond = c.condString(c.getCond());
            String key = c.getProp();
            Object value = c.getVal();

            if(i == 0 ) {
                builder.append(" WHERE ");
            } else {
                builder.append(" AND ");
            }
            if(value instanceof String) {
                builder.append(String.format(" %s %s '%s' ", key, cond, value)); 
            } else {
                builder.append(String.format(" %s %s %s ", key, cond, value));
            }
        }
        return builder.toString();
    }

    @Override
    public void addEdge(String inVertID, String outVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot add edge with missing or invalid relation");
        }
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot add edge with missing or invalid inVertID");
        }
        if(outVertID == null || outVertID.equals("")){
            throw new IllegalArgumentException("cannot add edge with missing or invalid outVertID");
        }
       
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("inVertID", inVertID);
        props.put("outVertID", outVertID);
        props.put("relation", relation);
        
        //and now finally add edge to graphDB.  If it fails, return false here. if it was ok, then we can continue below.
        String query = String.format("Select * from [%s,%s]", outVertID, inVertID);
        List<OrientVertex> vertexList = getVerticesFromQuery(query);
        if(vertexList.size() == 2) {
            vertexList.get(0).addEdge(relation, vertexList.get(1), null /*iClassName*/, null /*iClusterName*/, props);
            
            // alternate way to create an edge using both vertex's, we need another call to specify the properties
//            String id = String.format("class:%s",relation);
//            OrientEdge e =graphDB.addEdge((Object)id, vertexList.get(0), vertexList.get(1), relation);
//            e.setProperties(props);
            graphDB.commit();
        } else {
            String msg = String.format("cannot add edge with missing or invalid vertID, either %s or %s", outVertID, inVertID);
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public List<String> getInVertIDsByRelation(String outVertID, String relation) {
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invlid relation");
        }
        if(outVertID == null || outVertID.equals("")){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        
        Object query_ret = getVertByID(outVertID);
        if(query_ret == null){
            logger.warn("getInVertIDsByRelation could not find outVertID:" + outVertID);
            throw new IllegalArgumentException("missing or invalid outVertID");
        }

        
        String query = String.format("SELECT expand(out('%s')) FROM %s",relation, outVertID); 
        List<OrientVertex>results = getVerticesFromQuery(query);

        List<String> relatedIDs = new ArrayList<String>();
        for(OrientVertex item : results){
            String idValue = item.getId().toString();
            relatedIDs.add(idValue);
//            String idvalue = item.getProperties().get("@rid").toString();
            
        }
        return relatedIDs;
    }

    @Override
    public List<String> getOutVertIDsByRelation(String inVertID, String relation){
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invlid relation");
        }
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        
        Object query_ret = getVertByID(inVertID);
        if(query_ret == null){
            logger.warn("getOutVertIDsByRelation could not find inVertID:" + inVertID);
            throw new IllegalArgumentException("missing or invalid inVertID");
        }

        
        String query = String.format("SELECT expand(in('%s')) FROM %s",relation, inVertID); 
        List<OrientVertex>results = getVerticesFromQuery(query);

        List<String> relatedIDs = new ArrayList<String>();
        for(OrientVertex item : results){
            String idValue = item.getId().toString();
            relatedIDs.add(idValue);
//            String idvalue = item.getProperties().get("@rid").toString();
            
        }
        return relatedIDs;
    }

    @Override
    public List<String> getVertIDsByRelation(String vertID, String relation) {
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invlid relation");
        }
        if(vertID == null || vertID.equals("")) {
            throw new IllegalArgumentException("cannot get edge with missing or invalid vertID");
        }

        Object query_ret = getVertByID(vertID);
        if(query_ret == null){
            logger.warn("getVertIDsByRelation could not find vertID:" + vertID);
            throw new IllegalArgumentException("missing or invalid vertID");
        }

        String query = String.format("SELECT expand(both('%s')) FROM %s",relation, vertID); 
        List<OrientVertex>results = getVerticesFromQuery(query);

        List<String> relatedIDs = new ArrayList<String>();
        for(OrientVertex item : results){
            String idValue = item.getId().toString();
            relatedIDs.add(idValue);
//            String idvalue = item.getProperties().get("@rid").toString();

        }
        return relatedIDs;
    }

    @Override
    public List<String> getInVertIDsByRelation(String outVertID, String relation, List<DBConstraint> constraints) {
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(outVertID == null || outVertID.equals("")){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }

        Object query_ret = getVertByID(outVertID);
        if(query_ret == null){
            logger.warn("getInVertIDsByRelation could not find outVertID:" + outVertID);
            throw new IllegalArgumentException("missing or invalid outVertID");
        }

        String relationQuery = String.format("SELECT expand(out('%s')) FROM %s",relation, outVertID);
        String overallQuery = wrapQueryWithWhereClause(relationQuery, constraints);
        List<OrientVertex>results = getVerticesFromQuery(overallQuery);

        List<String> relatedIDs = new ArrayList<String>();
        for(OrientVertex item : results){
            String idValue = item.getId().toString();
            relatedIDs.add(idValue);
//            String idvalue = item.getProperties().get("@rid").toString();

        }
        return relatedIDs;
    }

    @Override
    public List<String> getOutVertIDsByRelation(String inVertID, String relation, List<DBConstraint> constraints) {
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(inVertID == null || inVertID.equals("")){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }

        Object query_ret = getVertByID(inVertID);
        if(query_ret == null){
            logger.warn("getOutVertIDsByRelation could not find inVertID:" + inVertID);
            throw new IllegalArgumentException("missing or invalid inVertID");
        }

        String relationQuery = String.format("SELECT expand(in('%s')) FROM %s",relation, inVertID);
        String overallQuery = wrapQueryWithWhereClause(relationQuery, constraints);
        List<OrientVertex>results = getVerticesFromQuery(overallQuery);

        List<String> relatedIDs = new ArrayList<String>();
        for(OrientVertex item : results){
            String idValue = item.getId().toString();
            relatedIDs.add(idValue);
        }
        return relatedIDs;
    }

    @Override
    public List<String> getVertIDsByRelation(String vertID, String relation, List<DBConstraint> constraints) {
        if(relation == null || relation.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid relation");
        }
        if(vertID == null || vertID.equals("")){
            throw new IllegalArgumentException("cannot get edge with missing or invalid vertID");
        }

        Object query_ret = getVertByID(vertID);
        if(query_ret == null){
            logger.warn("getVertIDsByRelation could not find vertID:" + vertID);
            throw new IllegalArgumentException("missing or invalid vertID");
        }

        String relationQuery = String.format("SELECT expand(both('%s')) FROM %s",relation, vertID);
        String overallQuery = wrapQueryWithWhereClause(relationQuery, constraints);
        List<OrientVertex>results = getVerticesFromQuery(overallQuery);

        List<String> relatedIDs = new ArrayList<String>();
        for(OrientVertex item : results){
            String idValue = item.getId().toString();
            relatedIDs.add(idValue);
        }
        return relatedIDs;
    }

    /*
     * Only used by removeAllVertices()
     */
    /*
    private void removeCachedVertices(){
        //NB: this query is slow enough that connection can time out if the DB starts with many vertices.

        if(vertIDCache.isEmpty())
            return;

        //clear the cache now.
        vertIDCache.clear();
        vertIDCacheRecentlyRead.clear();// = new HashSet<String>((int) (VERT_ID_CACHE_LIMIT * 1.5));
    }
    */
    
    @Override
    public void removeVertByID(String id) {
        
        // remove the cached vertices
        //removeCachedVertices();
        
        String query = String.format("DELETE VERTEX %s", id);
        executeSQL(query);
    }

    @Override
    public void removeEdgeByRelation(String v1, String v2, String relation) {
       
        String query = String.format("DELETE EDGE FROM %s TO %s WHERE relation=\"%s\"", v1, v2, relation);
        executeSQL(query);
        
    }

    @Override
    public int getEdgeCountByRelation(String inVertID, String outVertID, String relation) {
        int edgeCount = 0;
        
        Object query_ret;

        query_ret = getVertByID(outVertID);
        if(query_ret == null){
            logger.warn("getEdgeCount could not find out_id:" + outVertID);
            throw new IllegalArgumentException("invalid outVertID: "+ outVertID);
        }
        query_ret = getVertByID(inVertID);
        if(query_ret == null){
            logger.warn("getEdgeCount could not find inv_id:" + inVertID);
            throw new IllegalArgumentException("invalid inVertID: "+ inVertID);
        }

        String query = String.format("SELECT expand(out('%s')) FROM %s",relation, outVertID); 
        List<OrientVertex>results = getVerticesFromQuery(query);

        for(OrientVertex item : results){
            String idvalue = item.getProperties().get("@rid").toString();
            if(inVertID.equals(idvalue))
                edgeCount++;
        }
        return edgeCount;
    }
    
    @Override
    public void close() {
        if(graphDB != null){
            graphDB.shutdown();
        }
    }
    
    /**
     * need to commit this transaction
     */
    public void commit() {
        graphDB.commit();
    }

    @Override
    public void open() {
        logger.info("connecting to DB...");

        try {
            // extract configuration for the DB of interest
            String dbName = (String)configuration.get("graph-name");
            graphDB = new OrientGraph(dbName);
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
            logger.warn(getStackTrace(e));
            graphDB.shutdown();
            throw new StuccoDBException("could not create OrientDB client connection");
        }
    }

    private static String getStackTrace(Exception e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAllVertices() {
        
        // remove the cached vertices
        //removeCachedVertices();

        //NB: this query is slow enough that connection can time out if the DB starts with many vertices.
        String query = String.format("DELETE VERTEX V");
        executeSQL(query);
        
    }

    @Override
    public DBConstraint getConstraint(String property, Condition condition,
            Object value) {
        return new OrientDBConstraint(property, condition, value);
    }

    @Override
    public void buildIndex(String indexConfig) {
        IndexDefinitionsToOrientDB loader = new IndexDefinitionsToOrientDB();
        loader.setDBConnection(this);
        try {
            loader.parse(new File(indexConfig));
        } catch (IOException e) {
            throw new StuccoDBException(e);
        }
        
    }

    @Override
    public void loadState(String filePath){
        //TODO: implement
    }

    @Override
    public void saveState(String filePath){
        //TODO: implement
    }
    
    /**
     * return the raw handle to the DB
     */
    public OrientGraph getGraphDB() {
        return graphDB;
    }

    @Override
    public long getVertCount() {
        long count = this.graphDB.countVertices();
        return count;
    }

    @Override
    public long getEdgeCount() {
        long count = this.graphDB.countEdges();
        return count;
    }

    @Override
    public List<Map<String, Object>> getOutEdges(String outVertID) {
        if(outVertID == null || outVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        
        Object query_ret = getVertByID(outVertID);
        if(query_ret == null){
            logger.warn("getOutVertIDs could not find inVertID:" + outVertID);
            throw new IllegalArgumentException("missing or invalid outVertID");
        }
        
        String query = String.format("SELECT expand(outE()) FROM %s", outVertID); 
        List<OrientEdge>results = getEdgesFromQuery(query);

        List<Map<String,Object> > edgePropertyList = new ArrayList<Map<String,Object>>();
        for(OrientEdge item : results){
            Map<String,Object> edgeProperties = new HashMap<String, Object>();
            String relation = item.getLabel();
            String inVertID = item.getInVertex().getIdentity().toString();
            
            edgeProperties.put("inVertID", inVertID);
            edgeProperties.put("outVertID", outVertID);
            edgeProperties.put("relation", relation);
            edgePropertyList.add(edgeProperties);
        }
        
        return edgePropertyList;
    }

    @Override
    public List<Map<String, Object>> getInEdges(String inVertID) {
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }

        Object query_ret = getVertByID(inVertID);
        if(query_ret == null){
            logger.warn("getInVertIDs could not find inVertID:" + inVertID);
            throw new IllegalArgumentException("missing or invalid inVertID");
        }

        String query = String.format("SELECT expand(inE()) FROM %s", inVertID); 
        List<OrientEdge>results = getEdgesFromQuery(query);

        List<Map<String,Object> > edgePropertyList = new ArrayList<Map<String,Object>>();
        for(OrientEdge item : results){
            Map<String,Object> edgeProperties = new HashMap<String, Object>();
            String relation = item.getLabel();
            String outVertID = item.getOutVertex().getIdentity().toString();

            edgeProperties.put("inVertID", inVertID);
            edgeProperties.put("outVertID", outVertID);
            edgeProperties.put("relation", relation);
            edgePropertyList.add(edgeProperties);
        }

        return edgePropertyList;
    }

    //TODO: profile this paging implementation, and possibly change
    @Override
    public List<Map<String, Object>> getOutEdgesPage(String outVertID, int offset, int limit) {
        if(outVertID == null || outVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid outVertID");
        }
        
        Object query_ret = getVertByID(outVertID);
        if(query_ret == null){
            logger.warn("getOutVertIDs could not find inVertID:" + outVertID);
            throw new IllegalArgumentException("missing or invalid outVertID");
        }
        
        String query = String.format("SELECT expand(outE()) FROM %s", outVertID); 
        List<OrientEdge>results = getEdgesFromQuery(query);
        List<Map<String,Object> > edgePropertyList = new ArrayList<Map<String,Object>>();
        if (results.size() > offset) {
            int end = Math.min(results.size(), offset + limit);
            for (int i = offset; i < end; i++) {
                OrientEdge item = results.get(i);
                Map<String,Object> edgeProperties = new HashMap<String, Object>();
                String relation = item.getLabel();
                String inVertID = item.getInVertex().getIdentity().toString();
                
                edgeProperties.put("inVertID", inVertID);
                edgeProperties.put("outVertID", outVertID);
                edgeProperties.put("relation", relation);
                edgePropertyList.add(edgeProperties);
            }
        }
        
        return edgePropertyList;
    }

    //TODO: profile this paging implementation, and possibly change
    @Override
    public List<Map<String, Object>> getInEdgesPage(String inVertID, int offset, int limit) {
        if(inVertID == null || inVertID.equals("") ){
            throw new IllegalArgumentException("cannot get edge with missing or invalid inVertID");
        }

        Object query_ret = getVertByID(inVertID);
        if(query_ret == null){
            logger.warn("getInVertIDs could not find inVertID:" + inVertID);
            throw new IllegalArgumentException("missing or invalid inVertID");
        }

        String query = String.format("SELECT expand(inE()) FROM %s", inVertID); 
        List<OrientEdge>results = getEdgesFromQuery(query);

        List<Map<String,Object> > edgePropertyList = new ArrayList<Map<String,Object>>();
        if (results.size() > offset) {
            int end = Math.min(results.size(), offset + limit);
            for (int i = offset; i < end; i++) {
                OrientEdge item = results.get(i);
                Map<String,Object> edgeProperties = new HashMap<String, Object>();
                String relation = item.getLabel();
                String outVertID = item.getOutVertex().getIdentity().toString();

                edgeProperties.put("inVertID", inVertID);
                edgeProperties.put("outVertID", outVertID);
                edgeProperties.put("relation", relation);
                edgePropertyList.add(edgeProperties);
            }
        }

        return edgePropertyList;
    }

    /**
     * Used by postgres
     */
    public void bulkLoadGraph(JSONObject graph) {}

    /**
     * TODO: implementation
     * return the number of vertices
     * @return count
     */
    @Override
    public long getVertCountByConstraints(List<DBConstraint> constraints) {
        return 0L;
    }

    /**
     * gets the number of edges in the graph dest id = inVertID
     */
    @Override
    public long getInEdgeCount(String inVertID) {
        return 0L;
    };

    /**
     * gets the number of edges in the graph with src id = outVertID
     */
    @Override
    public long getOutEdgeCount(String outVertID) {
        return 0L;
    }
}
