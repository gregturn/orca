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

import com.netflix.frigga.Names
import com.netflix.spinnaker.orca.clouddriver.pipeline.support.Location
import com.netflix.spinnaker.orca.clouddriver.pipeline.support.TargetServerGroup
import com.netflix.spinnaker.orca.kato.pipeline.support.ResizeStrategy.Capacity
import com.netflix.spinnaker.orca.kato.pipeline.support.ResizeStrategy.OptionalConfiguration
import com.netflix.spinnaker.orca.kato.pipeline.support.ResizeStrategy.ResizeAction
import com.netflix.spinnaker.orca.clouddriver.utils.OortHelper
import com.netflix.spinnaker.orca.pipeline.model.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ScaleToClusterResizeStrategy implements ResizeStrategy{

  @Autowired
  OortHelper oortHelper

  @Override
  boolean handles(ResizeAction resizeAction) {
    return resizeAction == ResizeAction.scale_to_cluster
  }

  @Override
  Capacity capacityForOperation(Stage stage, String account, String serverGroupName, String cloudProvider, Location location, OptionalConfiguration resizeConfig) {
    def names = Names.parseName(serverGroupName)
    def appName = names.app
    def clusterName = names.cluster

    def cluster = oortHelper.getCluster(appName, account, clusterName, cloudProvider)
    List<TargetServerGroup> targetServerGroups = cluster
      .orElse([serverGroups: []])
      .serverGroups.collect { new TargetServerGroup(serverGroup: it) }
      .findAll { it.getLocation() == location }

    if (!targetServerGroups) {
      throw new IllegalStateException("no server groups found for cluster $cloudProvider/$account/$clusterName in $location")
    }
    Capacity capacity = targetServerGroups.inject(new Capacity(0, 0, 0)) { Capacity capacity, TargetServerGroup tsg ->
      capacity.min = Math.max(capacity.min, tsg.capacity?.min ?: 0)
      capacity.max = Math.max(capacity.max, tsg.capacity?.max ?: 0)
      capacity.desired = Math.max(capacity.desired, tsg.capacity?.desired ?: 0)
      return capacity
    }

    int increment = 0
    if (resizeConfig.scalePct) {
      double factor = Math.abs(resizeConfig.scalePct) / 100.0d
      increment = (int) Math.ceil(capacity.desired * factor)
    } else if (resizeConfig.scaleNum) {
      increment = Math.abs(resizeConfig.scaleNum)
    }
    //ensure our bounds are legitimate:
    capacity.max = Math.max(capacity.max, capacity.min)
    capacity.min = Math.min(capacity.max, capacity.min)
    capacity.desired = Math.max(capacity.min, capacity.desired)
    capacity.desired = Math.min(capacity.desired + increment, capacity.max)

    return capacity
  }
}
