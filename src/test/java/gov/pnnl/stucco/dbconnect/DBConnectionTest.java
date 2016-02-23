package gov.pnnl.stucco.dbconnect;


import gov.pnnl.stucco.dbconnect.Condition;
import gov.pnnl.stucco.dbconnect.DBConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.json.JSONObject;

/**
 * Unit test for generically Testing the DBConnection
 * NOTE: two environment variable must be defined:
 *       STUCCO_DB_CONFIG=<path/filename.yml>
 *       STUCCO_DB_TYPE= INMEMORY|ORIENTDB|TITAN|NEO4J
 */
public class DBConnectionTest 
extends TestCase
{
    private static DBConnectionFactory factory;// = DBConnectionFactory.getFactory(DBConnectionFactory.Type.INMEMORY);
    private static DBConnectionTestInterface conn;

    static {
        // get environment variables
        String type = System.getenv("STUCCO_DB_TYPE");
        if (type == null) {
            throw (new NullPointerException("Missing environment variable STUCCO_DB_TYPE"));
        }

        factory = DBConnectionFactory.getFactory(DBConnectionFactory.Type.valueOf(type));

        String config = System.getenv("STUCCO_DB_CONFIG");
        if (config == null) {
            throw (new NullPointerException("Missing environment variable STUCCO_DB_CONFIG"));
        }
        factory.setConfiguration(config);

    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DBConnectionTest( String testName )
    {
        super( testName );
    }


    public void setUp(){
        //TODO: add this to other tests below
        conn = factory.getDBConnectionTestInterface();
        conn.open();
        //System.out.println(" Global setUp started");
        //System.out.println(" Global setUp done");
    }

    public void tearDown(){
        //System.out.println(" Global tearDown ");
        conn.removeAllVertices();
        conn.close();
    }

    /**
     * @return the suite of tests being tested
     */
    //	public static Test suite()
    //	{
    //		//return new TestSuite( DBConnectionTest.class );
    //		return new TestSetup(new TestSuite(DBConnectionTest.class)) {
    //
    //
    //	        }
    //	    };
    //	}

    /**
     * Tests loading, querying, and other basic operations for vertices, edges, properties.
     */
    public void testLoad()
    {

        //conn.removeAllVertices(); // when all vertices are removed all edges are dropped

        String vert1 = "{" +
                "\"name\":\"CVE-1999-0002\"," +
                "\"_type\":\"vertex\","+
                "\"source\":[\"CVE\"],"+
                "\"description\":\"Buffer overflow in NFS mountd gives root access to remote attackers, mostly in Linux systems.\","+
                "\"references\":["+
                "\"CERT:CA-98.12.mountd\","+
                "\"http://www.ciac.org/ciac/bulletins/j-006.shtml\","+
                "\"http://www.securityfocus.com/bid/121\","+
                "\"XF:linux-mountd-bo\"],"+
                "\"status\":\"Entry\","+
                "\"score\":1.0"+
                "}";
        String vert2 = "{"+
                "\"name\":\"CVE-1999-nnnn\"," +
                "\"_type\":\"vertex\","+
                "\"source\":[\"CVE\"],"+
                "\"description\":\"test description asdf.\","+
                "\"references\":["+
                "\"http://www.google.com\"],"+
                "\"status\":\"Entry\","+
                "\"score\":1.0"+
                "}";
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert1)));
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert2)));

        //find this node, check some properties.
        String id = conn.getVertIDByName("CVE-1999-0002");
        Map<String, Object> vertProps = conn.getVertByID(id);
        String[] expectedRefs = {"CERT:CA-98.12.mountd","XF:linux-mountd-bo","http://www.ciac.org/ciac/bulletins/j-006.shtml","http://www.securityfocus.com/bid/121"};
        String[] actualRefs = ((ArrayList<String>)vertProps.get("references")).toArray(new String[0]);
        assertTrue(expectedRefs.length == actualRefs.length);
        Arrays.sort(expectedRefs);
        Arrays.sort(actualRefs);
        assertTrue(Arrays.equals(expectedRefs, actualRefs));

        //find the other node, check its properties.
        String id2 = conn.getVertIDByName("CVE-1999-nnnn");
        vertProps = (Map<String,Object>)conn.getVertByName("CVE-1999-nnnn");
        assertEquals("test description asdf.", vertProps.get("description"));
        expectedRefs = new String[]{"http://www.google.com"};
        actualRefs = ((ArrayList<String>)vertProps.get("references")).toArray(new String[0]);
        assertTrue(Arrays.equals(expectedRefs, actualRefs));

        //There should be no edge between them
        assertEquals(0, conn.getEdgeCountByRelation(id, id2, "sameAs"));
        assertEquals(0, conn.getEdgeCountByRelation(id2, id, "sameAs")); //just to be sure.

        //now add an edge
        //conn.addEdge(inVid, outVid, label);
        conn.addEdge(id, id2, "sameAs");

        //and now we can test the edge between them
        int c1 = conn.getEdgeCountByRelation(id, id2, "sameAs");
        int c2 = conn.getEdgeCountByRelation(id2, id, "sameAs");
        assertEquals(1, c1);
        assertEquals(0, c2);

        List<String> matchingIDs = null;
        matchingIDs = conn.getInVertIDsByRelation(id2, "sameAs");
        assertEquals(1, matchingIDs.size());
        assertEquals(id, matchingIDs.get(0));

        matchingIDs = conn.getOutVertIDsByRelation(id, "sameAs");
        assertEquals(1, matchingIDs.size());
        assertEquals(id2, matchingIDs.get(0));

        //conn.removeAllVertices();
    }


    /**
     * Tests updating vertex properties
     */
    public void testUpdate()
    {
        //		InMemoryDBConnection conn = new InMemoryDBConnection();

        //conn.removeAllVertices();

        String vert1 = "{"+
                "\"endIPInt\":55," +
                "\"_type\":\"vertex\","+
                "\"source\": [\"aaaa\"],"+
                "\"name\":\"testvert_55\"" +
                "}";
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert1)));

        String id = conn.getVertIDByName("testvert_55");

        Map<String, Object> vertProps = conn.getVertByID(id);
        assertEquals( "55", vertProps.get("endIPInt").toString());
        assertEquals( "[aaaa]", vertProps.get("source").toString());

        Map<String, Object> newProps = new HashMap<String, Object>();
        newProps.put("source", "aaaa");
        conn.updateVertex(id, newProps);

        vertProps = conn.getVertByID(id);
        assertEquals( "55", vertProps.get("endIPInt").toString());
        //assertEquals( "[aaaa]", vertProps.get("source").toString());//NB: behavior is now different.
        assertEquals( "aaaa", vertProps.get("source").toString());

        newProps = new HashMap<String, Object>();
        newProps.put("startIPInt", "33");
        newProps.put("endIPInt", "44");
        newProps.put("source", "bbbb");
        conn.updateVertex(id, newProps);

        vertProps = conn.getVertByID(id);
        assertEquals("33", vertProps.get("startIPInt").toString());
        assertEquals("44", vertProps.get("endIPInt").toString());
        //assertEquals("[aaaa, bbbb]", vertProps.get("source").toString());//NB: behavior is now different.
        assertEquals("bbbb", vertProps.get("source").toString());

        newProps = new HashMap<String, Object>();
        String[] sourceArray = {"cccc", "dddd"};
        newProps.put("source", sourceArray);
        conn.updateVertex(id, newProps);

        vertProps = conn.getVertByID(id);
        //assertEquals("[aaaa, bbbb, cccc, dddd]", vertProps.get("source").toString());//NB: behavior is now different.
        assertEquals(sourceArray, vertProps.get("source"));

        newProps = new HashMap<String, Object>();
        Set<String> sourceSet = new HashSet<String>();
        sourceSet.add("eeee");
        sourceSet.add("ffff");
        newProps.put("source", sourceSet);
        conn.updateVertex(id, newProps);

        vertProps = conn.getVertByID(id);
        //assertEquals("[aaaa, bbbb, cccc, dddd, eeee, ffff]", vertProps.get("source").toString());//NB: behavior is now different.
        assertEquals("[eeee, ffff]", vertProps.get("source").toString());

        newProps = new HashMap<String, Object>();
        List<String> sourceList = new ArrayList<String>();
        sourceList.add("gggg");
        sourceList.add("hhhh");
        newProps.put("source", sourceList);
        conn.updateVertex(id, newProps);

        vertProps = conn.getVertByID(id);
        //assertEquals("[aaaa, bbbb, cccc, dddd, eeee, ffff, gggg, hhhh]", vertProps.get("source").toString());//NB: behavior is now different.
        assertEquals("[gggg, hhhh]", vertProps.get("source").toString());

        newProps = new HashMap<String, Object>();
        String[] sourceArr = new String[]{"hhhh", "iiii"};
        newProps.put("source", sourceArr);
        conn.updateVertex(id, newProps);

        vertProps = conn.getVertByID(id);
        //assertEquals("[aaaa, bbbb, cccc, dddd, eeee, ffff, gggg, hhhh, iiii]", vertProps.get("source").toString());//NB: behavior is now different.
        assertEquals(sourceArr, vertProps.get("source"));

        //conn.removeAllVertices();
    }

    /**
     * creates a vertex of high reverse degree, and one of low degree, and searches for the edge(s) between them.
     */
    public void testHighForwardDegreeVerts()
    {
        //		InMemoryDBConnection conn = new InMemoryDBConnection();

        //conn.removeAllVertices();

        String vert1 = "{" +
                "\"name\":\"/usr/local/something\"," +
                "\"_type\":\"vertex\","+
                "\"source\":\"test\","+
                "\"vertexType\":\"software\""+
                "}";
        String vert2 = "{" +
                "\"name\":\"11.11.11.11:1111_to_22.22.22.22:1\"," +
                "\"_type\":\"vertex\","+
                "\"source\":\"test\","+
                "\"vertexType\":\"flow\""+
                "}";
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert1)));
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert2)));

        //find node ids
        String id = conn.getVertIDByName("/usr/local/something");
        String id2 = conn.getVertIDByName("11.11.11.11:1111_to_22.22.22.22:1");

        //There should be no edge between them
        assertEquals(0, conn.getEdgeCountByRelation(id, id2, "hasFlow"));
        assertEquals(0, conn.getEdgeCountByRelation(id2, id, "hasFlow"));

        //now add an edge
        //conn.addEdge(inVid, outVid, label);
        conn.addEdge(id2, id, "hasFlow");

        //Confirm the edge between them
        int c1 = conn.getEdgeCountByRelation(id2, id, "hasFlow");
        int c2 = conn.getEdgeCountByRelation(id, id2, "hasFlow");
        assertEquals(1, c1);
        assertEquals(0, c2);

        for(int i=2; i<800; i++){
            String currentVert = "{" +
                    "\"name\":\"11.11.11.11:1111_to_22.22.22.22:" + i + "\"," +
                    "\"_type\":\"vertex\","+
                    "\"source\":\"test\","+
                    "\"vertexType\":\"flow\""+
                    "}";
            String currentId = conn.addVertex(conn.jsonVertToMap(new JSONObject(currentVert)));

            //conn.addEdge(inVid, outVid, label);
            conn.addEdge(currentId, id, "hasFlow");
        }

        //find node ids
        id = conn.getVertIDByName("/usr/local/something");
        id2 = conn.getVertIDByName("11.11.11.11:1111_to_22.22.22.22:1");

        //Confirm the edge between them
        assertEquals(1, conn.getEdgeCountByRelation(id2, id, "hasFlow"));
        assertEquals(0, conn.getEdgeCountByRelation(id, id2, "hasFlow"));
    }

    /**
     * creates a vertex of high reverse degree, and one of low degree, and searches for the edge(s) between them.
     */
    public void testHighReverseDegreeVerts()
    {
        //		InMemoryDBConnection conn = new InMemoryDBConnection();

        //conn.removeAllVertices();

        String vert1 = "{" +
                "\"name\":\"11.11.11.11:1111\"," +
                "\"_type\":\"vertex\","+
                "\"source\":\"test\","+
                "\"vertexType\":\"address\""+
                "}";
        String vert2 = "{" +
                "\"name\":\"11.11.11.11\"," +
                "\"_type\":\"vertex\","+
                "\"source\":\"test\","+
                "\"vertexType\":\"IP\""+
                "}";
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert1)));
        conn.addVertex(conn.jsonVertToMap(new JSONObject(vert2)));

        //find node ids
        String id = conn.getVertIDByName("11.11.11.11:1111");
        String id2 = conn.getVertIDByName("11.11.11.11");

        //There should be no edge between them
        assertEquals(0, conn.getEdgeCountByRelation(id, id2, "hasIP"));
        assertEquals(0, conn.getEdgeCountByRelation(id2, id, "hasIP"));

        //conn.addEdge(inVid, outVid, label);
        conn.addEdge(id2, id, "hasIP");

        //find node ids
        id = conn.getVertIDByName("11.11.11.11:1111");
        id2 = conn.getVertIDByName("11.11.11.11");

        //Confirm the edge between them
        assertEquals(1, conn.getEdgeCountByRelation(id2, id, "hasIP"));
        assertEquals(0, conn.getEdgeCountByRelation(id, id2, "hasIP"));

        for(int i=1200; i<2000; i++){
            String currentVert = "{" +
                    "\"name\":\"11.11.11.11:" + i + "\"," +
                    "\"_type\":\"vertex\","+
                    "\"source\":\"test\","+
                    "\"vertexType\":\"address\""+
                    "}";
            String currentId = conn.addVertex(conn.jsonVertToMap(new JSONObject(currentVert)));

            //conn.addEdge(inVid, outVid, label);
            conn.addEdge(id2, currentId, "hasIP");
        }

        //find node ids
        id = conn.getVertIDByName("11.11.11.11:1111");
        id2 = conn.getVertIDByName("11.11.11.11");

        //Confirm the edge between them
        assertEquals(1, conn.getEdgeCountByRelation(id2, id, "hasIP"));
        assertEquals(0, conn.getEdgeCountByRelation(id, id2, "hasIP"));
    }

    /**
     * creates a small set of vertices, searches this set by constraints on properties
     */
    public void testConstraints()
    {
        //		InMemoryDBConnection conn = new InMemoryDBConnection();
        Map<String, Object> vert;
        List<DBConstraint> constraints;
        String id;
        List<String> ids;
        List<String> expectedIds;

        vert = new HashMap<String, Object>();
        vert.put("name", "aaa_5");
        vert.put("aaa", 5);
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "aaa_6");
        vert.put("aaa", 6);
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "aaa_7");
        vert.put("aaa", 7);
        conn.addVertex(vert);

        DBConstraint c1 = conn.getConstraint("aaa", Condition.eq, new Integer(5) );
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        assertEquals(1, ids.size());
        assertTrue(ids.contains(conn.getVertIDByName("aaa_5")));
        //System.out.println("Found " + ids.size() + " matching verts with aaa == 5");

        c1 = conn.getConstraint("aaa", Condition.neq, new Integer(5) );
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("aaa_6"));
        expectedIds.add(conn.getVertIDByName("aaa_7"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with aaa != 5");

        c1 = conn.getConstraint("aaa", Condition.gt, new Integer(5) );
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("aaa_6"));
        expectedIds.add(conn.getVertIDByName("aaa_7"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with aaa > 5");

        c1 = conn.getConstraint("aaa", Condition.gte, new Integer(5) );
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("aaa_5"));
        expectedIds.add(conn.getVertIDByName("aaa_6"));
        expectedIds.add(conn.getVertIDByName("aaa_7"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(3, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with aaa >= 5");

        c1 = conn.getConstraint("aaa", Condition.lt, new Integer(6) );
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        assertTrue(ids.contains(conn.getVertIDByName("aaa_5")));
        assertEquals(1, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with aaa < 6");

        c1 = conn.getConstraint("aaa", Condition.lte, new Integer(6) );
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("aaa_5"));
        expectedIds.add(conn.getVertIDByName("aaa_6"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with aaa <= 6");

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_4_5_6");
        vert.put("bbb", (new int[] {4,5,6}) );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_Integer_4_5_6");
        vert.put("bbb", (new Integer[] {new Integer(4), new Integer(5), new Integer(6)}) );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_7_8_9");
        vert.put("bbb", (new int[] {7,8,9}) );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_5_6_7_8");
        vert.put("bbb", (new int[] {5,6,7,8}) );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_asdf");
        vert.put("bbb", "asdf" );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_asdf4.222");
        vert.put("bbb", "asdf4.222" );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_55");
        vert.put("bbb", 55 );
        conn.addVertex(vert);

        vert = new HashMap<String, Object>();
        vert.put("name", "bbb_101_102_103");
        vert.put("bbb", (new double[] {101, 102, 103.0}) );
        conn.addVertex(vert);

        c1 = conn.getConstraint("bbb", Condition.in, 4);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("bbb_4_5_6"));
        expectedIds.add(conn.getVertIDByName("bbb_Integer_4_5_6"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 4 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, new Integer(4));
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("bbb_4_5_6"));
        expectedIds.add(conn.getVertIDByName("bbb_Integer_4_5_6"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with (Integer)4 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, 4.0);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        assertEquals(0, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 4.0 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, 4.2);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        assertEquals(0, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 4.2 in bbb");

        c1 = conn.getConstraint("bbb", Condition.notin, 4);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("aaa_5"));
        expectedIds.add(conn.getVertIDByName("aaa_6"));
        expectedIds.add(conn.getVertIDByName("aaa_7"));
        expectedIds.add(conn.getVertIDByName("bbb_7_8_9"));
        expectedIds.add(conn.getVertIDByName("bbb_5_6_7_8"));
        expectedIds.add(conn.getVertIDByName("bbb_asdf"));
        expectedIds.add(conn.getVertIDByName("bbb_asdf4.222"));
        expectedIds.add(conn.getVertIDByName("bbb_55"));
        expectedIds.add(conn.getVertIDByName("bbb_101_102_103"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(9, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts without 4 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, 5);
        DBConstraint c2 = conn.getConstraint("bbb", Condition.in, 7);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        constraints.add(c2);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("bbb_5_6_7_8"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(1, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 5 and 7 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, "asdf");
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("bbb_asdf"));
        expectedIds.add(conn.getVertIDByName("bbb_asdf4.222"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 'a' in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, "as");
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("bbb_asdf"));
        expectedIds.add(conn.getVertIDByName("bbb_asdf4.222"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(2, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with \"as\" in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, 101);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        assertEquals(0, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 101 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, 103);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        assertEquals(0, ids.size());
        //System.out.println("Found " + ids.size() + " matching verts with 103 in bbb");

        c1 = conn.getConstraint("bbb", Condition.in, 103.0);
        constraints = new LinkedList<DBConstraint>();
        constraints.add(c1);
        ids = conn.getVertIDsByConstraints(constraints);
        expectedIds = new LinkedList<String>();
        expectedIds.add(conn.getVertIDByName("bbb_101_102_103"));
        assertTrue(ids.containsAll(expectedIds));
        assertEquals(1, ids.size());
        //		System.out.println("Found " + ids.size() + " matching verts with 103.0 in bbb");

    }


}


