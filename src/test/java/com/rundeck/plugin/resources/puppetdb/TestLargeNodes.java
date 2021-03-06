package com.rundeck.plugin.resources.puppetdb;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rundeck.plugin.resources.puppetdb.client.PuppetAPI;
import com.rundeck.plugin.resources.puppetdb.client.PuppetDB;
import com.rundeck.plugin.resources.puppetdb.client.model.*;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by greg on 3/7/16.
 */
public class TestLargeNodes {

    Mapper mapper;

    Gson gson;
    PuppetAPI testApi;
    Map<String, Object> mapping;

    @Before
    public void before() {
        this.mapper = new Mapper(Optional.<String>absent());
        this.gson = new Gson();
        this.testApi = testApi();
    }

    @Test
    public void test_known_mapping() {
        PuppetDB db = new PuppetDB(testApi);
        this.mapping = getMapping("simple/known_mapping.json");
        final ImmutableList<PuppetDBNode> nodesWithFacts = FluentIterable.from(testApi.getNodes(null))
                                                                         .transform(db.queryNode())
                                                                         .toList();

        final PuppetDBNode puppetDBNode = nodesWithFacts.get(0);

        final Optional<INodeEntry> maybeNode = mapper.apply(puppetDBNode, mapping);
        final INodeEntry nodeEntry = maybeNode.orNull();

        assertTrue("maybeNode should be present", maybeNode.isPresent());
        assertEquals("nodeEntry.hostname should be 100.112.162.79", "100.112.162.79", nodeEntry.getHostname());
        assertEquals("nodeEntry.username should be username", "username", nodeEntry.getUsername());
    }

    @Test
    public void test_known_mapping_with_missing_property() {
        PuppetDB db = new PuppetDB(testApi);
        this.mapping = getMapping("simple/known_mapping_with_missing_property.json");
        long now = System.currentTimeMillis();
        final ImmutableList<PuppetDBNode> nodesWithFacts = FluentIterable.from(testApi.getNodes(null))
                                                                         .transform(db.queryNode())
                                                                         .toList();

        System.out.println("dur " + (System.currentTimeMillis() - now));
        final PuppetDBNode puppetDBNode = nodesWithFacts.get(0);

        final Optional<INodeEntry> maybeNode = mapper.apply(puppetDBNode, mapping);
        final INodeEntry nodeEntry = maybeNode.orNull();

        assertTrue("maybeNode should be present", maybeNode.isPresent());
        assertEquals("nodeEntry.hostname should be 100.112.162.79", "100.112.162.79", nodeEntry.getHostname());
        assertEquals("nodeEntry.username should be username", "username", nodeEntry.getUsername());
        for (Map.Entry<String, String> e : nodeEntry.getAttributes().entrySet()) {
            assertTrue(e != null);
            assertTrue(e.getKey() != null);
            assertTrue(e.getValue() != null);
        }
        for (Object o : nodeEntry.getTags()) {
            assertTrue(o != null);
        }

        assertTrue(nodeEntry.getDescription() != null);
        //assertTrue(nodeEntry.getFrameworkProject() != null);
        assertTrue(nodeEntry.getNodename() != null);
        assertTrue(nodeEntry.getOsArch() != null);
        //assertTrue(nodeEntry.getOsFamily() != null);
        assertTrue(nodeEntry.getOsName() != null);
        assertTrue(nodeEntry.getOsVersion() != null);
        assertTrue(nodeEntry.getUsername() != null);
        assertTrue(nodeEntry.extractHostname() != null);
        //assertTrue(nodeEntry.extractPort() != null);
        assertTrue(nodeEntry.extractUserName() != null);
    }

    public Map<String, Object> getMapping(final String fileName) {
        final Type mappingType = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(readFile(fileName), mappingType);
    }

    public PuppetAPI testApi() {
        return new PuppetAPI() {
            @Override
            public List<NodeFact> getFactSet(final Set<String> facts, final String userQuery) {
                return null;
            }

            @Override
            public List<CertNodeClass> getClassesForAllNodes(final String userQuery) {
                return null;
            }

            @Override
            public List<Node> getNodes(final String userQuery) {
//                try {
//                    System.out.println("getNodes...");
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                return gson.fromJson(readFile("large/nodes.json"), Node.LIST);
            }

            @Override
            public List<Fact> getFactsForNode(final Node node) {
//                try {
//                    System.out.println("getFactsForNode...");
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                return gson.fromJson(readFile("simple/facts.json"), Fact.LIST);
            }

            @Override
            public List<NodeClass> getClassesForNode(final Node node) {
                return gson.fromJson(readFile("simple/classes.json"), NodeClass.LIST);
            }
        };
    }

    public String readFile(final String name) {
        try (
                final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(name);
                final Scanner scanner = new Scanner(inputStream)
        ) {
            scanner.useDelimiter("\\Z");
            return scanner.next();
        } catch (Exception ex) {
            System.err.println("can't read file: " + name);
            ex.printStackTrace(System.err);
        }

        return "";
    }

}
