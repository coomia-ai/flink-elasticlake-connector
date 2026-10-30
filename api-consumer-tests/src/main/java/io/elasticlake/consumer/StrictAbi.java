package io.elasticlake.consumer;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

final class StrictAbi {
  static String surface(Class<?> root) {
    var types = new ArrayList<Class<?>>();
    types.add(root);
    types.addAll(Arrays.asList(root.getDeclaredClasses()));
    var lines = new TreeSet<String>();
    for (Class<?> type : types) {
      if (!Modifier.isPublic(type.getModifiers())) continue;
      lines.add(type.toGenericString());
      if (type.isRecord()) {
        var components = type.getRecordComponents();
        for (int index = 0; index < components.length; index++) {
          var component = components[index];
          lines.add(
              type.getName()
                  + " component["
                  + index
                  + "] "
                  + component.getName()
                  + ":"
                  + component.getGenericType().getTypeName());
        }
      }
      if (type.isEnum())
        lines.add(type.getName() + " enum=" + Arrays.toString(type.getEnumConstants()));
      if (type.isSealed())
        lines.add(
            type.getName()
                + " permits="
                + Arrays.toString(
                    Arrays.stream(type.getPermittedSubclasses())
                        .map(Class::getName)
                        .sorted()
                        .toArray()));
      for (var method : type.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) lines.add(method.toGenericString());
      }
      for (var constructor : type.getDeclaredConstructors()) {
        if (Modifier.isPublic(constructor.getModifiers())) lines.add(constructor.toGenericString());
      }
      for (var field : type.getDeclaredFields()) {
        if (Modifier.isPublic(field.getModifiers())) lines.add(field.toGenericString());
      }
    }
    return String.join("\n", lines) + "\n";
  }

  public static void main(String[] args) throws Exception {
    if (Runtime.version().feature() != 17) throw new AssertionError("Actual Java17 required");
    System.out.print(surface(Class.forName("io.elasticlake.connector.api.v3.ConnectorApi")));
  }
}
