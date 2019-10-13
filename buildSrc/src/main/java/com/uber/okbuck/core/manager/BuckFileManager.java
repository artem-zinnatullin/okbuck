package com.uber.okbuck.core.manager;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.errorprone.annotations.Var;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.extension.RuleOverridesExtension;
import com.uber.okbuck.template.common.GeneratedHeader;
import com.uber.okbuck.template.common.LoadStatements;
import com.uber.okbuck.template.core.Rule;
import kotlin.text.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuckFileManager {

  private static final byte[] NEWLINE = System.lineSeparator().getBytes(StandardCharsets.UTF_8);
  private static final String RES_GLOB = "res_glob";
  private static final String SUBDIR_GLOB = "subdir_glob";

  private final RuleOverridesExtension ruleOverridesExtension;

  public BuckFileManager(RuleOverridesExtension ruleOverridesExtension) {
    this.ruleOverridesExtension = ruleOverridesExtension;
  }

  public void writeToBuckFile(List<Rule> rules, File buckFile) {
    this.writeToBuckFile(rules, buckFile, TreeMultimap.create());
  }

  public void writeToBuckFile(
      @Var List<Rule> rules, File buckFile, Multimap<String, String> extraLoadStatements) {
    if (!rules.isEmpty()) {
      Multimap<String, String> loadStatements = getLoadStatements(rules);
      loadStatements.putAll(extraLoadStatements);
      File parent = buckFile.getParentFile();
      if (!parent.exists() && !parent.mkdirs()) {
        throw new IllegalStateException("Couldn't create dir: " + parent);
      }

      if (rules.stream().noneMatch(it -> it.ruleType().contains("android_binary"))) {
        // Remove android_build_config targets from non-app packages.
        rules = rules
            .stream()
            .filter(it -> !it.ruleType().contains("android_build_config"))
            .collect(Collectors.toList());

        rules = rules
            .stream()
            .map(rule -> {
                  rule.deps = (Collection) rule
                      .deps
                      .stream()
                      .filter(dep -> !((String) dep).startsWith(":build_config_"))
                      .collect(Collectors.toList());

                  return rule;
                }
            )
            .collect(Collectors.toList());
      }

      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream(8 * 1024 * 1024);

        GeneratedHeader.template().render(os);
        if (!loadStatements.isEmpty()) {
          LoadStatements.template(writableLoadStatements(loadStatements)).render(os);
        }

        for (int index = 0; index < rules.size(); index++) {
          // Don't add a new line before the first rule
          if (index != 0) {
            os.write(NEWLINE);
          }
          rules.get(index).render(os);
        }

        String buckFileContents = os.toString("UTF-8");

        try {
          StringBuilder sb = new StringBuilder(buckFileContents.length());

          for (String line : Splitter.onPattern("\\r?\\n").split(buckFileContents)) {
            // Remove default Java source & target levels.
            if (!line.contains("source = '8'")
                && !line.contains("target = '8'")
                && !line.contains("disable_lint = True")) {
              sb.append(line);
              sb.append("\n");
            }
          }

          FileUtils.write(buckFile, sb.toString(), Charsets.UTF_8);
        } catch (IOException e) {
          throw new RuntimeException("Error writing " + buckFile.getAbsolutePath(), e);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Couldn't create the buck file", e);
      }
    }
  }

  private Multimap<String, String> getLoadStatements(List<Rule> rules) {
    Multimap<String, String> loadStatements = TreeMultimap.create();
    Map<String, RuleOverridesExtension.OverrideSetting> overrides =
        ruleOverridesExtension.getOverrides();
    for (Rule rule : rules) {
      // Android resource template requires res_glob function from buck defs
      if (RuleType.ANDROID_MODULE.getBuckName().equals(rule.ruleType())
          || RuleType.KOTLIN_ANDROID_MODULE.getBuckName().equals(rule.ruleType())) {
        loadStatements.put(OkBuckGradlePlugin.OKBUCK_TARGETS_TARGET, RES_GLOB);
        loadStatements.put(OkBuckGradlePlugin.OKBUCK_TARGETS_TARGET, SUBDIR_GLOB);
      }
      if (overrides.containsKey(rule.ruleType())) {
        RuleOverridesExtension.OverrideSetting setting = overrides.get(rule.ruleType());
        loadStatements.put(setting.getImportLocation(), setting.getNewRuleName());
        rule.ruleType(setting.getNewRuleName());
      }
    }
    return loadStatements;
  }

  private static List<String> writableLoadStatements(Multimap<String, String> loadStatements) {
    return loadStatements
        .asMap()
        .entrySet()
        .stream()
        .map(
            loadStatement ->
                Stream.concat(Stream.of(loadStatement.getKey()), loadStatement.getValue().stream())
                    .map(statement -> "'" + statement + "'")
                    .collect(Collectors.joining(", ", "load(", ")")))
        .collect(Collectors.toList());
  }
}
