package org.plantcausal.report.json;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
/** Undefined/not-applicable scalar diagnostics are JSON null, never string "NaN". */
public final class FiniteDoubleSerializer extends JsonSerializer<Double> {
 @Override public void serialize(Double value,JsonGenerator generator,SerializerProvider provider) throws IOException {
  if(value==null||!Double.isFinite(value))generator.writeNull();else generator.writeNumber(value);
 }
}
