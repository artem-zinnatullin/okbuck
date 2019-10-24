package com.uber.okbuck.composer.android;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.manager.RobolectricManager;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.base.SourceSetType;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.template.android.AndroidTestRule;
import com.uber.okbuck.template.core.Rule;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AndroidTestRuleComposer extends AndroidBuckRuleComposer {

  private static final ImmutableSet<String> ANDROID_TEST_LABELS =
      ImmutableSet.of();

  private AndroidTestRuleComposer() {
    // no instance
  }

  public static Rule compose(
      AndroidLibTarget target,
      @Nullable String manifestRule,
      List<String> deps,
      List<String> aidlRuleNames,
      @Nullable String appClass) {

    List<String> testDeps = new ArrayList<>(deps);
    testDeps.add(":" + src(target));
    testDeps.addAll(external(target.getExternalDeps(SourceSetType.TEST)));
    testDeps.addAll(targets(target.getTargetDeps(SourceSetType.TEST)));

    List<String> testAptDeps = new ArrayList<>();
    testAptDeps.addAll(external(target.getExternalAptDeps(SourceSetType.TEST)));
    testAptDeps.addAll(targets(target.getTargetAptDeps(SourceSetType.TEST)));

    Set<String> providedDeps = new LinkedHashSet<>();
    providedDeps.addAll(external(target.getExternalProvidedDeps(SourceSetType.TEST)));
    providedDeps.addAll(targets(target.getTargetProvidedDeps(SourceSetType.TEST)));
    // providedDeps.add(D8Util.RT_STUB_JAR_RULE); We add it through macro.

    List<String> preFinalJvmArgs = target.getTestOptions().getJvmArgs();
    List<String> finalJvmArgs;

    // If vm_args are what we have defaults via macros, don't render them in BUILD files.
    if (preFinalJvmArgs.size() == 6
        && preFinalJvmArgs.contains("-Djava.awt.headless=true")
        && preFinalJvmArgs.contains("-Dfile.encoding=UTF-8")
        && preFinalJvmArgs.contains("-Duser.country=US")
        && preFinalJvmArgs.contains("-Duser.language=en")
        && preFinalJvmArgs.contains("-Duser.variant")
        && preFinalJvmArgs.contains("-ea")) {
      finalJvmArgs = ImmutableList.of();
    } else {
      finalJvmArgs = preFinalJvmArgs;
    }

    AndroidTestRule androidTest =
        new AndroidTestRule()
            .srcs(target.getTest().getSources())
            .exts(target.getTestRuleType().getProperties())
            .apPlugins(getApPlugins(target.getTestApPlugins()))
            .aptDeps(testAptDeps)
            .providedDeps(providedDeps)
            .resources(target.getTest().getJavaResources())
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .exportedDeps(aidlRuleNames)
            .excludes(appClass != null ? ImmutableSet.of(appClass) : ImmutableSet.of())
            .options(target.getTest().getCustomOptions())
            .jvmArgs(finalJvmArgs)
            .env(target.getTestOptions().getEnv())
            .robolectricManifest(manifestRule)
            .runtimeDependency(RobolectricManager.ROBOLECTRIC_CACHE_TARGET);

    if (target.getTestRuleType().equals(RuleType.KOTLIN_ROBOLECTRIC_TEST)) {
      androidTest.language("kotlin");
    }

    return androidTest
        .ruleType(target.getTestRuleType().getBuckName())
        .defaultVisibility()
        .deps(testDeps)
        .name(test(target))
        .labels(ANDROID_TEST_LABELS)
        .extraBuckOpts(target.getExtraOpts(RuleType.ROBOLECTRIC_TEST));
  }
}
