package org.obolibrary.robot;

import java.util.List;

/**
 * Entry point for bundles of ROBOT plugins.
 *
 * @author <a href="mailto:dgouttegattat@incenp.org">Damien Goutte-Gattat</a>
 */
public interface ICommandProvider {

  /**
   * Get all commands provided by this bundle.
   *
   * @return the list of commands to add to ROBOT's CLI
   */
  List<Command> getCommands();
}
