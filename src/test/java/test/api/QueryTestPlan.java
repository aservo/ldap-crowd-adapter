package test.api;

import com.aservo.ldap.adapter.api.database.Row;
import com.aservo.ldap.adapter.api.database.exception.UnknownColumnException;
import com.aservo.ldap.adapter.api.entity.ColumnNames;
import com.aservo.ldap.adapter.api.entity.EntityType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


public interface QueryTestPlan {

    interface Element {

        boolean isIgnored();

        String getDescription();

        String getBase();

        String getFilter();

        String getScope();

        Iterable<Row> getExpectations();
    }

    static List<Element> createQueryTestPlan(File testPlanFile)
            throws Exception {

        Gson gson = new Gson();
        List<Element> testPlanElements = new LinkedList<>();

        JsonObject objectNode =
                gson.fromJson(
                        new InputStreamReader(new FileInputStream(testPlanFile), StandardCharsets.UTF_8),
                        JsonObject.class);

        JsonArray tests = objectNode.getAsJsonArray("query_tests");

        for (JsonElement test : tests) {

            final boolean ignored = test.getAsJsonObject().get("ignored").getAsBoolean();
            final String description = test.getAsJsonObject().get("description").getAsString();
            final String base = test.getAsJsonObject().get("base").getAsString();
            final String filter = test.getAsJsonObject().get("filter").getAsString();
            final String scope = test.getAsJsonObject().get("scope").getAsString();
            final HashSet<Row> expectations = new HashSet<>();

            testPlanElements.add(new Element() {

                public boolean isIgnored() {

                    return ignored;
                }

                public String getDescription() {

                    return description;
                }

                public String getBase() {

                    return base;
                }

                public String getFilter() {

                    return filter;
                }

                public String getScope() {

                    return scope;
                }

                public Iterable<Row> getExpectations() {

                    return expectations;
                }
            });

            JsonArray expectationsNode = test.getAsJsonObject().getAsJsonArray("expectations");

            for (JsonElement element : expectationsNode) {

                EntityType entityType = EntityType.fromString(element.getAsJsonObject().get("type").getAsString());
                List<String> names = new LinkedList<>();

                JsonElement idElement = element.getAsJsonObject().get("id");
                JsonElement idsElement = element.getAsJsonObject().get("ids");

                if (idElement != null && idsElement == null)
                    names.add(idElement.getAsString());
                else if (idElement == null && idsElement != null)
                    idsElement.getAsJsonArray().forEach(x -> names.add(x.getAsString()));
                else
                    throw new IllegalArgumentException("Expect exclusively the key 'id' or 'ids' for expectations.");

                for (String name : names) {

                    expectations.add(new Row() {

                        public <T> T apply(String columnName, Class<T> clazz) {

                            if (columnName.equals(ColumnNames.TYPE))
                                return (T) entityType.toString();

                            if (columnName.equals(ColumnNames.ID))
                                return (T) name.toLowerCase();

                            if (columnName.equals(ColumnNames.NAME) && entityType == EntityType.GROUP)
                                return (T) name;

                            if (columnName.equals(ColumnNames.USERNAME) && entityType == EntityType.USER)
                                return (T) name;

                            throw new UnknownColumnException("Cannot find column " + columnName + " for entity.");
                        }
                    });
                }
            }
        }

        return testPlanElements;
    }
}
