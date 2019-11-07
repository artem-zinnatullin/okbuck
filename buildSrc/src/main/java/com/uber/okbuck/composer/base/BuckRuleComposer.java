package com.uber.okbuck.composer.base;

import com.google.common.base.Splitter;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public abstract class BuckRuleComposer {

  public static boolean isExplicitSrcsTarget(Target target) {
    if (target.getName().startsWith("bin_") || target.getName().startsWith("src_")) {
      return true;
    }

    for (String explicitSrcsPackage: Splitter.on(';').split(System.getenv("OKBUCK_EXPLICIT_SRCS_TARGETS"))) {
      if (target.getPath().equals(explicitSrcsPackage)) {
        return true;
      }
    }

    return false;
  }

  public static boolean shouldGenerateSrcs(Target target) {
    return isExplicitSrcsTarget(target);
  }

  public BuckRuleComposer() {}

  public static Set<String> external(Set<ExternalDependency> deps) {
    return deps.stream().map(BuckRuleComposer::external).collect(Collectors.toSet());
  }

  public static String external(ExternalDependency dep) {
    return String.format("//%s:%s", dep.getTargetPath(), dep.getTargetName());
  }

  public static Set<String> externalApt(Set<ExternalDependency> deps) {
    return external(deps);
  }

  public static Set<String> targets(Set<Target> deps) {
    return deps.stream().map(BuckRuleComposer::targets).collect(Collectors.toSet());
  }

  private static String targets(Target dep) {
    if (isExplicitSrcsTarget(dep)) {
      return String.format("//%s:src_%s", dep.getPath(), dep.getName());
    } else {
      return String.format("//%s:lib", dep.getPath());
    }
  }

  public static Set<String> targetsApt(Set<Target> deps) {
    return deps.stream()
        .filter(target -> target.getClass().equals(JvmTarget.class))
        .map(BuckRuleComposer::targets)
        .collect(Collectors.toSet());
  }

  public static String binTargets(Target dep) {
    return String.format("//%s:bin_%s", dep.getPath(), dep.getName());
  }

  @Nullable
  public static String fileRule(@Nullable String fileString) {
    if (fileString == null) {
      return null;
    }

    Path filePath = Paths.get(fileString);
    Path parentFilePath = filePath.getParent();

    if (parentFilePath == null) {
      return String.format("//:%s", filePath);
    } else {
      return String.format("//%s:%s", parentFilePath, parentFilePath.relativize(filePath));
    }
  }
}
