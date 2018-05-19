/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.matching.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.graphhopper.http.GraphHopperServerConfiguration;
import com.graphhopper.http.WebHelper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.VirtualEdgeIteratorState;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint3D;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Karich
 */
public class MapMatchingResourceTest {

    private static final String DIR = "../target/mapmatchingtest";

    private static final GraphHopperServerConfiguration config = new GraphHopperServerConfiguration();

    static {
        config.getGraphHopperConfiguration().merge(new CmdArgs().
                put("graph.flag_encoders", "car").
                put("prepare.ch.weightings", "no").
                put("datareader.file", "../map-data/leipzig_germany.osm.pbf").
                put("graph.location", DIR));
    }

    @ClassRule
    public static final DropwizardAppRule<GraphHopperServerConfiguration> app = new DropwizardAppRule(MapMatchingApplication.class, config);

    @AfterClass
    public static void cleanUp() {
        Helper.removeDir(new File(DIR));
    }

    @Test
    public void testGPX() throws Exception {
        String xmlStr = Helper.isToString(getClass().getResourceAsStream("tour2-with-loop.gpx"));
        final Response response = app.client().target("http://localhost:8080/match").request().buildPost(Entity.xml(xmlStr)).invoke();
        assertEquals(200, response.getStatus());
        JsonNode json = response.readEntity(JsonNode.class);
        JsonNode path = json.get("paths").get(0);

        assertEquals(5, path.get("instructions").size());
        assertEquals(5, WebHelper.decodePolyline(path.get("points").asText(), 10, false).size());
        assertEquals(106.15, path.get("time").asLong() / 1000f, 0.1);
        assertEquals(106.15, json.get("map_matching").get("time").asLong() / 1000f, 0.1);
        assertEquals(811.56, path.get("distance").asDouble(), 1);
        assertEquals(811.56, json.get("map_matching").get("distance").asDouble(), 1);
    }

    @Test
    public void testEmptyGPX() throws Exception {
        // empty xml
        String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" creator=\"Graphhopper\" version=\"1.1\" xmlns:gh=\"https://graphhopper.com/public/schema/gpx/1.1\"></gpx>";
        final Response response = app.client().target("http://localhost:8080/match").request().buildPost(Entity.xml(xmlStr)).invoke();
        assertEquals(200, response.getStatus());
        JsonNode json = response.readEntity(JsonNode.class);
        JsonNode path = json.get("paths").get(0);
        assertEquals(0, WebHelper.decodePolyline(path.get("points").asText(), 10, false).size());
    }

    private List<EdgeMatch> getEdgeMatch() {
        List<EdgeMatch> list = new ArrayList<>();
        list.add(new EdgeMatch(getEdgeInterator(), getGpxExtension()));
        return list;
    }

    private List<GPXExtension> getGpxExtension() {
        List<GPXExtension> list = new ArrayList<>();
        QueryResult queryResult1 = new QueryResult(-3.4445, -38.9990) {
            @Override
            public GHPoint3D getSnappedPoint() {
                return new GHPoint3D(-3.4446, -38.9996, 0);
            }
        };
        QueryResult queryResult2 = new QueryResult(-3.4445, -38.9990) {
            @Override
            public GHPoint3D getSnappedPoint() {
                return new GHPoint3D(-3.4449, -38.9999, 0);
            }
        };

        list.add(new GPXExtension(new GPXEntry(-3.4446, -38.9996, 100000), queryResult1));
        list.add(new GPXExtension(new GPXEntry(-3.4448, -38.9999, 100001), queryResult2));
        return list;
    }

    private EdgeIteratorState getEdgeInterator() {
        PointList pointList = new PointList();
        pointList.add(-3.4445, -38.9990);
        pointList.add(-3.5550, -38.7990);
        return new VirtualEdgeIteratorState(0, 0, 0, 1, 10, 0, "test of iterator", pointList);
    }

    @Test
    public void shouldCreateBasicStructure() {
        JsonNode jsonObject = MapMatchingResource.convertToTree(new MatchResult(getEdgeMatch()), false, false);
        JsonNode route = jsonObject.get("diary").get("entries").get(0);
        JsonNode link = route.get("links").get(0);
        JsonNode geometry = link.get("geometry");
        assertEquals("geometry should have type", "LineString", geometry.get("type").asText());
        assertEquals("geometry should have coordinates", "LINESTRING (-38.999 -3.4445, -38.799 -3.555)", geometry.get("coordinates").asText());

        assertEquals("wpts[0].timestamp should exists", 100000l, link.get("wpts").get(0).get("timestamp").asLong());
        assertEquals("wpts[0].y should exists", "-3.4446", link.get("wpts").get(0).get("y").asText());
        assertEquals("wpts[0].x should exists", "-38.9996", link.get("wpts").get(0).get("x").asText());

        assertEquals("wpts[1].timestamp should exists", 100001l, link.get("wpts").get(1).get("timestamp").asLong());
        assertEquals("wpts[1].y should exists", "-3.4449", link.get("wpts").get(1).get("y").asText());
        assertEquals("wpts[1].x should exists", "-38.9999", link.get("wpts").get(1).get("x").asText());
    }
}
