package com.rundeck.plugin.resources.puppetdb;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rundeck.plugin.resources.puppetdb.client.PuppetAPI;
import com.rundeck.plugin.resources.puppetdb.client.model.Fact;
import com.rundeck.plugin.resources.puppetdb.client.model.Node;
import com.rundeck.plugin.resources.puppetdb.client.model.NodeClass;
import com.rundeck.plugin.resources.puppetdb.client.model.PuppetDBNode;
import org.junit.Before;
import org.junit.Test;

public class Mapper_UT {

    // class under test
    Mapper mapper;

    // support
    Gson gson;
    PuppetAPI testApi;
    Map<String, Object> mapping;

    @Before
    public void before() {
        this.mapper = new Mapper();
        this.gson = new Gson();
        this.testApi = testApi();
        this.mapping = getMapping();
    }

    @Test
    public void test_known_mapping() {
        final List<PuppetDBNode> nodesWithFacts = testApi.getNodes()
                .stream()
                .map(testApi::getNodeWithFacts)
                .collect(toList());

        final PuppetDBNode puppetDBNode = nodesWithFacts.get(0);

        final Optional<INodeEntry> maybeNode = mapper.apply(puppetDBNode, mapping);
        final INodeEntry nodeEntry = maybeNode.orElse(null);

        assertTrue("maybeNode should be present", maybeNode.isPresent());
        assertEquals("nodeEntry.hostname should be 100.112.162.79", "100.112.162.79", nodeEntry.getHostname());
        assertEquals("nodeEntry.username should be username", "username", nodeEntry.getUsername());
    }

    public Map<String, Object> getMapping() {
        final Type mappingType = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(readFile("knownMapping.json"), mappingType);
    }

    public PuppetAPI testApi() {
        return new PuppetAPI() {
            @Override
            public List<Node> getNodes() {
                return gson.fromJson(readFile("nodes.json"), Node.LIST);
            }

            @Override
            public List<Fact> getFactsForNode(final Node node) {
                return gson.fromJson(readFile("facts.json"), Fact.LIST);
            }

            @Override
            public List<NodeClass> getClassesForNode(final Node node) {
                return emptyList();
            }
        };
    }

    public String readFile(final String name) {
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(name);
             final Scanner scanner = new Scanner(inputStream)) {
            scanner.useDelimiter("\\Z");
            return scanner.next();
        } catch (Exception ex) {
            System.err.println("can't read file: " + name);
            ex.printStackTrace(System.err);
        }

        return "";
    }
}
