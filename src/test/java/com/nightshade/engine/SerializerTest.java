package com.nightshade.engine;

import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {

    @Test
    void applyMappingSkipsStringsCommentsAndDotCalls() {
        List<String> lines = List.of(
            "public class Test {",
            "  void run() {",
            "    String s = \"count\"; int count = 1; // count",
            "    myObj.process();",
            "  }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Map<String, String> mapping = Map.of("count", "v_aaaaaaa", "process", "v_bbbbbbb");

        Serializer serializer = new Serializer();
        List<String> out = serializer.applyMapping(source, mapping);
        String joined = String.join("\n", out);

        assertAll("applyMapping invariants",
            () -> assertTrue(joined.contains("\"count\"")),
            () -> assertTrue(joined.contains("// count")),
            () -> assertTrue(joined.contains("int v_aaaaaaa = 1")),
            () -> assertTrue(joined.contains("myObj.process()"))
        );
    }
}
