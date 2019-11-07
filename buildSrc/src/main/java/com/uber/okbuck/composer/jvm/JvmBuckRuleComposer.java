package com.uber.okbuck.composer.jvm;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.annotation.JvmPlugin;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import java.util.Set;
import java.util.stream.Collectors;

public class JvmBuckRuleComposer extends BuckRuleComposer {

  public static String src(JvmTarget target) {
    if (isExplicitSrcsTarget(target)) {
      return "src_" + target.getName();
    } else {
      return "lib";
    }
  }

  public static String bin(JvmTarget target) {
    if (isExplicitSrcsTarget(target)) {
      return "bin_" + target.getName();
    } else {
      return "bin";
    }
  }

  public static String test(JvmTarget target) {
    if (isExplicitSrcsTarget(target)) {
      return "test_" + target.getName();
    } else {
      return "test";
    }
  }

  public static String integrationTest(JvmTarget target) {
    if (isExplicitSrcsTarget(target)) {
      return "integration_test_" + target.getName();
    } else {
      return "integration_test";
    }
  }

  /**
   * Returns the ap's plugin rules path
   *
   * @param aps Annotation Processor plugin's UID
   * @return Set of java annotation processor plugin's rule paths.
   */
  public static Set<String> getApPlugins(Set<JvmPlugin> aps) {
    return aps.stream().map(JvmBuckRuleComposer::getApPluginRulePath).collect(Collectors.toSet());
  }

  /**
   * Returns the java annotation processor plugin's rule name using the pluginUID
   *
   * @param plugin plugin used to get the rule name
   * @return Plugin rule name.
   */
  protected static String getApPluginRuleName(JvmPlugin plugin) {
    return String.format("processor-%s", plugin.pluginUID());
  }

  /**
   * Returns the java annotation processor plugin's rule path using the pluginUID
   *
   * @param plugin plugin used to get the rule path
   * @return Plugin rule path.
   */
  private static String getApPluginRulePath(JvmPlugin plugin) {
    if (plugin.pluginDependency().isPresent()) {
      return String.format(
          "//%s:%s", plugin.pluginDependency().get().getTargetPath(), getApPluginRuleName(plugin));
    } else {
      return String.format(
          "//" + OkBuckGradlePlugin.WORKSPACE_PATH + "/processor:%s", getApPluginRuleName(plugin));
    }
  }
}
