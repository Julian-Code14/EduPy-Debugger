package de.code14.edupydebugger.core.publish;

import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ValueDTO;
import de.code14.edupydebugger.server.dto.VariableDTO;
import de.code14.edupydebugger.server.dto.VariablesPayload;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

public class PayloadPublisherTests {

    @Test
    public void publishVariablesWithSnippet_setsPreviewAndFullForComposite() {
        // Arrange: one composite variable backed by an ObjectInfo with long attribute
        VariableDTO dto = new VariableDTO();
        dto.id = "1";
        dto.names = Collections.singletonList("obj");
        dto.pyType = "MyClass"; // non-primitive
        dto.scope = "local";
        dto.value = new ValueDTO(); // kind/repr will be set by publisher

        Map<String, ObjectInfo> objects = new HashMap<>();
        List<AttributeInfo> attrs = new ArrayList<>();
        attrs.add(new AttributeInfo("name", "str", "abcdefghijklmnopqrstuvwxyz", "public"));
        attrs.add(new AttributeInfo("count", "int", "42", "public"));
        objects.put("1", new ObjectInfo(Collections.singletonList("obj:MyClass"), attrs));

        List<VariableDTO> variables = new ArrayList<>();
        variables.add(dto);

        try (MockedStatic<DebugServerEndpoint> endpoint = mockStatic(DebugServerEndpoint.class)) {
            ArgumentCaptor<VariablesPayload> cap = ArgumentCaptor.forClass(VariablesPayload.class);
            endpoint.when(() -> DebugServerEndpoint.publishVariables(any(VariablesPayload.class))).thenAnswer(inv -> null);

            // Act
            PayloadPublisher.publishVariablesWithSnippet(variables, objects);

            // Assert
            endpoint.verify(() -> DebugServerEndpoint.publishVariables(cap.capture()), times(1));
            VariablesPayload payload = cap.getValue();
            assertNotNull(payload);
            assertEquals(1, payload.variables.size());
            VariableDTO got = payload.variables.get(0);
            assertEquals("composite", got.value.kind);
            assertNotNull(got.value.repr);
            assertNotNull(got.value.full);
            // Preview is truncated to 20 chars + ellipsis
            assertTrue(got.value.repr.contains("name: abcdefghijklmnopqrst [...]") || got.value.repr.startsWith("name: abcdefghijklmnopqrst"));
            // Full contains the complete attribute value
            assertTrue(got.value.full.contains("name: abcdefghijklmnopqrstuvwxyz"));
            assertTrue(got.value.full.contains("count: 42"));
        }
    }
}

