package com.uber.okbuck.composer.jvm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.base.SourceSetType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.jvm.JvmRule;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class JvmTestRuleComposer extends JvmBuckRuleComposer {

  private static final ImmutableSet<String> JAVA_TEST_LABELS = ImmutableSet.of();

  private JvmTestRuleComposer() {
    // no instance
  }

  public static Rule compose(JvmTarget target, RuleType ruleType) {
    List<String> deps =
        ImmutableList.<String>builder()
            .add(":" + src(target))
            .addAll(external(target.getExternalDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetDeps(SourceSetType.TEST)))
            .build();

    Set<String> aptDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalAptDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetAptDeps(SourceSetType.TEST)))
            .build();

    Set<String> providedDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalProvidedDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetProvidedDeps(SourceSetType.TEST)))
            .build();

    List<String> preFinalJvmArgs = target.getTestOptions().getJvmArgs();
    List<String> finalJvmArgs;

    // If vm_args are what we have defaults via macros, don't render them in BUILD files.
    if (preFinalJvmArgs.size() == 5
        && preFinalJvmArgs.contains("-Dfile.encoding=UTF-8")
        && preFinalJvmArgs.contains("-Duser.country=US")
        && preFinalJvmArgs.contains("-Duser.language=en")
        && preFinalJvmArgs.contains("-Duser.variant")
        && preFinalJvmArgs.contains("-ea")) {
      finalJvmArgs = ImmutableList.of();
    } else {
      finalJvmArgs = preFinalJvmArgs;
    }

    Set<String> srcs;
    List<String> exts;

    if (shouldGenerateSrcs(target)) {
      srcs = target.getTest().getSources();
      exts = ruleType.getProperties();
    } else {
      // Do not render srcs on non-app targets.
      srcs = Collections.emptySet();
      exts = Collections.emptyList();
    }

    return new JvmRule()
        .srcs(srcs)
        .exts(exts)
        .apPlugins(getApPlugins(target.getTestApPlugins()))
        .aptDeps(aptDeps)
        .providedDeps(providedDeps)
        .resources(target.getTest().getJavaResources())
        .sourceCompatibility(target.getSourceCompatibility())
        .targetCompatibility(target.getTargetCompatibility())
        .options(target.getTest().getCustomOptions())
        .jvmArgs(finalJvmArgs)
        .env(target.getTestOptions().getEnv())
        .ruleType(ruleType.getBuckName())
        .defaultVisibility()
        .deps(deps)
        .name(test(target))
        .labels(JAVA_TEST_LABELS)
        .extraBuckOpts(target.getExtraOpts(ruleType));
  }
}
