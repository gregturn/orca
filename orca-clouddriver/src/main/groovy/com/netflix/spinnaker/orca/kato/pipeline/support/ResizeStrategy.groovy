/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.kato.pipeline.support

import com.netflix.spinnaker.orca.clouddriver.pipeline.support.Location
import com.netflix.spinnaker.orca.pipeline.model.Stage
import groovy.transform.Canonical

interface ResizeStrategy {
  static enum ResizeAction {
    scale_exact, scale_up, scale_down, scale_to_cluster
  }

  static class OptionalConfiguration {
    ResizeAction action
    Integer scalePct
    Integer scaleNum
    String resizeType

    //Temporary shim to support old style configuration where scale_exact was not an action
    //TODO(cfieber) - remove this after we've rolled this in and don't need to roll back
    ResizeAction getActualAction() {
      if (resizeType == 'exact') {
        return ResizeAction.scale_exact
      }
      // resize Orchestration doesn't provide action currently
      if (action == null) {
        return ResizeAction.scale_exact
      }
      return action
    }
  }

  @Canonical
  static class Capacity {
    Integer max
    Integer desired
    Integer min
  }

  boolean handles(ResizeAction resizeAction)
  Capacity capacityForOperation(Stage stage, String account, String serverGroupName, String cloudProvider, Location location, OptionalConfiguration resizeConfig)
}
